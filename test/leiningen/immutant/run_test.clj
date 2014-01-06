(ns leiningen.immutant.run-test
  (:use midje.sweet
        leiningen.immutant.test-helper)
  (:require [clojure.java.io :as io]))

(setup-test-env)

(defn call-run [& args]
  (with-tmp-jboss-home
    (apply run-lein "immutant" "run"
           (conj (vec args)
                 :dir "/tmp"
                 :env (assoc base-lein-env "JBOSS_HOME"  *tmp-jboss-home*)
                 :return-result? true))))

(println "\n==> Testing run")

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
      (re-find #"--server-config=standalone-ha.xml" out) => truthy))

  (fact "should massage the --offset arg"
    (let [regex #"-Djboss.socket.binding.port-offset=42"]
      (re-find regex (:out (call-run "--offset 42"))) => truthy
      (re-find regex (:out (call-run "--offset=42"))) => truthy
      (re-find regex (:out (call-run "-o 42"))) => truthy
      (re-find regex (:out (call-run "-o=42"))) => truthy))

  (fact "should massage the --node-name arg"
    (let [regex #"-Djboss.node.name=toby"]
      (re-find regex (:out (call-run "--node-name toby"))) => truthy
      (re-find regex (:out (call-run "--node-name=toby"))) => truthy
      (re-find regex (:out (call-run "-n toby"))) => truthy
      (re-find regex (:out (call-run "-n=toby"))) => truthy)))
