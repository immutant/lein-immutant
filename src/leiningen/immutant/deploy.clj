(ns leiningen.immutant.deploy
  (:use leiningen.immutant.common)
  (:require [clojure.java.io            :as io]
            [leiningen.immutant.archive :as archive]))

(defn make-descriptor [project]
  (with-out-str
    (prn (assoc (:immutant project) :root (:target-dir project)))))

(defn deploy-archive [project]
  (let [archive-file (io/file (archive-name project))
        deployed-file (deployed-archive-file project)]
    (if (.exists archive-file)
      (println (.getName archive-file) "already exists, skipping archive step.")
      (archive/archive project))
    (io/copy archive-file deployed-file)
    (spit (archive-dodeploy-marker project) "")
    deployed-file))

(defn deploy-dir [project]
  (let [deployed-file (descriptor-file project)]
    (spit deployed-file (make-descriptor project))
    (spit (dodeploy-marker project) "")
    deployed-file))

(defn deploy 
  "Deploys the current project to the Immutant specified by ~/.lein/immutant/current or $IMMUTANT_HOME"
  [project & args] 
  (with-jboss-home
    (let [deployed-file
          (if (some #{"--archive"} args)
            (deploy-archive project)
            (deploy-dir project))]
      (println "Deployed" (app-name project) "to" (.getAbsolutePath deployed-file)))))

(defn undeploy
  "Undeploys the current project from the Immutant specified by ~/.lein/immutant/current or $IMMUTANT_HOME"
  [project]
  (with-jboss-home
    (if-let [files (seq (filter #(.exists %)
                                (map #(% project)
                                     [descriptor-file
                                      dodeploy-marker
                                      deployed-marker
                                      deployed-archive-file
                                      archive-dodeploy-marker
                                      archive-deployed-marker])))]
      (do
        (doseq [file files]
          (io/delete-file file))
        (println "Undeployed" (app-name project) "from" (.getAbsolutePath (deployment-dir))))
      (println "No action taken:" (app-name project) "is not deployed to" (.getAbsolutePath (deployment-dir))))))

