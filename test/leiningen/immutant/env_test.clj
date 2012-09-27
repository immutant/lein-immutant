(ns leiningen.immutant.env-test
  (:use midje.sweet
        leiningen.immutant.test-helper)
  (:require [clojure.java.io :as io]))

(setup-test-env)

(defn run-lein-env [gen env & args]
  (apply run-lein gen "immutant" "env"
         (conj (vec args)
               :dir "/tmp"
               :env env
               :return-result? true)))

(for-all-generations
  (println "\n==> Testing with lein generation" *generation*)

  (facts "with IMMUTANT_HOME set"
    (let [immutant-home (io/file "immutant-home")
          env (assoc base-lein-env
                "IMMUTANT_HOME" (.getAbsolutePath immutant-home))]
      
      (fact (str "env should work for lein " *generation*)
        (let [result (run-lein-env *generation* env)]
          (:exit result) => 0
          (:out result)  => (re-pattern
                             (str "immutant-home: "
                                  (.getAbsolutePath immutant-home)
                                  " \\(from: \\$IMMUTANT_HOME\\)"))
          (:out result)  => (re-pattern
                             (str "jboss-home: "
                                  (.getAbsolutePath immutant-home)
                                  "/jboss \\n"))))
      
      (fact (str "env with an arg should work for lein " *generation*)
        (let [result (run-lein-env *generation* env "immutant-home")]
          (:exit result) => 0
          (:out result)  => (.getAbsolutePath immutant-home))
        (let [result (run-lein-env *generation* env "jboss-home")]
          (:exit result) => 0
          (:out result)  => (str (.getAbsolutePath immutant-home) "/jboss")))
      
      (fact (str "env with an invalid arg should work for lein " *generation*)
        (let [result (run-lein-env *generation* env "ham")]
          (:exit result) => 0
          (:out result)  => ""
          (:err result)  => "ham is an unknown env key. Valid keys are: (immutant-home jboss-home)\n"))))

  (facts "with IMMUTANT_HOME not set"
    (let [immutant-home (io/file (io/resource "lein-home/immutant/current"))]
      (fact (str "env should work for lein " *generation*)
        (let [result (run-lein-env *generation* base-lein-env)]
          (:exit result) => 0
          (:out result)  => (re-pattern
                             (str "immutant-home: "
                                  (.getAbsolutePath immutant-home)
                                  " \\n"))
          (:out result)  => (re-pattern
                             (str "jboss-home: "
                                  (.getAbsolutePath immutant-home)
                                  "/jboss \\n"))))
      
      (fact (str "env with an arg should work for lein " *generation*)
        (let [result (run-lein-env *generation* base-lein-env "immutant-home")]
          (:exit result) => 0
          (:out result)  => (.getAbsolutePath immutant-home))
        (let [result (run-lein-env *generation* base-lein-env "jboss-home")]
          (:exit result) => 0
          (:out result)  => (str (.getAbsolutePath immutant-home) "/jboss"))))))
