(ns leiningen.immutant.init
  (:use leiningen.immutant.common)
  (:use fleet)
  (:require [clojure.java.io :as io]))

(defn init 
  "Adds a sample immutant.clj configuration file to the project"
  [project]
  (let [file (io/file (:root project) "immutant.clj")]
    (if-not (.exists file)
      (do
        (io/copy
         (str ((fleet [ns] (slurp (io/resource "immutant.clj.fleet")))
               (:name project)))
         file)
        (println "Wrote sample ./immutant.clj"))
      (err "./immutant.clj already exists"))))

