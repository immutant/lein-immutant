(ns leiningen.immutant.init
  (:use leiningen.immutant.common)
  (:use fleet)
  (:require [clojure.java.io :as io]
            [leiningen.new :as lnew]
            [leiningen.core :as lcore]))

(defn init 
  "Adds a sample immutant.clj configuration file to an existing project"
  [project]
  (let [file (io/file (:root project) "immutant.clj")]
    (if-not (.exists file)
      (do
        (io/copy
         (str ((fleet [ns] (slurp (io/resource "immutant.clj.fleet")))
               (:name project)))
         file)
        (println "Wrote sample immutant.clj"))
      (abort "immutant.clj already exists"))))

(defn new
  "Create a new project skeleton initialized for immutant."
  [project-name]
  (lnew/new project-name)
  (init (lcore/read-project
         (-> (System/getProperty "leiningen.original.pwd")
             (io/file (name (symbol project-name)) "project.clj")
             (.getAbsolutePath)))))

