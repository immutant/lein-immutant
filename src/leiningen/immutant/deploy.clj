(ns leiningen.immutant.deploy
  (:require [clojure.string                :as str]
            [leiningen.immutant.archive    :as archive-task]
            [leiningen.immutant.common     :as c]
            [immutant.deploy-tools.archive :as archive]
            [immutant.deploy-tools.deploy  :as deploy]
            [immutant.deploy-tools.util    :as util]))

(def deploy-options
  (concat
   [["-a" "--archive" :flag true]
    (c/as-config-option ["-p" "--profiles" "--lein-profiles" :parse-fn #(str/split % #",")])  
    (c/as-config-option ["--context-path"])
    (c/as-config-option ["--virtual-host"])]
   archive-task/archive-options))

(def undeploy-options
  [["-n" "--name"]])

(defn deploy 
  "Deploys a project to the Immutant specified by ~/.lein/immutant/current or $IMMUTANT_HOME"
  [project root opts]
  (let [[options config] (c/group-options opts deploy-options)
        jboss-home (c/get-jboss-home)
        deployed-file (if (:archive options)
                        (deploy/deploy-archive jboss-home project root
                                               (assoc options
                                                 :copy-deps-fn archive-task/copy-dependencies))
                        (deploy/deploy-dir jboss-home project root options config))]
    (c/verify-root-arg project root "deploy")
    (println "Deployed" (util/app-name project root) "to" (.getAbsolutePath deployed-file))))

(defn undeploy
  "Undeploys a project from the Immutant specified by ~/.lein/immutant/current or $IMMUTANT_HOME"
  [project root opts]
  (let [app-name (str (util/app-name project root)
                      (if-let [name (:name opts)]
                        (str " (as: " name ")")))
        jboss-home (c/get-jboss-home)
        deploy-path (.getAbsolutePath (util/deployment-dir jboss-home))]
    (c/verify-root-arg project root "undeploy")
    (if (deploy/undeploy jboss-home project root opts)
      (println "Undeployed" app-name "from" deploy-path)
      (println "No action taken:" app-name "is not deployed to" deploy-path))))

