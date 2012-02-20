(ns deploy-tools.common
  (:require [clojure.java.io :as io]))

(defn get-application-root [args]
  (io/file (or (first (filter #(not (.startsWith % "--")) args))
               (System/getProperty "user.dir"))))

(def ^{:dynamic true} *jboss-home*)

(defmacro with-jboss-home [jboss-home & forms]
  `(if ~jboss-home
    (if (.isDirectory ~jboss-home)
      (binding [*jboss-home* ~jboss-home]
        (do ~@forms))
      (abort (.getAbsolutePath ~jboss-home) "does not exist."))
    (abort "Could not locate jboss home. Set $JBOSS_HOME or $IMMUTANT_HOME.")))

(defn err [& message]
  (binding [*out* *err*]
    (apply println message)))

;; borrowed from leiningen.core
(defn abort
  "Print msg to standard err and exit with a value of 1."
  [& msg]
  (binding [*out* *err*]
    (apply println msg)
    (shutdown-agents)
    (System/exit 1)))

(defn app-name [project root-dir]
  (:name project (.getName root-dir)))

(defn descriptor-name [project root-dir]
  (str (app-name project root-dir) ".clj"))

(defn archive-name [project root-dir]
  (str (app-name project root-dir) ".ima"))

(defn deployment-dir []
  (io/file  *jboss-home* "standalone" "deployments"))

(defn deployment-file [archive-name]
  (io/file (deployment-dir) archive-name))

(defn marker [suffix deployment-file]
  (io/file (str (.getAbsolutePath deployment-file) suffix)))

(def dodeploy-marker
  (partial marker ".dodeploy"))

(def deployed-marker
  (partial marker ".deployed"))


