(ns leiningen.immutant.env-test
  (:use midje.sweet)
  (:require [leiningen.immutant.test-helper :as h]
            [clojure.java.io                :as io]))

(h/setup-test-env)

(defn run-lein-env [gen env & args]
  (apply h/run-lein gen "immutant" "env"
         (conj (vec args)
               :dir "/tmp"
               :env env
               :return-result? true)))

(h/for-all-generations
  (println "\n==> Testing with lein generation" h/*generation*)

  (facts "with IMMUTANT_HOME set"
    (let [alt-immutant-home (io/file (io/resource "alt-immutant-install"))
          env (assoc h/base-lein-env
                "IMMUTANT_HOME" (.getAbsolutePath alt-immutant-home))]
      
      (fact (str "env should work for lein " h/*generation*)
        (let [result (run-lein-env h/*generation* env)]
          (:exit result) => 0
          (:out result)  => (re-pattern
                             (str "immutant-home: "
                                  (.getAbsolutePath alt-immutant-home)
                                  " \\(from: \\$IMMUTANT_HOME\\)"))
          (:out result)  => (re-pattern
                             (str "jboss-home: "
                                  (.getAbsolutePath alt-immutant-home)
                                  "/jboss \\n"))))
      
      (fact (str "env with an arg should work for lein " h/*generation*)
        (let [result (run-lein-env h/*generation* env "immutant-home")]
          (:exit result) => 0
          (:out result)  => (.getAbsolutePath alt-immutant-home))
        (let [result (run-lein-env h/*generation* env "jboss-home")]
          (:exit result) => 0
          (:out result)  => (str (.getAbsolutePath alt-immutant-home) "/jboss")))
      
      (fact (str "env with an invalid arg should work for lein " h/*generation*)
        (let [result (run-lein-env h/*generation* env "ham")]
          (:exit result) => 0
          (:out result)  => ""
          (:err result)  => "ham is an unknown env key. Valid keys are: (immutant-home jboss-home)\n")))))
