(ns leiningen.immutant.shim
  "Functions for dealing with operations across different lein versions."
  (:require [clojure.string  :as str]
            [clojure.java.io :as io]))

(def ^{:doc "True if running under lein2"}
  lein2?
  (or (try
        (require 'leiningen.core.main
                 'leiningen.core.eval
                 'leiningen.core.project
                 'leiningen.core.classpath
                 'leiningen.core.user
                 'leiningen.new.templates)
        true
        (catch java.io.FileNotFoundException _))
      (do ;; lein1
        (require 'leiningen.core
                 'leiningen.compile
                 'leiningen.deps
                 'leiningen.util.paths)
        false)))

(defn resolve-for-current-lein [lein1 lein2]
  (resolve (if lein2? lein2 lein1)))

(defn copy-dependencies [project]
  (when project
    (let [dependencies ((resolve 'leiningen.core.classpath/resolve-dependencies)
                        :dependencies project)
          lib-dir (io/file (:root project) "lib")]
      (println "Copying" (count dependencies) "dependencies to ./lib")
      (if-not (.exists lib-dir)
        (.mkdir lib-dir))
      (doseq [dep (map io/file dependencies)]
        (io/copy dep (io/file lib-dir (.getName dep)))))))

(def copy-dependencies-fn
  (resolve-for-current-lein
   'leiningen.deps/deps
   'leiningen.immutant.shim/copy-dependencies))

(def lein-read-project-fn
  (resolve-for-current-lein
   'leiningen.core/read-project
   'leiningen.core.project/read))

(defn read-project [file profiles]
  (if lein2?
    ((resolve 'leiningen.core.project/read) file profiles)
    ((resolve 'leiningen.core/read-project) file))) ;; ignore profiles

(def leiningen-home-fn
  (resolve-for-current-lein
   'leiningen.util.paths/leiningen-home
   'leiningen.core.user/leiningen-home))

(def lein-sh-fn
  (resolve-for-current-lein
   'leiningen.compile/sh
   'leiningen.core.eval/sh))
