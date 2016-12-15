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

(defn add-sub-track!
  "Given a tuple of unique ID and re-frame subscription ID,
   Make a new track or add this id to the set of client subs."
  [[unique-id sub-id]]
  (swap! tracks update sub-id
         (fn [?t uid sid]
           (if ?t
             (update ?t :client-subs conj uid)
             (let [new-sub (re-frame/subscribe [sid])]
               {:tracker (r/track!
                          (fn []
                            (->client [:update sid @new-sub]))
                          [])
                :client-subs #{uid}})))
         unique-id
         sub-id))

(defn dispose-sub-track!
  "Given a tuple of unique ID and re-frame subscription ID,
  remove the unique ID from the "
  [[unique-id sub-id]]
  (swap! tracks
         (fn [tmap uid sid]
           (if-let [t (get tmap sid)]
             (let [{:keys [tracker
                           client-subs]} t
                   client-subs-after (disj client-subs uid)]
               (if (< 0 (count client-subs-after))
                 (assoc tmap sid {:tracker tracker
                                  :client-subs
                                  client-subs-after})
                 (do
                   ;; stop sending updates
                   (r/dispose! tracker)
                   ;; remove from client cache
                   (->client [:remove sid])
                   (dissoc tmap sid))))
             tmap))
         unique-id
         sub-id))

(defn client->
  "Decode and dispatch client events"
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
