(ns leiningen.immutant.test
  (:refer-clojure :exclude [test])
  (:require fntest.jboss
            [leiningen.immutant.common  :as common]
            [immutant.deploy-tools.util :as util]
            [clojure.tools.nrepl        :as repl]
            [clojure.string             :as str])
  (:use [fntest.core :only [with-jboss with-deployment]]))

(def ^:dynamic *nrepl-port* 7888)

(def test-options
  [["-n" "--name"]
   ["-d" "--dir"]
   ["-p" "--port"]])

(defn nrepl
  "Invoke command in remote nrepl"
  [command]
  (with-open [conn (repl/connect :port *nrepl-port*)]
    (-> (repl/client conn Long/MAX_VALUE)
        (repl/client-session)
        (repl/message {:op :eval :code command})
        doall)))

(defn parse
  [results]
  (with-out-str
    (doseq [v results]
      (if-let [out (some v [:out :err :value])]
        (print out)))))

(defn execute [command]
  (println "nrepl>" command)
  (println (parse (nrepl command))))

(defn run-tests
  "Load test namespaces beneath dir and run them"
  [& [dir]]
  (execute (repl/code (require '[clojure.test :as t]
                               '[immutant.utilities :as u]
                               '[bultitude.core :as b])))
  (execute (str/replace-first (repl/code (let [nses (b/namespaces-in-dir (u/app-relative "TEST"))]
                                           (apply require nses)
                                           (apply t/run-tests nses)))
                              "TEST"
                              (or dir "test"))))

(defn run-in-container
  "Starts up an Immutant, if necessary, deploys an application named
   by name and located at root, and invokes f, after which the app is
   undeployed, and the Immutant, if started, is shut down"
  [name root f & [port]]
  (binding [*nrepl-port* (or port *nrepl-port*)
            fntest.jboss/*home* (common/get-jboss-home)]
    (let [deployer (with-deployment name
                     {:root root
                      :swank-port nil
                      :nrepl-port *nrepl-port*})]
      (with-jboss #(deployer f) :lazy))))

(defn test
  "Runs tests inside an Immutant, after starting one (if necessary) and deploying the project"
  [project root opts]
  (run-in-container (or (:name opts) (util/app-name project root))
                    root
                    #(run-tests (:dir opts))
                    (read-string (or (:port opts) (str *nrepl-port*)))))

