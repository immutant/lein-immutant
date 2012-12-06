(ns leiningen.immutant.init
  (:use leiningen.immutant.common)
  (:require [clojure.java.io            :as io]
            [clojure.string             :as str]
            [leinjacker.utils           :as lj]
            [immutant.deploy-tools.util :as util]))

(defn sample-immutant-clj [project]
  (if lein2?
    (((lj/try-resolve 'leiningen.new.templates/renderer) "immutant") "init.tmpl" project)
    (str/replace (slurp (io/resource "leiningen/new/immutant/init.tmpl"))
                 #"(\{\{raw-name\}\}|\{\{namespace\}\}|\{\{nested-dirs\}\})"
                 (:name project))))

(defn init 
  "Adds a sample immutant.init namespace to an existing project"
  [project]
  (let [file (io/file (:root project) "src/immutant/init.clj")]
    (if-not (.exists file)
      (do
        (-> file (.getParentFile) (.mkdirs))
        (spit file (sample-immutant-clj project))
        (println "Wrote sample src/immutant/init.clj"))
      (util/abort "src/immutant/init.clj already exists"))))

(defn new
  "Creates a new project skeleton initialized for Immutant.
This delegates to lein's builtin 'new' task using the 'immutant' template,
and is the same as calling 'lein new immutant project-name'"
  [project-name]
  (if (nil? project-name)
    (println "You must provide a project name.")
    (if lein2?
      ((lj/try-resolve 'leiningen.new/create) "immutant" project-name)
      (do
        ((lj/try-resolve 'leiningen.new/new) project-name)
        (init (lj/read-lein-project
               (-> (System/getProperty "leiningen.original.pwd")
                   (io/file (name (symbol project-name)) "project.clj")
                   (.getAbsolutePath))
               nil))))))

