(ns leiningen.immutant.deploy
  (:use leiningen.immutant.common)
  (:require [clojure.java.io :as io]))

(defn make-descriptor [project]
  (with-out-str
    (prn (assoc (:immutant project) :root (:target-dir project)))))

(defn deploy [project]
  (with-jboss-home
    (let [descriptor (io/file jboss-home "standalone" "deployments" (descriptor-name project))]
      (io/copy (make-descriptor project)
               descriptor)
      (io/copy ""
               (dodeploy-marker descriptor))
      (println "Deployed" (app-name project) "to" (.getAbsolutePath descriptor)))))
