(ns re-thread.client
  (:require
   [re-thread.codec :refer [encode-data decode-data]]
   [reagent.core :as r]
   [reagent.ratom :as ra]))

;; State

;; Holds the last value of any subs currently used by any component
(defonce sub-cache
  (r/atom {}))

;; Worker, will be empty until init
(defonce worker
  (atom nil))

(defn worker->
  "Handle Messages from the worker"
  [msg]
  (let [[action sub-id v :as event-vec] (decode-data (.-data msg))]
    (case action
      ;; Update data for a sub
      :update (swap! sub-cache assoc sub-id v)
      ;; Remove subs from the cache when not needed
      :remove (swap! sub-cache dissoc sub-id)

      (.warn js/console (str "unhandled message: " event-vec)))))

(defn new-worker
  "Instantiate a new worker with a listener"
  [js-path]
  (let [w (js/Worker. js-path)]
    (doto w
      (.addEventListener "message" worker->))))

(defn ->worker
  "Submit an event to be sent to the worker."
  [event]
  (.postMessage @worker (encode-data event)))

;; API

(defn init
  "Initialize the worker with the given path.
   Call this in your core namespace once!"
  [js-path & [cb-fn]]
  (reset! worker (new-worker js-path))
  (when cb-fn
    (cb-fn))
  true)

(defn dispatch
  "Re-frame dispatch on worker."
  [event-v]
  (->worker [:dispatch event-v]))

(defn subscribe
  "Like re-frame.core/subscribe, returns a reaction that will recieve
   the computed value of the sub, with updates. Unlike re-frame, a second
   arg can be provided with a default value to prevent nils/thrashing"
  [[sub-id] & [?default]]
  (let [uid (random-uuid)] ;; Unique id for this subscription

    ;; Request the sub
    (->worker [:subscribe [uid sub-id]])

    ;; Return the reaction
    (ra/make-reaction
     (fn []
       (get @sub-cache sub-id ?default))

     :on-dispose
     (fn [_]
       ;; We let the worker know we're unsubscribing, it will handle
       ;; clearing the cache if needed.
       (->worker [:unsubscribe [uid sub-id]])))))
