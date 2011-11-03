(ns leiningen.immutant.init
  (:use leiningen.immutant.common)
  (:use fleet)
  (:require [clojure.java.io :as io]))

(defn init [project]
  (let [file (io/file (:root project) "immutant.clj")]
    (if-not (.exists file)
      (do
        (io/copy
         (str ((fleet [ns] (slurp (io/resource "immutant.clj.fleet")))
               (:name project)))
         file)
        (println "Wrote sample immutant.clj"))
      (println "immutant.clj already exists!"))))

