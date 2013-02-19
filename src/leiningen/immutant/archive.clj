(ns leiningen.immutant.archive
  (:require [clojure.java.io               :as io]
            [leiningen.core.classpath      :as cp]
            [leiningen.immutant.common     :as c]
            [immutant.deploy-tools.archive :as archive]))

(def archive-options
  [["-i" "--include-dependencies" :flag true]
   ["-n" "--name"]])

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
pointing to the app directory. If the --include-dependencies (or -i)
option is provided, all of the application's dependencies will be
included in the archive as well. This task can be run outside of a
project dir of the path to the project is provided."
  [project root options]
  (let [dest-dir (:root project root)
        jar-file (archive/create project root dest-dir
                                 (assoc options :copy-deps-fn copy-dependencies))]
    (c/verify-root-arg project root "archive")
    (println "Created" (.getAbsolutePath jar-file))))
