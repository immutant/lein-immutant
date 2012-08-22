(ns leiningen.immutant.run
  (:require [leiningen.immutant.common  :as common]
            [leinjacker.eval            :as eval]
            [immutant.deploy-tools.util :as util]))

(let [jboss-home (common/get-jboss-home)]
  (defn standalone-sh []
    (str (.getAbsolutePath jboss-home) "/bin/standalone."
         (if common/windows? "bat" "sh")))

  (defn run
    "Starts up the Immutant specified by ~/.lein/immutant/current or $IMMUTANT_HOME, displaying its console output"
    ([]
       (run nil))
    ([project & opts]
       (util/with-jboss-home jboss-home
         (and project (not (util/application-is-deployed? project nil))
              (common/err "WARNING: The current app is not deployed - deploy with 'lein immutant deploy'"))
         (let [script (standalone-sh)
               params (replace {"--clustered" "--server-config=standalone-ha.xml"} opts)]
           (apply println "Starting Immutant:" script params)
           (when common/windows?
             (println "\n*********************************************************************************************************")
             (println "NOTE: ^C currently won't cause Immutant to exit on Windows. You'll need to kill it with the Task Manager.")
             (println "*********************************************************************************************************\n"))
           (apply eval/sh script params))))))
