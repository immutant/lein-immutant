(ns leiningen.immutant.deploy
  (:refer-clojure :exclude [list])
  (:require [clojure.java.io               :as io]
            [clojure.string                :as str]
            [leiningen.immutant.archive    :as archive-task]
            [leiningen.immutant.common     :as c]
            [immutant.deploy-tools.archive :as archive]
            [immutant.deploy-tools.deploy  :as deploy]
            [immutant.deploy-tools.util    :as util]))

(defn ^:internal deployed-files []
  (filter
   #(re-find #"(\.clj|\.ima)$" (.getName %))
   (file-seq (util/deployment-dir (c/get-jboss-home)))))

(def deploy-options
  (concat
   [["-a" "--archive" :flag true]
    (c/as-config-option ["-p" "--profiles" "--lein-profiles" :parse-fn #(str/split % #",")])]
   archive-task/archive-options))

(defn- glob->regex [glob]
  (-> glob
      (str/replace #"(?<![.\\])\*" ".*")
      (str/replace #"(?<![.\\])\?" ".?")
      (str/replace #"^" "^")
      (#(if (re-find #"\.(clj|ima)$" %)
          (str % "$")
          (str % "\\.(clj|ima)$"))) 
      re-pattern))

(defn ^:internal  matching-deployments [pattern-str]
  (if pattern-str
    (filter
     #(re-find (glob->regex pattern-str) (.getName %))
     (deployed-files))))

(defn ^:internal undeploy-descriptors [descriptors]
  (doseq [f descriptors]
    (deploy/rm-deployment-files [f])
    (println "Undeployed" (.getCanonicalPath f))))

(defn deploy 
  "Deploys a project to the current Immutant

If passed the --archive option, it will deploy an archive of the app
instead of a descriptor pointing to the app on disk. This will
currently recreate the archive on every deploy.  By default, the
deployment will be named after the project name in project.clj.  This
can be overridden via the --name (or -n) option.

If passed a bare argument, the task will assume it is a path to a
project to be deployed, and will switch to the context of that
project. This works when lein is invoked in or out of a project.

Any profiles that are active (via with-profile) will be captured in
the :lein-profiles key in the descriptor and applied when the app is
deployed.

You can override the default context-path (based off of the deployment
name) and virtual-host with the --context-path and --virtual-host
options, respectively. 

By default, the plugin will locate the current Immutant by looking at
~/.lein/immutant/current. This can be overriden by setting the
$IMMUTANT_HOME environment variable."
  [project root opts]
  (let [[options config] (c/group-options opts deploy-options)
        jboss-home (c/get-jboss-home)
        profiles (c/extract-profiles project)
        deployed-file
        (if (:archive options)
          (deploy/deploy-archive jboss-home
                                 project
                                 (io/file (:root project root))
                                 (System/getProperty "user.dir")
                                 (-> options
                                     (merge config)
                                     (dissoc :archive)
                                     (assoc :copy-deps-fn archive-task/copy-dependencies
                                            :lein-profiles profiles)))
          (deploy/deploy-dir jboss-home project root options
                             (if profiles
                               (assoc config :lein-profiles profiles)
                               config)))]
    (if-let [given-profiles (:lein-profiles config)]
      (c/err (format "Warning: setting lein profiles via --lein-profiles is deprecated.
         Specify profiles using lein's with-profile higher order task:
           %s\n"
                     (c/deploy-with-profiles-cmd given-profiles))))
    (println "Deployed" (util/app-name project root) "to" (.getAbsolutePath deployed-file))))

(defn undeploy
  "Undeploys a project from the current Immutant

If passed a bare argument, the task will first treat it as a glob
argument specifying one or more deployments to undeploy. If it does
not match any current deployments, it is assumed to be a path to a
project to be undeployed, and will switch to the context of that
project. This works when lein is invoked in or out of a project.

Examples of matching deployment names with globs:

Given    Will undeploy
-----    -------------
ham      ham.clj, ham.ima
ham.clj  ham.clj
ha?      ham.clj, ham.ima, hat.clj, hat.ima
*h*      ham.clj, ham.ima, ship.clj, ship.ima, moh.clj, moh.ima
h*.clj   ham.clj, hat.clj
*        everything

Note that depending on your shell, you may have to quote or escape *
and ? in globs.

By default, the plugin will locate the current Immutant by looking at
~/.lein/immutant/current. This can be overriden by setting the
$IMMUTANT_HOME environment variable."
  [project root opts]
  (let [app-name (util/app-name project root)
        jboss-home (c/get-jboss-home)
        deploy-path (.getAbsolutePath (util/deployment-dir jboss-home))]
    (if-let [deployments (matching-deployments app-name)]
      (undeploy-descriptors deployments)
      (println "No action taken:" app-name "is not deployed to" deploy-path))))

(defn list
  "Lists currently deployed applications along with the status of each

This will currently only list disk-based deployments, and won't list
anything deployed via the JBoss CLI or management interface."
  []
  (println (format "The following applications are deployed to %s:"
                   (c/get-immutant-home)))
  (->>
   (for [f (deployed-files)]
     (format "  %-40s (status: %s)"
             (.getName f)
             (condp #(.exists (%1 %2)) f
               util/dodeploy-marker "deploying"
               util/deployed-marker "deployed"
               util/failed-marker   "failed"
               "unknown")))
   (str/join "\n")
   println))
