(ns leiningen.immutant.eval
  (:refer-clojure :exclude [eval])
  (:require [clojure.tools.nrepl :as repl]))

(def ^:dynamic *nrepl-conn*)

(def eval-options
  [["-h" "--host"]
   ["-p" "--port"]])

(defn get-host [& [opts]]
  (or (:host opts) "localhost"))

(defn get-port [& [opts]]
  (read-string (or (:port opts) "7888")))

(defmacro with-connection
  "Takes :host and :port options"
  [opts & body]
  `(with-open [c# (repl/connect :host (get-host ~opts) :port (get-port ~opts))]
     (binding [*nrepl-conn* c#]
       ~@body)))

(defn nrepl
  "Invoke command in remote nrepl"
  [command]
  (-> (repl/client *nrepl-conn* Long/MAX_VALUE)
      (repl/client-session)
      (repl/message {:op :eval :code command})
      doall))

(defn parse
  "Summary of the nrepl results, with :err merged into :out in the correct order"
  [results]
  (reduce
   (fn [m [k v]]
     (case k
       (:out :err) (update-in m [:out] #(str % v))
       (assoc m k v)))
   {} (apply concat results)))

(defn execute [command]
  (println "nrepl>" command)
  (let [result (parse (nrepl command))]
    (if (:out result) (println (:out result)))
    (println (:value result))
    (read-string (:value result))))

(defn eval
  "Eval some code in a remote nrepl"
  [command & [opts]]
  (with-connection opts
    (execute command)))

