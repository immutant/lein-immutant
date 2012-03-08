(ns immutant.deploy-tools.archive
  (:use immutant.deploy-tools.util)
  (:require [clojure.java.io :as io])
  (:import (java.io         BufferedOutputStream FileOutputStream)
           (java.util.jar   JarEntry JarOutputStream)
           (java.util.regex Pattern)))

(def ^{:dynamic true} *dependency-resolver* (fn [_]))

;; Much of this is adapted from leiningen.jar so we don't
;; have to depend on lein internals, and because we generate
;; jars a bit differently

(defn ^{:internal true} trim-leading-str [s to-trim]
  (.replaceAll s (str "^" (Pattern/quote to-trim)) ""))

(defn ^{:internal true} unix-path [path]
  (.replace path "\\" "/"))

(defn ^{:internal true} copy-to-jar [root-path jar file]
  (let [root (str (unix-path root-path) \/)]
    (doseq [child (file-seq (io/file file))]
      (let [path (reduce trim-leading-str (unix-path (str child))
                         [root "/"])]
        (when (and (.exists child)
                   (not (.isDirectory child)))
          (.putNextEntry jar (doto (JarEntry. path)
                               (.setTime (.lastModified child))))
          (io/copy child jar))))))

(defn ^{:internal true} write-jar [root-path out-file filespecs]
  (with-open [jar (-> out-file
                      (FileOutputStream.)
                      (BufferedOutputStream.)
                      (JarOutputStream.))]
    (doseq [filespec filespecs]
      (copy-to-jar root-path jar filespec))))

(defn ^{:internal true} entry-points
  "Specifies the top level files to be archived, along with the dirs to be recursively archived."
  [project root-path]
  (map #(if (.startsWith % root-path)
          %
          (str root-path "/" %))
       (flatten
        [(:library-path project "lib")
         (:source-path project "src")
         (:resources-path project "resources")
         "project.clj"
         "immutant.clj"])))

(defn create [project root-dir dest-dir]
  (let [jar-file (io/file dest-dir (archive-name project root-dir))
        root-path (.getAbsolutePath root-dir)]
    (*dependency-resolver* project)
    (write-jar root-path jar-file (entry-points project root-path))
    jar-file))

