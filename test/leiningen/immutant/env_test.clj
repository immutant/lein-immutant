(ns leiningen.immutant.env-test
  (:use midje.sweet
        leiningen.immutant.test-helper)
  (:require [clojure.java.io :as io]))

(setup-test-env)

(defn run-lein-env [env & args]
  (apply run-lein "immutant" "env"
         (conj (vec args)
               :dir "/tmp"
               :env env
               :return-result? true)))

(println "\n==> Testing env")

(facts "with IMMUTANT_HOME set"
  (let [immutant-home (io/file "/tmp" "immutant-home")
        env (assoc base-lein-env
              "IMMUTANT_HOME" (.getAbsolutePath immutant-home))
        jboss-home (io/file immutant-home "jboss")]
    (.mkdirs jboss-home)
    
    (fact "env should work"
      (let [result (run-lein-env env)]
        (:exit result) => 0
        (:out result)  => (re-pattern
                           (str "immutant-home: "
                                (.getAbsolutePath immutant-home)
                                " \\(from: \\$IMMUTANT_HOME\\)"))
        (:out result)  => (re-pattern
                           (str "jboss-home: "
                                (.getAbsolutePath jboss-home)
                                " \\n"))))
    
    (fact "env with an arg should work"
      (let [result (run-lein-env env "immutant-home")]
        (:exit result) => 0
        (:out result)  => (.getAbsolutePath immutant-home))
      (let [result (run-lein-env env "jboss-home")]
        (:exit result) => 0
        (:out result)  => (.getAbsolutePath jboss-home)))
    
    (fact "env with an invalid arg should work"
      (let [result (run-lein-env env "ham")]
        (:exit result) => 0
        (:out result)  => ""
        (:err result)  => "ham is an unknown env key. Valid keys are: (immutant-home jboss-home)\n")))

  (facts "with no jboss dir under IMMUTANT_HOME"
    (let [immutant-home (io/file "/tmp" "immutant-home-no-jboss")
          env (assoc base-lein-env
                "IMMUTANT_HOME" (.getAbsolutePath immutant-home))]
      (.mkdirs immutant-home)
      
      (fact "env should work"
        (let [result (run-lein-env env)]
          (:exit result) => 0
          (:out result)  => (re-pattern
                             (str "immutant-home: "
                                  (.getAbsolutePath immutant-home)
                                  " \\(from: \\$IMMUTANT_HOME\\)"))
          (:out result)  => (re-pattern
                             (str "jboss-home: "
                                  (.getAbsolutePath immutant-home)
                                  " \\n"))))
      
      (fact "env with an arg should work"
        (let [result (run-lein-env env "immutant-home")]
          (:exit result) => 0
          (:out result)  => (.getAbsolutePath immutant-home))
        (let [result (run-lein-env env "jboss-home")]
          (:exit result) => 0
          (:out result)  => (.getAbsolutePath immutant-home))))))

(facts "with IMMUTANT_HOME not set"
  (let [immutant-home (io/file (io/resource "user-home/.immutant/current"))
        jboss-home (io/file immutant-home "jboss")]
    (fact "env should work"
      (let [result (run-lein-env base-lein-env)]
        (:exit result) => 0
        (:out result)  => (re-pattern
                           (str "immutant-home: "
                                (.getAbsolutePath immutant-home)
                                " \\n"))
        (:out result)  => (re-pattern
                           (str "jboss-home: "
                                (.getAbsolutePath jboss-home)
                                " \\n"))))
    
    (fact "env with an arg should work"
      (let [result (run-lein-env base-lein-env "immutant-home")]
        (:exit result) => 0
        (:out result)  => (.getAbsolutePath immutant-home))
      (let [result (run-lein-env base-lein-env "jboss-home")]
        (:exit result) => 0
        (:out result)  => (.getAbsolutePath jboss-home)))))
