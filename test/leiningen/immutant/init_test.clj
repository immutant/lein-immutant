(ns leiningen.immutant.init-test
  (:use midje.sweet
        leiningen.immutant.test-helper)
  (:require [clojure.java.io :as io]))

(setup-test-env)

(println "\n==> Testing new/init")

(let [project-name (str "testproj")]
  
  (fact (str "new should work")
    (with-tmp-dir
      (let [project-dir (io/file *tmp-dir* project-name)]
        (run-lein "immutant" "new" project-name
                  :dir *tmp-dir*
                  :env base-lein-env) => 0
                  (.exists (io/file project-dir)) => true
                  (.exists (io/file project-dir "src/immutant/init.clj")) => true
                  (.contains (slurp (io/file project-dir "src/immutant/init.clj"))
                             (str ":use " project-name ".core")) => true)))

  (if (= 2)
    (fact "'new immutant' should work with lein 2"
      (with-tmp-dir
        (let [project-name (str project-name "new")
              project-dir (io/file *tmp-dir* project-name)]
          (run-lein "new" "immutant" project-name
                    :dir *tmp-dir*
                    :env base-lein-env) => 0
                    (.exists (io/file project-dir)) => true
                    (.exists (io/file project-dir "src/immutant/init.clj")) => true
                    (.contains (slurp (io/file project-dir "src/immutant/init.clj"))
                               (str ":use " project-name ".core")) => true
                               (.contains (slurp (io/file project-dir (str "src/" project-name "/core.clj")))
                                          (str "(ns " project-name ".core")) => true
                                          (let [test-file (slurp (io/file project-dir (str "test/" project-name "/core_test.clj")))]
                                            (.contains test-file (str "(ns " project-name ".core-test")) => true
                                            (.contains test-file (str project-name ".core)")) => true)))))
  
  (fact (str "init should work")
    (with-tmp-dir
      (let [project-dir (io/file *tmp-dir* project-name)]
        (run-lein "new" project-name
                  :dir *tmp-dir*
                  :env base-lein-env)
        (run-lein "immutant" "init"
                  :dir project-dir
                  :env base-lein-env) => 0
                  (.exists (io/file project-dir "src/immutant/init.clj")) => true
                  (.contains (slurp (io/file project-dir "src/immutant/init.clj"))
                             (str ":use " project-name ".core")) => true))))

