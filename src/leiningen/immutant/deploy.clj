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
  "Deploys a project to the current Immutant

If passed the --archive option, it will deploy an archive of the app
instead of a descriptor pointing to the app on disk. This will
currently recreate the archive on every deploy.  By default, the
deployment will be named after the project name in project.clj.  This
can be overridden via the --name (or -n) option.

Any profiles that are active (via with-profile) will be captured in
the :lein-profiles key in the descriptor and applied when the app is
deployed.

You can override the default context-path (based off of the deployment
name) and virtual-host with the --context-path and --virtual-host
options, respectively. This task can be run outside of a project dir
of the path to the project is provided.

By default, the plugin will locate the current Immutant by looking at
~/.lein/immutant/current. This can be overriden by setting the
$IMMUTANT_HOME environment variable."
  [project root opts]
  (let [[options config] (c/group-options opts deploy-options)
        jboss-home (c/get-jboss-home)
        deployed-file
        (if (:archive options)
          (deploy/deploy-archive jboss-home project root
                                 (assoc options
                                   :copy-deps-fn archive-task/copy-dependencies))
          (deploy/deploy-dir jboss-home project root options
                             (if-let [profiles (c/extract-profiles project)]
                               (assoc config :lein-profiles profiles)
                               config)))]
    (c/verify-root-arg project root "deploy")
    (if-let [given-profiles (:lein-profiles config)]
      (c/err (format "Warning: setting lein profiles via --lein-profiles is deprecated.
         Specify profiles using lein's with-profile higher order task:
           %s\n"
                     (c/deploy-with-profiles-cmd given-profiles))))
    (println "Deployed" (util/app-name project root) "to" (.getAbsolutePath deployed-file))))

(defn undeploy
  "Undeploys a project from the current Immutant

If the `--name` option was used to deploy the app, you'll need to pass
the same name to undeploy as well. This task can be run outside of a
project dir of the path to the project is provided.

By default, the plugin will locate the current Immutant by looking at
~/.lein/immutant/current. This can be overriden by setting the
$IMMUTANT_HOME environment variable."
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

