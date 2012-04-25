(ns leiningen.immutant.common
  (:require [clojure.java.io         :as io]
            [leiningen.immutant.shim :as shim]
            [leiningen.help          :as lhelp]))

(defn get-application-root [args]
  (io/file (or (first (filter #(not (.startsWith % "--")) args))
               (System/getProperty "user.dir"))))

(defn immutant-storage-dir []
  (.getAbsolutePath
   (doto (io/file (shim/leiningen-home-fn) "immutant")
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
  (println (if shim/lein2?
             (lhelp/help-for nil "immutant")
             (lhelp/help-for "immutant"))))

(defn unknown-subtask [subtask]
  (err "Unknown subtask" subtask)
  (print-help))

(defn resolve-project [project root-dir]
  (let [project-file (io/file root-dir "project.clj")]
    (if (or project (not (.exists project-file))) 
      [project root-dir]
      [(shim/read-project (.getAbsolutePath project-file) [:default]) root-dir])))
