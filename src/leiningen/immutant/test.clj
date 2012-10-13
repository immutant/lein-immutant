(ns leiningen.immutant.test
  (:refer-clojure :exclude [test])
  (:require fntest.jboss
            [leiningen.immutant.eval    :as eval]
            [leiningen.immutant.common  :as common]
            [immutant.deploy-tools.util :as util]
            [clojure.tools.nrepl        :as repl]
            [clojure.string             :as str])
  (:use [fntest.core :only [with-jboss with-deployment]]))

(def test-options
  [["-n" "--name"]
   ["-d" "--dir"]
   ["-p" "--port"]])

(def deps-command (repl/code
                   (use 'immutant.dev
                        'clojure.pprint)
                   (pprint (add-dependencies! "TEST-DIR" '[bultitude "0.1.7"]))))

(def load-command (repl/code
                   (require '[clojure.test :as t]
                            '[immutant.utilities :as u]
                            '[bultitude.core :as b])))

(def run-command (repl/code
                  (let [nses (b/namespaces-in-dir (u/app-relative "TEST-DIR"))]
                    (apply require nses)
                    (t/successful? (apply t/run-tests nses)))))

(defn run-tests
  "Load test namespaces beneath dir and run them"
  [& [opts]]
  (let [dir (or (:dir opts) "test")]
    (eval/with-connection opts
      (eval/execute (str/replace deps-command "TEST-DIR" dir))
      (eval/execute load-command)
      (eval/execute (str/replace run-command "TEST-DIR" dir)))))

(defn run-in-container
  "Starts up an Immutant, if necessary, deploys an application named
   by name and located at root, and invokes f, after which the app is
   undeployed, and the Immutant, if started, is shut down"
  [name root f & [opts]]
  (binding [fntest.jboss/*home* (common/get-jboss-home)]
    (let [deployer (with-deployment name
                     {:root root
                      :swank-port nil
                      :nrepl-port (eval/get-port opts)})]
      (with-jboss #(deployer f) :lazy))))

(defn test
  "Runs tests inside an Immutant, after starting one (if necessary) and deploying the project"
  [project root opts]
  (when-not (run-in-container (or (:name opts) (util/app-name project root))
                              root
                              #(run-tests opts)
                              opts)
    (println "Tests failed")
    (System/exit -1)))

