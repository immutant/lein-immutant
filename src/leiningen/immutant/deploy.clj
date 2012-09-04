(ns leiningen.immutant.deploy
  (:require [clojure.string                :as str]
            [clojure.tools.cli             :as cli]
            [leiningen.immutant.archive    :as archive-task]
            [leiningen.immutant.common     :as common]
            [immutant.deploy-tools.archive :as archive]
            [immutant.deploy-tools.deploy  :as deploy]
            [immutant.deploy-tools.util    :as util]))

(defn deploy 
  "Deploys a project to the Immutant specified by ~/.lein/immutant/current or $IMMUTANT_HOME"
  [project root & args] 
  (let [[opts other-args _] (apply cli/cli args
                                   (concat
                                    [["-a" "--archive" :flag true]
                                     ["-p" "--profiles" :parse-fn #(str/split % #",")]]
                                    archive-task/archive-options))
        jboss-home (common/get-jboss-home)
        deployed-file (if (:archive opts)
                        (deploy/deploy-archive jboss-home project root
                                               (archive-task/include-dependencies? opts)
                                               archive-task/copy-dependencies)
                        (deploy/deploy-dir jboss-home project root (:profiles opts)))]
    (println "Deployed" (util/app-name project root) "to" (.getAbsolutePath deployed-file))))

(defn undeploy
  "Undeploys a project from the Immutant specified by ~/.lein/immutant/current or $IMMUTANT_HOME"
  [project root]
  (let [app-name (util/app-name project root)
        jboss-home (common/get-jboss-home)
        deploy-path (.getAbsolutePath (util/deployment-dir jboss-home))]
    (if (deploy/undeploy jboss-home project root)
      (println "Undeployed" app-name "from" deploy-path)
      (println "No action taken:" app-name "is not deployed to" deploy-path))))

