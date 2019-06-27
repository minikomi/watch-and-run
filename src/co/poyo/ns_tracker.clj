(ns  co.poyo.ns-tracker
  (:require [mount.core :as mount]
            [ns-tracker.core :as ns-tracker]))

(mount/defstate tracker
  :start
  (ns-tracker/ns-tracker ["src"]))
