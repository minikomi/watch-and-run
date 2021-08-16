(ns co.poyo.watch-and-run.file-map
  (:require [me.raynes.fs :as fs]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [taoensso.timbre :as timbre]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]))

(defn spit-txt [base-path {:keys [template path data]}]
  (let [out-file
        (apply fs/file
               base-path
               path)]
    (timbre/infof "BUILD [%s]->[%s/%s]"
                  template
                  base-path
                  (str/join "/" path))
    (io/make-parents out-file)
    (spit
     out-file
     ((resolve template) data))))

(defn sym->ns-sym
  "extracts the namespace part from a symbol
   of type namespace/symbol"
  [sym]
  (symbol (namespace sym)))


(defn load-edn
  "Load edn from an io/reader source (filename or io/resource)."
  [source]
  (try
    (with-open [r (io/reader source)]
      (edn/read (java.io.PushbackReader. r)))
    (catch java.io.IOException e
      (timbre/errorf "Couldn't open '%s': %s" source (.getMessage e)))
    (catch RuntimeException e
      (timbre/errorf "Error parsing edn file '%s': %s" source (.getMessage e)))))

(defn -update-acc [base-path acc f]
  (let [ns-sym (sym->ns-sym f)]
    (require ns-sym)
    (if (resolve f)
      (vswap! acc conj
              {:base-path base-path
               :path (:path node)
               :template (:template node)
               :data (:data node)
               :ns-sym ns-sym
               :build-fn (if (:build-fn node)
                           (fn [] ((:build-fn node)
                                   base-path
                                   node))
                           (fn [] (spit-txt base-path node)))})
      (timbre/warnf "Could not find sym/ns [%s]" (:node template)))))

(defn parse-file-map
  ([file-map-edn]
   (parse-file-map file-map-edn {:base-path ""}))
  ([file-map-edn {:keys [base-path]}]
   (try
     (let [acc (volatile! #{})]
       (doall
        (tree-seq
         (fn file-map->jobs-br? [node] ;; branch
           (if-let [f (or (:build-fn node) (:template node))]
             (do (-update-acc base-path acc f)
                 false) ; end tree walk here
             (map? node)))
         (fn file-map->jobs-children [node] ;; children
           (for [[p n] node
                 :when (and (map? n) (not (:template p)))]
             (assoc n :path ((fnil conj []) (:path node) p))))
         file-map-edn))
       @acc)
     (catch Exception e
       (timbre/errorf "Error loading file map %s \n%s"
                      (.getMessage e)
                      (with-out-str
                        (pprint file-map-edn)))))))

(defn load-file-map
  ([file-map-source]
   (load-file-map file-map-source {:base-path "target"}))
  ([file-map-source opts]
   (parse-file-map (load-edn file-map-source) opts)))

(defn run-all-jobs [jobs]
  (doall
   (pmap (fn [{:keys [build-fn]}]
           (build-fn))
         jobs)))
