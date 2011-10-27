(ns leiningen.immutant.deploy
  (:use leiningen.immutant.common)
  (:require [clojure.java.io :as io]))

(defn make-descriptor [project]
  (with-out-str
    (prn (assoc (:immutant project) :root (:target-dir project)))))

(defn deploy [project]
  (with-jboss-home
    (io/copy (make-descriptor project)
             (descriptor-file project))
    (io/copy ""
             (dodeploy-marker project))
    (println "Deployed" (app-name project) "to" (.getAbsolutePath (descriptor-file project)))))
