(ns leiningen.immutant.archive
  (:use leiningen.immutant.common)
  (:require [clojure.java.io   :as io]
            [leiningen.deps    :as deps]
            [leiningen.jar     :as jar])
  (:import (java.util.jar   JarEntry)
           (java.util.regex Pattern)))

;; FIXME - this currently relies too heavily on leiningen.jar internals

;; borrowed from leiningen.jar since it's private there
(defn- trim-leading-str [s to-trim]
  (.replaceAll s (str "^" (Pattern/quote to-trim)) ""))

;; leiningen.jar/copy-to-jar for :paths strips the prefix from
;; resources/, classes/, and src/ so they all appear at the root,
;; whereas we want to preserve the file system structure
(defmethod jar/copy-to-jar :full-path [project jar-os spec]
  (let [root (str (jar/unix-path (:root project)) \/)]
    (doseq [child (file-seq (io/file (:path spec)))]
      (let [path (reduce trim-leading-str (jar/unix-path (str child))
                         [root "/"])]
        (when-not (jar/skip-file? child path "")
          (.putNextEntry jar-os (doto (JarEntry. path)
                                  (.setTime (.lastModified child))))
          (io/copy child jar-os))))))

(defn- specs
  "specifies the files to be archived."
  [project]
  (concat
   [{:type :full-path
     :path (:library-path project)}
    {:type :full-path
     :path (:source-path project)}
    {:type :full-path
     :path (str (:root project) "/project.clj")}
    {:type :full-path
     :path (str (:root project) "/immutant.clj")}]
   (when (and (:resources-path project)
              (.exists (io/file (:resources-path project))))
     [{:type :full-path
       :path (:resources-path project)}])))

(defn- create-archive [project]
  (let [jar-path (archive-name project)]
    (jar/write-jar project jar-path (specs project))
    (println "Created" jar-path)))

(defn archive
  "Creates an Immutant archive from the current project"
  [project]
  ;; This will need to change for lein 2.0 - deps/deps currently
  ;; resolves and copies the deps to lib/. In 2.0,
  ;; it will just resolve the deps and do no copying.
  (deps/deps project)
  (create-archive project))
