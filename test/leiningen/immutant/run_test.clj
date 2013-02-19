(ns leiningen.immutant.run-test
  (:use midje.sweet
        leiningen.immutant.test-helper)
  (:require [clojure.java.io :as io]))

(setup-test-env)

(defn call-run [& args]
  (with-tmp-jboss-home
    (apply run-lein *generation* "immutant" "run"
           (conj (vec args)
                 :dir "/tmp"
                 :env (assoc base-lein-env "JBOSS_HOME"  *tmp-jboss-home*)
                 :return-result? true))))

(for-all-generations
 (println "\n==> Testing run with lein generation" *generation*)

 (facts "run"

   (fact "should call standalone.sh"
     (let [{:keys [out]} (call-run)]
       (re-find #"called" out) => truthy))
   
   (fact "should pump stdout and stderr"
     (let [{:keys [out err]} (call-run)]
       (re-find #"to stdout" out) => truthy
       (re-find #"to stderr" out) => falsey
       (re-find #"to stderr" err) => truthy
       (re-find #"to stdout" err) => falsey))
   
   (fact "should pass through options"
     (let [{:keys [out]} (call-run "a" "b")]
       (re-find #"a b" out) => truthy))
   
   (fact "should massage the --clustered arg"
     (let [{:keys [out]} (call-run "--clustered")]
       (re-find #"--server-config=standalone-ha.xml" out) => truthy))))






