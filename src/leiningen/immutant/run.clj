(ns leiningen.immutant.run
  (:require [clojure.java.io            :as io]
            [leiningen.immutant.common  :as common]
            [jboss-as.management        :as api]
            [immutant.deploy-tools.util :as util]))

(defn- pump
  "Borrowed from leiningen.core/eval and modified."
  [reader out line-fn]
  (let [line-fn (or line-fn identity)]
    (loop [line (.readLine reader)]
      (when line
        (.write out (str (line-fn line) "\n"))
        (.flush out)
        (Thread/sleep 10)
        (recur (.readLine reader))))))

(defn- add-shutdown-hook! [f]
  (.addShutdownHook (Runtime/getRuntime) (Thread. f)))

(defn- sh
  "A version of clojure.java.shell/sh that streams out/err, and returns the process created.
It also allows applying a fn to each line of the output. Borrowed from leiningen.core/eval
and modified."
  [cmd & [line-fn]]
  (let [proc (.exec (Runtime/getRuntime)
                    (into-array cmd))]
    (.start (Thread.
             (bound-fn []
               (with-open [out (io/reader (.getInputStream proc))
                           err (io/reader (.getErrorStream proc))]
                 (doto (Thread. (bound-fn []
                                  (pump out *out* line-fn)))
                   .start
                   .join)
                 (doto (Thread. (bound-fn []
                                  (pump err *err* line-fn)))
                   .start
                   .join)))))
    proc))

(let [mgt-url (atom nil)
      jboss-home (common/get-jboss-home)]
  (defn- standalone-sh []
    (str (.getAbsolutePath jboss-home) "/bin/standalone."
         (if common/windows? "bat" "sh")))

  (defn- find-mgt-url [line]
    (if-let [match (re-find #"Http management interface listening on (.*/management)" line)]
      (reset! mgt-url (last match)))
    line)

  (defn- shutdown []
    (if-let [url @mgt-url]
      (binding [api/*api-endpoint* url]
        (println "Sending a shutdown message to" url)
        (try
          (api/shutdown)
          (catch Exception _)))))
  
  ;;TODO: fix run to use cli for options
  (defn run
    "Starts up the Immutant specified by ~/.lein/immutant/current or $IMMUTANT_HOME, displaying its console output"
    ([]
       (run nil))
    ([project & opts]
       (util/with-jboss-home jboss-home
         (and project (not (util/application-is-deployed? project nil nil))
              (common/err "WARNING: The current app may not be deployed - deploy with 'lein immutant deploy'"))
         (let [script (standalone-sh)
               params (replace {"--clustered" "--server-config=standalone-ha.xml"} opts)]
           (apply println "Starting Immutant:" script params)
           (let [proc (sh (into [script] params) find-mgt-url)]
             (add-shutdown-hook! shutdown)
             (.waitFor proc)))))))
