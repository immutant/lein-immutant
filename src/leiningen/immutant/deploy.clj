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
      (println "A" (.getName archive-file) "already exists, skipping archive step.")
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
