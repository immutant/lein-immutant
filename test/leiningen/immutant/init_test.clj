(ns leiningen.immutant.init-test
  (:use midje.sweet)
  (:require [leiningen.immutant.test-helper :as h]
            [clojure.java.io                :as io]))

(h/setup-test-env)

(h/for-all-generations
  (println "\n==> Testing with lein generation" h/*generation*)

  (let [project-name (str "testproj-" h/*generation*)]
    
    (fact (str "new should work for lein " h/*generation*)
      (h/with-tmp-dir
        (let [project-dir (io/file h/*tmp-dir* project-name)]
          (h/run-lein h/*generation* "immutant" "new" project-name
                      :dir h/*tmp-dir*
                      :env h/base-lein-env) => 0
          (.exists (io/file project-dir)) => true
          (.exists (io/file project-dir "immutant.clj")) => true)))

    (if (= 2 h/*generation*)
      (fact "'new immutant' should work with lein 2"
      (h/with-tmp-dir
        (let [project-name (str project-name "-new")
              project-dir (io/file h/*tmp-dir* project-name)]
          (h/run-lein h/*generation* "new" "immutant" project-name
                      :dir h/*tmp-dir*
                      :env h/base-lein-env) => 0
          (.exists (io/file project-dir)) => true
          (.exists (io/file project-dir "immutant.clj")) => true))))
    
    (fact (str "init should work for lein " h/*generation*)
      (h/with-tmp-dir
        (let [project-dir (io/file h/*tmp-dir* project-name)]
          (h/run-lein h/*generation* "new" project-name
                      :dir h/*tmp-dir*
                      :env h/base-lein-env)
          (h/run-lein h/*generation* "immutant" "init"
                      :dir project-dir
                      :env h/base-lein-env) => 0
          (.exists (io/file project-dir "immutant.clj")) => true)))))

