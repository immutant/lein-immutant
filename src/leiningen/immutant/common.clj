(ns leiningen.immutant.common
  (:require [clojure.java.io :as io]
            [leiningen.help :as lhelp]
            [leiningen.util.paths :as lpaths]))

(defn get-application-root [args]
  (io/file (or (first (filter #(not (.startsWith % "--")) args))
               (System/getProperty "user.dir"))))

(defn immutant-storage-dir []
  (.getAbsolutePath
   (doto (io/file (lpaths/leiningen-home) "immutant")
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
  (println (lhelp/help-for "immutant")))

(defn unknown-subtask [subtask]
  (err "Unknown subtask" subtask)
  (print-help))

