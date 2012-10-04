(ns leiningen.immutant.test
  (:refer-clojure :exclude [test])
  (:require fntest.jboss
            [leiningen.immutant.common :as common]
            [clojure.tools.nrepl :as repl])
  (:use [fntest.core :only [with-jboss with-deployment]]))

(def NREPL_PORT (or (System/getenv "NREPL_PORT") 7888))

(defn nrepl
  "Invoke command in remote nrepl"
  [command]
  (println "nrepl>" command)
  (with-open [conn (repl/connect :port NREPL_PORT)]
    (-> (repl/client conn 10000)
        (repl/client-session)
        (repl/message {:op :eval :code command})
        doall)))

(defn parse
  [results]
  (with-out-str
    (doseq [v results]
      (if-let [out (or (:out v) (:err v) (:value v))]
        (print out)))))

(defmacro command [form]
  `(println (parse (nrepl (repl/code ~form)))))

(defn run-tests
  "Invoke tests over nrepl"
  []
  (command (require '[clojure.test :as t]
                    '[immutant.utilities :as u]
                    '[bultitude.core :as b]))
  (command (let [nses (b/namespaces-in-dir (u/app-relative "test"))]
             (apply require nses)
             (apply t/run-tests nses))))

(defn deploy
  [project root]
  (with-deployment (str (:name project) "-testing")
    {:root root
     :swank-port nil
     :nrepl-port NREPL_PORT}))

(defn test
  "Starts Immutant, if necessary, deploys project, and runs tests in-container"
  [project root & {:keys [func] :or {func run-tests} :as opts}]
  (binding [fntest.jboss/*home* (common/get-jboss-home)]
    (with-jboss #((deploy project root) func) :lazy)))

