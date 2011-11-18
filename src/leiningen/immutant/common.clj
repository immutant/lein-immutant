(ns leiningen.immutant.common
  (:require [clojure.java.io :as io]
            [leiningen.help :as lhelp]
            [leiningen.core :as lcore]))

(defn get-immutant-home []
  (if-let [immutant-home (System/getenv "IMMUTANT_HOME")]
    (io/file immutant-home)))

(defn get-jboss-home []
  (if-let [jboss-home (System/getenv "JBOSS_HOME")]
    (io/file jboss-home)
    (when-let [immutant-home (get-immutant-home)]
      (io/file immutant-home "jboss"))))

(def *jboss-home*)

(defmacro with-jboss-home [& forms]
  `(if (get-jboss-home)
    (if (.isDirectory (get-jboss-home))
      (binding [*jboss-home* (get-jboss-home)]
        (do ~@forms))
      (abort (.getAbsolutePath (get-jboss-home)) "does not exist."))
    (abort "Could not locate jboss home. Set $JBOSS_HOME or $IMMUTANT_HOME.")))

(defn err [& message]
  (binding [*out* *err*]
    (apply println message)))

(defn abort [& message]
  (apply lcore/abort message))

(defn app-name [project]
  (:name project))

(defn descriptor-name [project]
  (str (app-name project) ".clj"))

(defn deployment-dir []
  (io/file  *jboss-home* "standalone" "deployments"))

(defn descriptor-file [project]
  (io/file (deployment-dir) (descriptor-name project)))

(defn marker [suffix project]
  (io/file (str (.getAbsolutePath (descriptor-file project)) suffix)))

(def dodeploy-marker
  (partial marker ".dodeploy"))

(def deployed-marker
  (partial marker ".deployed"))

(defn print-help []
  (println (lhelp/help-for "immutant")))

(defn unknown-subtask [subtask]
  (err "Unknown subtask" subtask)
  (print-help))

