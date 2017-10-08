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

(defmulti worker->* (fn [[action & _]]
                      action))

(defmethod worker->* :update
  [[action query-v v :as event-vec]]
  (swap! sub-cache assoc query-v v))

(defmethod worker->* :remove
  [[action query-v :as event-vec]]
  (swap! sub-cache dissoc query-v))

(defmethod worker->* :default
  [event-vec]
  (.warn js/console (str "unhandled message: " event-vec)))

(defn worker->
  "Handle Messages from the worker"
  [msg]
  (worker->* (decode-data (.-data msg))))

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
  [query-v & [?default]]
  (let [uid (random-uuid)] ;; Unique id for this subscription

    ;; Request the sub
    (->worker [:subscribe [uid query-v]])

    ;; Return the reaction
    (ra/make-reaction
     (fn []
       (get @sub-cache query-v ?default))

     :on-dispose
     (fn [_]
       ;; We let the worker know we're unsubscribing, it will handle
       ;; clearing the cache if needed.
       (->worker [:unsubscribe [uid query-v]])))))
