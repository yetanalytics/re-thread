(ns re-thread.auto-worker
  "Automatically installs the listener for the worker. Require this NS for
   minimally invasive use in an existing app."
  (:require [re-thread.worker :refer [listen!]]))

(defonce listener (listen!))
