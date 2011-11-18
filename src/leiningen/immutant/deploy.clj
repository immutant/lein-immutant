(ns leiningen.immutant.deploy
  (:use leiningen.immutant.common))

(defn make-descriptor [project]
  (with-out-str
    (prn (assoc (:immutant project) :root (:target-dir project)))))

(defn deploy 
  "Deploys the current project to the Immutant specified by $IMMUTANT_HOME"
  [project]
  (with-jboss-home
    (spit (descriptor-file project) (make-descriptor project))
    (spit (dodeploy-marker project) "")
    (println "Deployed" (app-name project) "to" (.getAbsolutePath (descriptor-file project)))))
