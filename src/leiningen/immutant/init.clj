(ns leiningen.immutant.init
  (:use leiningen.immutant.common)
  (:require [clojure.java.io :as io]
            [leiningen.new :as new]
            [leiningen.new.templates :as templates]
            [immutant.deploy-tools.util :as util]))

(defn init 
  "Adds a sample immutant.clj configuration file to an existing project"
  [project]
  (let [file (io/file (:root project) "immutant.clj")]
    (if-not (.exists file)
      (do
        (spit file ((templates/renderer "immutant") "immutant.clj" project))
        (println "Wrote sample immutant.clj"))
      (util/abort "immutant.clj already exists"))))

(defn new
  "Creates a new project skeleton initialized for Immutant.
This delegates to lein's builtin 'new' task using the 'immutant' template,
and is the same as calling 'lein new immutant project-name'"
  [project-name]
  (if (nil? project-name)
    (println "You must provide a project name.")
    (new/create "immutant" project-name)))

