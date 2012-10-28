(ns leiningen.immutant.run
  (:require [clojure.java.io            :as io]
            [clojure.string             :as str]
            [leiningen.immutant.common  :as common]
            [immutant.deploy-tools.util :as util]))


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
         (let [trampoline-file (System/getProperty "leiningen.trampoline-file")
               params (replace {"--clustered" "--server-config=standalone-ha.xml"} opts)
               command (concat [(standalone-sh)] params)]
           (apply println "Starting Immutant:" command)
           (.mkdirs (.getParentFile (io/file trampoline-file)))
           (spit trampoline-file 
                 (str/join " "
                  (map #(format "\"%s\"" %)
                       (replace {"--clustered" "--server-config=standalone-ha.xml"}
                                command)))))))))
