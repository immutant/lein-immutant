(ns leiningen.immutant.init
  (:use leiningen.immutant.common)
  (:require [clojure.java.io            :as io]
            [clojure.string             :as str]
            [leiningen.new              :as new]
            [leiningen.new.templates    :as tmpl]
            [immutant.deploy-tools.util :as util]))

(defn sample-immutant-init [project]
  ((tmpl/renderer "immutant") "init.tmpl" project))

(defn init 
  "Adds a sample immutant.init namespace to the current project"
  [project]
  (let [file (io/file (:root project) "src/immutant/init.clj")]
    (if-not (.exists file)
      (do
        (-> file (.getParentFile) (.mkdirs))
        (spit file (sample-immutant-init project))
        (println "Wrote sample src/immutant/init.clj"))
      (util/abort "src/immutant/init.clj already exists"))))

(defn new
  "Creates a new project skeleton initialized for Immutant

This delegates to lein's builtin 'new' task using the 'immutant'
template, and is the same as calling 'lein new immutant project-name'.
It creates a standard Leiningen project and adds a sample
immutant.init namespace."
  [project-name]
  (if (nil? project-name)
    (println "You must provide a project name.")
    (new/create "immutant" project-name)))

