(ns leiningen.immutant.undeploy
  (:use leiningen.immutant.common)
  (:require [clojure.java.io :as io]))

(defn undeploy
  "Undeploys the current project from the Immutant specified by $IMMUTANT_HOME"
  [project]
  (with-jboss-home
    (if-let [files (seq (filter #(.exists %)
                                [(descriptor-file project)
                                 (dodeploy-marker project)
                                 (deployed-marker project)]))]
      (do
        (doall (map #(io/delete-file %) files))
        (println "Undeployed" (app-name project) "from" (.getAbsolutePath (deployment-dir))))
      (println "No action taken:" (app-name project) "is not deployed to" (.getAbsolutePath (deployment-dir))))))
