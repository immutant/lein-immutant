(ns leiningen.immutant.archive
  (:require [clojure.java.io                :as io]
            [clojure.set                    :as set]
            [leiningen.core.classpath       :as cp]
            [leiningen.immutant.common      :as c]
            [immutant.deploy-tools.archive  :as archive]
            [immutant.dependency-exclusions :as depex]))

(def archive-options
  (concat
   [["-i" "--include-dependencies" :flag true]
    ["-n" "--name"]]
   c/descriptor-options))

(defn dependency-overrides [project]
  (as-> (:root project) %
      (io/file % "lib")
      (file-seq %)
      (next %)
      (map (memfn getName) %)
      (set %)))

(defn filter-overlap [project specs]
  (let [overrides (dependency-overrides project)
        overlap (set/intersection
                 overrides
                 (->> specs (map :name) (map #(subs % 4)) set))]
    (if (seq overlap)
      (println "Note: the following jars from the local repo are overriden by versions in lib/:"
               (clojure.string/join ", " overlap)))
    (remove #(some #{(:name %)}
                   (map (partial str "lib/")
                        overrides))
            specs)))

(defn dependency-filespecs [project]
  (when project
    (->> (cp/resolve-dependencies
          :dependencies
          (depex/exclude-immutant-deps project))
         (map io/file)
         (map #(archive/->VirtualFile
                (str "lib/" (.getName %)) %))
         (filter-overlap project))))

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
  (->> (archive/create
        project
        (io/file (:root project root))
        (System/getProperty "user.dir")
        (cond-> options
          true
          (assoc :lein-profiles (c/extract-profiles project))
          (:include-dependencies options)
          (assoc :extra-filespecs (dependency-filespecs project))))
       .getAbsolutePath
       (println "Created")))
