(ns leiningen.immutant.run
  (:use [leiningen.compile :only [sh]])
  (:require [leiningen.immutant.common  :as common]
            [immutant.deploy-tools.util :as util]))

(defn standalone-sh []
  (str (.getAbsolutePath util/*jboss-home*) "/bin/standalone.sh"))

(defn run
  "Starts up the Immutant specified by ~/.lein/immutant/current or $IMMUTANT_HOME, displaying its console output"
  ([]
     (run nil))
  ([project & opts]
     (util/with-jboss-home
       (and project (not (util/application-is-deployed? project nil))
            (common/err "WARNING: The current app is not deployed - deploy with 'lein immutant deploy'"))
       (let [script (standalone-sh)
             params (replace {"--clustered" "--server-config=standalone-ha.xml"} opts)]
         (apply println "Starting Immutant:" script params)
         (apply sh script params)))))
