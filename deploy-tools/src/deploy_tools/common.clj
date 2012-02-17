(ns deploy-tools.common
  (:require [clojure.java.io :as io]))

(defn get-application-root [args]
  (io/file (or (first (filter #(not (.startsWith % "--")) args))
               (System/getProperty "user.dir"))))

;; (defn immutant-storage-dir []
;;   (.getAbsolutePath
;;    (doto (io/file (lpaths/leiningen-home) "immutant")
;;      .mkdirs)))

;; (def current-path
;;   (io/file (immutant-storage-dir) "current"))

;; (defn get-immutant-home []
;;   (if-let [immutant-home (System/getenv "IMMUTANT_HOME")]
;;     (io/file immutant-home)
;;     (and (.exists current-path)
;;          current-path)))

;; (defn get-jboss-home []
;;   (if-let [jboss-home (System/getenv "JBOSS_HOME")]
;;     (io/file jboss-home)
;;     (when-let [immutant-home (get-immutant-home)]
;;       (io/file immutant-home "jboss"))))

(def ^{:dynamic true} *jboss-home*)

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

;; borrowed from leiningen.core
(defn exit
  "Call System/exit. Defined as a function so that rebinding is possible."
  ([code]
     (shutdown-agents)
     (System/exit code))
  ([] (exit 0)))

;; borrowed from leiningen.core
(defn abort
  "Print msg to standard err and exit with a value of 1."
  [& msg]
  (binding [*out* *err*]
    (apply println msg)
    (exit 1)))

(defn app-name [project]
  (:name project))

(defn descriptor-name [project]
  (str (app-name project) ".clj"))

(defn archive-name [project root-dir]
  (str (:name project (.getName root-dir)) ".ima"))

(defn deployment-dir []
  (io/file  *jboss-home* "standalone" "deployments"))

(defn descriptor-file [project]
  (io/file (deployment-dir) (descriptor-name project)))

(defn deployed-archive-file [project]
  (io/file (deployment-dir) (archive-name project)))

(defn marker [suffix project]
  (io/file (str (.getAbsolutePath (descriptor-file project)) suffix)))

(def dodeploy-marker
  (partial marker ".dodeploy"))

(def deployed-marker
  (partial marker ".deployed"))

(defn archive-marker [suffix project]
  (io/file (str (.getAbsolutePath (deployed-archive-file project)) suffix)))

(def archive-dodeploy-marker
  (partial archive-marker ".dodeploy"))

(def archive-deployed-marker
  (partial archive-marker ".deployed"))

;; (defn print-help []
;;   (println (lhelp/help-for "immutant")))

;; (defn unknown-subtask [subtask]
;;   (err "Unknown subtask" subtask)
;;   (print-help))

