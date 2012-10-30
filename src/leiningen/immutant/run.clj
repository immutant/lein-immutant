(ns leiningen.immutant.run
  (:require [clojure.java.io            :as io]
            [leiningen.immutant.common  :as common]
            [immutant.deploy-tools.util :as util]))


(defn- pump
  "Borrowed from leiningen.core/eval"
  [reader out]
  (let [buffer (make-array Character/TYPE 1000)]
    (loop [len (.read reader buffer)]
      (when-not (neg? len)
        (.write out buffer 0 len)
        (.flush out)
        (Thread/sleep 100)
        (recur (.read reader buffer))))))

(defn- sh
  "A version of clojure.java.shell/sh that streams out/err, and returns the process created.
Borrowed from leiningen.core/eval and modified."
  [& cmd]
  (let [proc (.exec (Runtime/getRuntime)
                    (into-array cmd))]
    (.start (Thread. (bound-fn []
                       (with-open [out (io/reader (.getInputStream proc))
                                   err (io/reader (.getErrorStream proc))]
                         (let [pump-out (doto (Thread. (bound-fn [] (pump out *out*))) .start)
                               pump-err (doto (Thread. (bound-fn [] (pump err *err*))) .start)]
                           (.join pump-out)
                           (.join pump-err))))))
    proc))

(let [jboss-home (common/get-jboss-home)]
  (defn- standalone-sh []
    (str (.getAbsolutePath jboss-home) "/bin/standalone."
         (if common/windows? "bat" "sh")))

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
           (let [proc (apply sh script params)]
             (.waitFor proc)))))))
