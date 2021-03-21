(ns co.poyo.file-map-test
  (:require
   [clojure.test :as t]
   [co.poyo.watch-and-run :as wr]))

(t/deftest test-util-fns
  (t/is (= (map #(.getPath %)
                (wr/read-files-from-src-path "test/co/poyo/test_src"))
           '("test/co/poyo/test_src/a.clj"
             "test/co/poyo/test_src/b.clj"
             "test/co/poyo/test_src/c1.clj"
             "test/co/poyo/test_src/c2.clj"
             "test/co/poyo/test_src/d.clj")))
  ; note : coerce dep graph into map for easier testing
  (t/is (= (into {} (wr/get-dep-graph ["test/co/poyo/test_src"]))
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
                         'co.poyo.test-src.d #{'co.poyo.test-src.c1}}}))

  (t/is (=

         '(co.poyo.test-src.a
           co.poyo.test-src.c1
           co.poyo.test-src.d
           co.poyo.test-src.b
           co.poyo.test-src.c2)

         (wr/all-local-deps-deep ["test/co/poyo/test_src"] 'co.poyo.test-src.a))))
