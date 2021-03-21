(ns co.poyo.watch-and-run
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.tools.namespace.file :as ns-file]
            [clojure.tools.namespace.find :as ns-find]
            [clojure.tools.namespace.track :as ns-track]
            [clojure.tools.namespace.reload :as ns-reload]
            [hawk.core :as hawk]
            [mount.core :as mount]
            [co.poyo.watch-and-run.ns-tracker :refer [tracker]]
            [taoensso.timbre :as timbre]))

(defn read-files-from-src-path [src-path]
  (ns-find/find-clojure-sources-in-dir
   (io/file src-path)))

(defn get-dep-graph [src-paths]
  (let [src-files (map read-files-from-src-path src-paths)
        src-files-unique (apply set/union src-files)
        tracker (ns-file/add-files {} src-files-unique)
        dep-graph (tracker ::ns-track/deps)]
    dep-graph))

(defn all-local-deps-deep
  "Creates a sequence of local-only deps for a given namespace.
   - The list starts with the given namespace.
   - The list is unique
   - The list is in 'dependent' order"
  [tracked-src-paths ns-sym]
  (let [all-dependencies (:dependencies (get-dep-graph tracked-src-paths))
        ns-names (set (ns-find/find-namespaces (map io/file tracked-src-paths)))
        seen (volatile! #{})
        part-of-project? (partial contains? ns-names)
        unseen? #(not (contains? @seen %))
        branch (fn [ns-sym]
                 (vswap! seen conj ns-sym))
        children (fn [ns-sym]
                   (->> ns-sym
                        (get all-dependencies)
                        (filter unseen?)
                        (filter part-of-project?)))]
    (tree-seq branch children ns-sym)))

(defn watch-handler [watched]
  (let [changed (set (tracker))]
    (doseq [{:keys [post-reload-fn ns-sym]} @watched
            :let [deps (all-local-deps-deep ns-sym)
                  intersection (set/intersection (set deps) changed)]
            :when (not-empty intersection)]
      (doseq [reload-ns-sym (reverse deps)
              :when (contains? changed reload-ns-sym)]
        (timbre/info "Reloading:" reload-ns-sym)
        (ns-reload/remove-lib ns-sym)
        (require reload-ns-sym :reload))
      (when post-reload-fn
        (post-reload-fn)))))

(mount/defstate watch-and-run
  :start
  (let [watched (atom #{})
        watcher (hawk/watch!
                 [{:paths (:src-paths (mount/args))
                   :handler (fn [ctx ev]
                              (watch-handler watched))}])]
    {:watcher watcher :watched watched})
  :stop
  (hawk/stop! (:watcher watch-and-run)))

(defn add-jobs [jobs]
  (if-let [watched (:watched watch-and-run)]
    (do (swap! watched into jobs)
        (doseq [{:keys [build-fn]} jobs]
          (build-fn))
        jobs)
    (timbre/warn "Watcher not running.")))

(defn remove-jobs [jobs]
  (if-let [watched (:watched watch-and-run)]
    (doseq [job jobs]
      (swap! watched disj job))
    (timbre/warn "Watcher not running.")))

(comment

  (mount/start))
