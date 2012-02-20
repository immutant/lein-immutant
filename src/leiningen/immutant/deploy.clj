(ns leiningen.immutant.deploy
  (:require [clojure.java.io              :as io]
            [leiningen.immutant.archive   :as archive]
            [leiningen.immutant.common    :as common]
            [immutant.deploy-tools.deploy :as deploy]
            [immutant.deploy-tools.util   :as util]))

(defn deploy 
  "Deploys the current project to the Immutant specified by ~/.lein/immutant/current or $IMMUTANT_HOME"
  [project root & args] 
  (let [jboss-home (common/get-jboss-home)
        deployed-file (if (some #{"--archive"} args)
                        (do
                          (archive/archive project root root) ;; FIXME: this can go away once we move to lein2
                          (deploy/deploy-archive jboss-home project root))
                        (deploy/deploy-dir jboss-home project root))]
    (println "Deployed" (util/app-name project root) "to" (.getAbsolutePath deployed-file))))

(defn undeploy
  "Undeploys the current project from the Immutant specified by ~/.lein/immutant/current or $IMMUTANT_HOME"
  [project root]
  (let [app-name (util/app-name project root)
        jboss-home (common/get-jboss-home)
        deploy-path (.getAbsolutePath (util/deployment-dir jboss-home))]
    (if (deploy/undeploy jboss-home project root)
      (println "Undeployed" app-name "from" deploy-path)
      (println "No action taken:" app-name "is not deployed to" deploy-path))))

