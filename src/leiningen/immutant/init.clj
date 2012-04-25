(ns leiningen.immutant.init
  (:use leiningen.immutant.common)
  (:require [clojure.java.io            :as io]
            [leiningen.new              :as new]
            [leiningen.immutant.shim    :as shim]
            [immutant.deploy-tools.util :as util]))

(defn sample-immutant-clj [project]
  (if shim/lein2?
    (((resolve 'leiningen.new.templates/renderer) "immutant") "immutant.clj" project)
    (.replace (slurp (io/resource "leiningen/new/immutant/immutant.clj")) "{{name}}"
              (:name project))))

(defn init 
  "Adds a sample immutant.clj configuration file to an existing project"
  [project]
  (let [file (io/file (:root project) "immutant.clj")]
    (if-not (.exists file)
      (do
        (spit file (sample-immutant-clj project))
        (println "Wrote sample immutant.clj"))
      (util/abort "immutant.clj already exists"))))

(defn new
  "Creates a new project skeleton initialized for Immutant.
This delegates to lein's builtin 'new' task using the 'immutant' template,
and is the same as calling 'lein new immutant project-name'"
  [project-name]
  (if (nil? project-name)
    (println "You must provide a project name.")
    (if shim/lein2?
      ((resolve 'leiningen.new/create) "immutant" project-name)
      (do
        ((resolve 'leiningen.new/new) project-name)
        (init (shim/read-project
               (-> (System/getProperty "leiningen.original.pwd")
                   (io/file (name (symbol project-name)) "project.clj")
                   (.getAbsolutePath))
               nil))))))

