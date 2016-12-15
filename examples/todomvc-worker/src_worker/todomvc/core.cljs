(ns todomvc.core
  (:require-macros [secretary.core :refer [defroute]])
  (:require [goog.events :as events]
            [reagent.core :as reagent]
            [re-frame.core :refer [dispatch dispatch-sync]]
            [secretary.core :as secretary]
            [todomvc.events]
            [todomvc.subs]
            [devtools.core :as devtools]
            [re-thread.worker :refer [listen!]])
  (:import [goog History]
           [goog.history EventType]))

(enable-console-print!)

;; Initialize and run the listener
(defonce init
  (delay
   (print "initializing Worker...")
   (dispatch-sync [:initialise-db])
   (listen!)))

@init

(defn ^:export main
  []
  (print "fig reload"))
