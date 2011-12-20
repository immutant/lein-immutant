(ns leiningen.immutant.undeploy
  (:use leiningen.immutant.common)
  (:require [clojure.java.io :as io]))

(defn undeploy
  "Undeploys the current project from the Immutant specified by ~/.lein/immutant/current or $IMMUTANT_HOME"
  [project]
  (with-jboss-home
    (if-let [files (seq (filter #(.exists %)
                                [(descriptor-file project)
                                 (dodeploy-marker project)
                                 (deployed-marker project)]))]
      (do
        (doseq [file files]
          (io/delete-file file))
        (println "Undeployed" (app-name project) "from" (.getAbsolutePath (deployment-dir))))
      (println "No action taken:" (app-name project) "is not deployed to" (.getAbsolutePath (deployment-dir))))))
