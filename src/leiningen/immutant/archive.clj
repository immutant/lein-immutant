(ns leiningen.immutant.archive
  (:require [clojure.java.io               :as io]
            [leinjacker.utils              :as lj]
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

(defn copy-dependencies* [project]
  (when project
    (let [dependencies ((lj/try-resolve 'leiningen.core.classpath/resolve-dependencies)
                        :dependencies (strip-immutant-deps project))
          lib-dir (io/file (:root project) "lib")]
      (println "Copying" (count dependencies) "dependencies to ./lib")
      (if-not (.exists lib-dir)
        (.mkdir lib-dir))
      (doseq [dep (map io/file dependencies)]
        (io/copy dep (io/file lib-dir (.getName dep)))))))

(def copy-dependencies
  (if c/lein2?
    copy-dependencies*
    (lj/try-resolve 'leiningen.deps/deps)))

(defn archive
  "Creates an Immutant archive from a project"
  [project root options]
  (let [dest-dir (:root project root)
        jar-file (archive/create project root dest-dir
                                 (assoc options :copy-deps-fn copy-dependencies))]
    (c/verify-root-arg project root "archive")
    (println "Created" (.getAbsolutePath jar-file))))
