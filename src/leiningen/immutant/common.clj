(ns leiningen.immutant.common
  (:require [clojure.java.io        :as io]
            [leiningen.help         :as lhelp]
            [leiningen.core.project :as project]
            [leiningen.core.user    :as user]))

(defn get-application-root [args]
  (io/file (or (first (filter #(not (.startsWith % "--")) args))
               (System/getProperty "user.dir"))))

(defn immutant-storage-dir []
  (.getAbsolutePath
   (doto (io/file (user/leiningen-home) "immutant")
     .mkdirs)))

(def current-path
  (io/file (immutant-storage-dir) "current"))

(defn get-immutant-home []
  (if-let [immutant-home (System/getenv "IMMUTANT_HOME")]
    (io/file immutant-home)
    (and (.exists current-path)
         current-path)))

(defn get-jboss-home []
  (if-let [jboss-home (System/getenv "JBOSS_HOME")]
    (io/file jboss-home)
    (when-let [immutant-home (get-immutant-home)]
      (io/file immutant-home "jboss"))))

(defn err [& message]
  (binding [*out* *err*]
    (apply println message)))

(defn print-help []
  (println (lhelp/help-for nil "immutant")))

(defn unknown-subtask [subtask]
  (err "Unknown subtask" subtask)
  (print-help))

(defn resolve-project [project root-dir]
  (let [project-file (io/file root-dir "project.clj")]
    (if (or project (not (.exists project-file))) 
      [project root-dir]
      [(project/read (.getAbsolutePath project-file) [:default]) root-dir])))
