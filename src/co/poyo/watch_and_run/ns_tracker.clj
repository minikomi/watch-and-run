(ns co.poyo.watch-and-run.ns-tracker
  (:require [mount.core :as mount]
            [ns-tracker.core :as ns-tracker]))

(mount/defstate tracker
  :start
  (ns-tracker/ns-tracker ["src"]))
