(ns re-thread.worker
  (:require
   [re-thread.codec :refer [encode-data decode-data]]
   [re-frame.core :as re-frame]
   [reagent.core :as r]))

(defn ->client
  "Send an event to the client."
  [event]
  (.postMessage js/self (encode-data event)))

;; Holds reagent tracks of re-frame subs
(defonce tracks
  (atom {}))

(defn- register-track
  [?t uid qv]
  (if ?t
    (update ?t :client-subs conj uid)
    (let [new-sub (re-frame/subscribe qv)]
      {:tracker (r/track!
                 (fn []
                   (->client [:update qv @new-sub]))
                 [])
       :client-subs #{uid}})))

(defn add-sub-track!
  "Given a tuple of unique ID and re-frame subscription query vector,
   Make a new track or add this id to the set of client subs."
  [[unique-id query-v]]
  (swap! tracks update query-v
         register-track
         unique-id
         query-v))

(defn- unregister-track
  [tmap uid qv]
  (if-let [t (get tmap qv)]
    (let [{:keys [tracker
                  client-subs]} t
          client-subs-after (disj client-subs uid)]
      (if (< 0 (count client-subs-after))
        (assoc tmap qv {:tracker tracker
                        :client-subs
                        client-subs-after})
        (do
          ;; stop sending updates
          (r/dispose! tracker)
          ;; remove from client cache
          (->client [:remove qv])
          (dissoc tmap qv))))
    tmap))

(defn dispose-sub-track!
  "Given a tuple of unique ID and re-frame subscription query vector,
  remove the uid, and remove the tracker if no other uids are left."
  [[unique-id query-v]]
  (swap! tracks
         unregister-track
         unique-id
         query-v))

(defn client->
  "Decode and dispatch client events."
  [msg]
  (let [[kind data] (decode-data (.-data msg))]
    (case kind
      :dispatch (re-frame/dispatch data)
      :subscribe (add-sub-track! data)
      :unsubscribe (dispose-sub-track! data))))

;; API

(defn listen!
  "Attaches a listener to get events from the client, then lets the client know
  it's ready."
  []
  (.addEventListener js/self "message" client-> false)
  true)
