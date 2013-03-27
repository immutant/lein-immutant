(ns leiningen.immutant.archive
  (:require [clojure.java.io               :as io]
            [leiningen.core.classpath      :as cp]
            [leiningen.immutant.common     :as c]
            [immutant.deploy-tools.archive :as archive]))

(def archive-options
  (concat
   [["-i" "--include-dependencies" :flag true]
    ["-n" "--name"]]
   c/descriptor-options))

(defn- strip-immutant-deps [project]
  (update-in project [:dependencies]
             (fn [deps] (remove #(and
                                  (= "org.immutant" (namespace (first %)))
                                  (.startsWith (name (first %)) "immutant"))
                                deps))))

(defn copy-dependencies [project]
  (when project
    (let [dependencies (cp/resolve-dependencies
                        :dependencies (strip-immutant-deps project))
          lib-dir (io/file (:root project) "lib")]
      (println "Copying" (count dependencies) "dependencies to ./lib")
      (if-not (.exists lib-dir)
        (.mkdir lib-dir))
      (doseq [dep (map io/file dependencies)]
        (io/copy dep (io/file lib-dir (.getName dep)))))))

(defn archive
  "Creates an Immutant archive from a project

Creates an Immutant archive (suffixed with '.ima') in the current
directory. By default, the archive file will be named after the
project name in project.clj. This can be overridden via the --name (or
-n) option. This archive can be deployed in lieu of a descriptor
pointing to the app directory.

Any profiles that are active (via with-profile) will be captured and
applied when the app is deployed.

If passed a bare argument, the task will assume it is a path to a
project to be archived, and will switch to the context of that
project. This works when lein is invoked in or out of a project.

You can override the default context-path (based off of the deployment
name) and virtual-host with the --context-path and --virtual-host
options, respectively. 

If the --include-dependencies (or -i) option is provided, all of the
application's dependencies will be included in the archive as well.

If the standard leiningen jar options :omit-source or :jar-exclusions
are set, they will be honored for archive creation."
  [project root options]
  (let [jar-file (archive/create project
                                 (io/file (:root project root))
                                 (System/getProperty "user.dir")
                                 (assoc options
                                   :copy-deps-fn copy-dependencies
                                   :lein-profiles (c/extract-profiles project)))]
    (println "Created" (.getAbsolutePath jar-file))))
