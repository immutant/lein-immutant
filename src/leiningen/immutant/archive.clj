(ns leiningen.immutant.archive
  (:require [clojure.java.io               :as io]
            [leiningen.immutant.shim       :as shim]
            [immutant.deploy-tools.archive :as archive]))

(defn archive
  "Creates an Immutant archive from a project"
  ([project root]
     (archive project root (System/getProperty "user.dir")))
  ([project root dest-dir]
     (binding [archive/*dependency-resolver* shim/copy-dependencies-fn]
       (let [jar-file (archive/create project root dest-dir)]
         (println "Created" (.getAbsolutePath jar-file))))))
