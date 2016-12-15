(ns todomvc-worker.core
  (:require-macros [secretary.core :refer [defroute]])
  (:require [re-thread.client :refer [init dispatch]]
            [goog.events :as events]
            [reagent.core :as reagent]
            [secretary.core :as secretary]
            [devtools.core :as devtools]
            [todomvc-worker.views])
  (:import [goog History]
           [goog.history EventType]))


;; -- Debugging aids ----------------------------------------------------------
(devtools/install!)       ;; we love https://github.com/binaryage/cljs-devtools
(enable-console-print!)   ;; so println writes to console.log

;; -- Worker Bootstrap --------------------------------------------------------
(defonce worker
  (delay (init "/js/bootstrap_worker.js")))

;; -- Routes and History ------------------------------------------------------

(defroute "/" [] (dispatch [:set-showing :all]))
(defroute "/:filter" [filter] (dispatch [:set-showing (keyword filter)]))

(def history
  (doto (History.)
    (events/listen EventType.NAVIGATE
                   (fn [event] (secretary/dispatch! (.-token event))))
    (.setEnabled true)))


;; -- Entry Point -------------------------------------------------------------

(defn ^:export main
  []
  @worker
  (reagent/render [todomvc-worker.views/todo-app]
                  (.getElementById js/document "app")))
