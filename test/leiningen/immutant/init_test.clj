(ns leiningen.immutant.init-test
  (:use midje.sweet
        leiningen.immutant.test-helper)
  (:require [clojure.java.io :as io]))

(setup-test-env)

(for-all-generations
  (println "\n==> Testing with lein generation" *generation*)

  (let [project-name (str "testproj-" *generation*)]
    
    (fact (str "new should work for lein " *generation*)
      (with-tmp-dir
        (let [project-dir (io/file *tmp-dir* project-name)]
          (run-lein *generation* "immutant" "new" project-name
                      :dir *tmp-dir*
                      :env base-lein-env) => 0
          (.exists (io/file project-dir)) => true
          (.exists (io/file project-dir "src/immutant/init.clj")) => true
          (.contains (slurp (io/file project-dir "src/immutant/init.clj"))
                     (str ":use " project-name ".core")) => true)))

    (if (= 2 *generation*)
      (fact "'new immutant' should work with lein 2"
      (with-tmp-dir
        (let [project-name (str project-name "-new")
              project-dir (io/file *tmp-dir* project-name)]
          (run-lein *generation* "new" "immutant" project-name
                      :dir *tmp-dir*
                      :env base-lein-env) => 0
          (.exists (io/file project-dir)) => true
          (.exists (io/file project-dir "src/immutant/init.clj")) => true))))
    
    (fact (str "init should work for lein " *generation*)
      (with-tmp-dir
        (let [project-dir (io/file *tmp-dir* project-name)]
          (run-lein *generation* "new" project-name
                      :dir *tmp-dir*
                      :env base-lein-env)
          (run-lein *generation* "immutant" "init"
                      :dir project-dir
                      :env base-lein-env) => 0
          (.exists (io/file project-dir "src/immutant/init.clj")) => true
          (.contains (slurp (io/file project-dir "src/immutant/init.clj"))
                     (str ":use " project-name ".core")) => true)))))

