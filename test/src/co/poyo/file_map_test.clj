(ns co.poyo.file-map-test
  (:require
   [clojure.test :as t]
   [co.poyo.watch-and-run :as wr]))

(t/deftest test-util-fns
  (t/is
   (=
    ;; actual
    (map #(.getPath %)
         (wr/read-files-from-src-path "test/src/co/poyo/test_src"))

    ;; expected
    '("test/src/co/poyo/test_src/a.clj"
      "test/src/co/poyo/test_src/b.clj"
      "test/src/co/poyo/test_src/c1.clj"
      "test/src/co/poyo/test_src/c2.clj"
      "test/src/co/poyo/test_src/d.clj")))

  (t/is
   (=
    ;; actual
    ;; NOTE : coerce dep graph into map for easier testing
    (into {} (wr/get-dep-graph ["test/src/co/poyo/test_src"]))
    ;; expected
    {:dependencies {'co.poyo.test-src.a #{'clojure.string
                                          'co.poyo.test-src.b
                                          'co.poyo.test-src.c1}
                    'co.poyo.test-src.b #{'co.poyo.test-src.c1
                                          'co.poyo.test-src.c2}
                    'co.poyo.test-src.c1 #{'co.poyo.test-src.d}}
     :dependents {'clojure.string #{'co.poyo.test-src.a}
                  'co.poyo.test-src.b #{'co.poyo.test-src.a}
                  'co.poyo.test-src.c1 #{'co.poyo.test-src.b
                                         'co.poyo.test-src.a}
                  'co.poyo.test-src.c2 #{'co.poyo.test-src.b}
                  'co.poyo.test-src.d #{'co.poyo.test-src.c1}}}

    ))

  (t/is
   (=
    ;; actual
    (wr/all-local-deps-deep ["test/src/co/poyo/test_src"] 'co.poyo.test-src.a)
    ;; expected
    '(co.poyo.test-src.a
      co.poyo.test-src.c1
      co.poyo.test-src.d
      co.poyo.test-src.b
      co.poyo.test-src.c2)
    )))

