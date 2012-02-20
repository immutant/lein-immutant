(ns leiningen.immutant.archive
  (:require [clojure.java.io               :as io]
            [leiningen.deps                :as deps]
            [immutant.deploy-tools.archive :as archive]))

(defn archive
  "Creates an Immutant archive from the current project"
  ([project root]
     (archive project root (System/getProperty "user.dir")))
  ([project root dest-dir]
     ;; This will need to change for lein 2.0 - deps/deps currently
     ;; resolves and copies the deps to lib/. In 2.0,
     ;; it will just resolve the deps and do no copying.
     (when (seq project)
       (deps/deps project))
     (let [jar-file (archive/create project root dest-dir)]
       (println "Created" (.getAbsolutePath jar-file)))))
