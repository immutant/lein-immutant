(ns leiningen.immutant.archive
  (:require [clojure.java.io               :as io]
            [clojure.tools.cli             :as cli]
            [leinjacker.utils              :as lj]
            [leiningen.immutant.common     :as common]
            [immutant.deploy-tools.archive :as archive]))

(def archive-options
  [["-i" "--include-dependencies" :flag true]])

(defn include-dependencies? [opts]
  (:include-dependencies opts))

(defn copy-dependencies* [project]
  (when project
    (let [dependencies ((lj/try-resolve 'leiningen.core.classpath/resolve-dependencies)
                        :dependencies project)
          lib-dir (io/file (:root project) "lib")]
      (println "Copying" (count dependencies) "dependencies to ./lib")
      (if-not (.exists lib-dir)
        (.mkdir lib-dir))
      (doseq [dep (map io/file dependencies)]
        (io/copy dep (io/file lib-dir (.getName dep)))))))

(def copy-dependencies
  (if common/lein2?
    copy-dependencies*
    (lj/try-resolve 'leiningen.deps/deps)))

(defn archive
  "Creates an Immutant archive from a project"
  [project root & args]
  (let [[opts other-args _] (apply cli/cli args archive-options)
        dest-dir (or (first other-args) (System/getProperty "user.dir"))]
    (let [jar-file (archive/create project root dest-dir
                                   (include-dependencies? opts) copy-dependencies)]
      (println "Created" (.getAbsolutePath jar-file)))))
