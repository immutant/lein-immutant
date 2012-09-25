(ns leiningen.immutant.common
  (:require [clojure.java.io         :as io]
            [leiningen.help          :as lhelp]
            [leinjacker.utils        :as lj]))

(def ^{:doc "True if running under lein2"}
  lein2? (= 2 (lj/lein-generation)))

(def windows?
  (re-find #"(?i)^windows" (System/getProperty "os.name")))

(defn get-application-root [args]
  (io/file (or (first args)
               (System/getProperty "user.dir"))))

(defn immutant-storage-dir []
  (.getAbsolutePath
   (doto (io/file (lj/lein-home) "immutant")
     .mkdirs)))

(def current-path
  (io/file (immutant-storage-dir) "current"))

(defn get-immutant-home []
  (if-let [immutant-home (System/getenv "IMMUTANT_HOME")]
    (io/file immutant-home)
    (when (.exists current-path)
      (if windows?
        (io/file (slurp current-path))
        current-path))))

(defn get-jboss-home []
  (if-let [jboss-home (System/getenv "JBOSS_HOME")]
    (io/file jboss-home)
    (when-let [immutant-home (get-immutant-home)]
      (io/file immutant-home "jboss"))))

(defn err [& message]
  (binding [*out* *err*]
    (apply println message)))

(defn print-help []
  (println (if lein2?
             (lhelp/help-for nil "immutant")
             (lhelp/help-for "immutant"))))

(defn unknown-subtask [subtask]
  (err "Unknown subtask" subtask)
  (print-help))

(defn resolve-project [project root-dir]
  (let [project-file (io/file root-dir "project.clj")]
    (if (or project (not (.exists project-file))) 
      [project root-dir]
      [(lj/read-lein-project (.getAbsolutePath project-file) [:default]) root-dir])))

(defn as-config-option [opt]
  (with-meta opt {:config true}))

(defn group-options [opts available-opts]
  (let [{:keys [config options]}
        (group-by
         (fn [[key _]]
           (if (:config (some #(if (some #{(str "--" (name key))} %)
                                 (meta %))
                              available-opts))
             :config
             :options))
         opts)]
    [(into {} options)
     (into {} config)]))
