(ns leiningen.immutant.undeploy
  (:use leiningen.immutant.common)
  (:require [clojure.java.io :as io]))

(defn undeploy [project]
  (with-jboss-home
    (if-let [files (seq (filter #(.exists %)
                                [(descriptor project)
                                 (dodeploy-marker project)
                                 (deployed-marker project)]))]
      (do
        (doall (map #(io/delete-file %) files))
        (println "Undeployed" (app-name project) "from" (.getAbsolutePath (deployment-dir))))
      (println "No action taken:" (app-name project) "is not deployed to" (.getAbsolutePath (deployment-dir))))))
