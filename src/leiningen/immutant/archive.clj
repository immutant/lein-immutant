(ns leiningen.immutant.archive
  (:require [clojure.java.io                :as io]
            [clojure.set                    :as set]
            [leiningen.core.classpath       :as cp]
            [leiningen.immutant.common      :as c]
            [immutant.deploy-tools.archive  :as archive]
            [immutant.dependency-exclusions :as depex]))

(def archive-options
  (concat
   [["-e" "--exclude-dependencies" :flag true]
    ["-n" "--name"]
    ["-v" "--version" :flag true]]
   c/descriptor-options))

(defn dependency-overrides [project]
  (as-> (:root project) %
      (io/file % "lib")
      (file-seq %)
      (next %)
      (map (memfn getName) %)
      (set %)))

(defn filter-overlap [specs project]
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

(defn filespecs-for-dep-type [project key prefix]
  (->> (cp/resolve-dependencies key project)
    (map io/file)
    (map #(archive/->VirtualFile
            (str prefix (.getName %)) %))))

(defn dependency-filespecs [project]
  (when project
    (concat
      (-> project
        depex/exclude-immutant-deps
        (filespecs-for-dep-type :dependencies "lib/")
         (filter-overlap project))
      (filespecs-for-dep-type project :plugins "plugin-deps/"))))

(defn merge-archive-options [project options]
  (let [include-deps? (not (:exclude-dependencies options))]
    (cond-> (assoc options :lein-profiles (c/extract-profiles project))
      include-deps? (assoc :extra-filespecs (dependency-filespecs project)
                           :resolve-dependencies false
                           :resolve-plugin-dependencies false))))

(defn archive
  "Creates an Immutant archive from a project

Creates an Immutant archive (suffixed with '.ima') in target/.  By
default, the archive file will be named after the project name in
project.clj. This can be overridden via the --name (or -n) option. If
the --version (or -v) flag is specified, the project version will be
appended to the name. This archive can be deployed in lieu of a
descriptor pointing to the app directory.

Any profiles that are active (via with-profile) will be captured and
applied when the app is deployed.

If passed a bare argument, the task will assume it is a path to a
project to be archived, and will switch to the context of that
project. This works when lein is invoked in or out of a project.

You can override the default context-path (based off of the deployment
name) and virtual-host with the --context-path and --virtual-host
options, respectively. 

If the --exclude-dependencies (or -e) option is provided, the
application's dependencies will not be included in the archive.

If the standard leiningen jar options :omit-source or :jar-exclusions
are set, they will be honored for archive creation."
  [project root options]
  (->> (archive/create
        project
        (io/file (:root project root))
        (io/file (:target-path project root))
        (merge-archive-options project options))
       .getAbsolutePath
       (println "Created")))
