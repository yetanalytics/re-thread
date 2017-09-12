(ns re-thread.codec
  (:require [cognitect.transit :as t]))

(def ^:dynamic writer (t/writer :json))
(def ^:dynamic reader (t/reader :json))

(defn encode-data [data]
  (t/write writer data))

(defn decode-data [s]
  (t/read reader s))
