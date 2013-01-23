(ns leiningen.immutant.install
  (:require [clojure.java.io            :as io]
            [clojure.java.shell         :as shell]
            [clojure.data.json          :as json]
            [overlay.core               :as overlayment]
            [leiningen.immutant.common  :as common]
            [immutant.deploy-tools.util :as util]))

(alter-var-root #'overlayment/*output-dir*
                (constantly (io/file (System/getProperty "java.io.tmpdir")
                                     (str "lein-immutant-" (java.util.UUID/randomUUID)))))

(defn releases-dir []
  (doto (io/file (common/immutant-storage-dir) "releases")
    .mkdirs))

(defn link-current [target]
  (.delete common/current-path)
  (let [current-path (.getAbsolutePath common/current-path)
        target-path (.getAbsolutePath target)]
    (if common/windows?
      (do
        (println "Storing path to" target-path "in" current-path)
        (spit current-path target-path))
      (do
        (println  "Linking" current-path "to" target-path)
        (shell/sh "ln" "-s" target-path current-path)))))

(defn version-exists [url dest-dir version]
  (if (overlayment/released-version? version)
    (let [dir (io/file dest-dir (format "immutant-%s" version))]
      (and (.exists dir) [dir version]))
    (try
      (let [incr-version (with-open [r (io/reader (overlayment/metadata-url url))]
                           (:build_number (json/read-json (slurp r))))
            dir (io/file dest-dir  (format "immutant-1.x.incremental.%s" incr-version))]
        (and (.exists dir) [dir (str "1.x.incremental." incr-version)]))
      (catch Exception e
        nil))))

(defn install
  "Downloads and installs an Immutant version"
  ([]
     (install nil nil))
  ([version]
     (install version nil))
  ([version dest-dir]
     (let [url (overlayment/url :immutant :bin
                                (if (and version
                                         (not (overlayment/released-version? version)))
                                  (.toUpperCase version)
                                  version))
           install-dir (or dest-dir (releases-dir))]
       (if-let [[existing-dir true-version] (version-exists url install-dir version)]
         (do
          (println (str "Version " true-version " already installed to " install-dir ", not downloading."))
          (link-current existing-dir))
         (if-let [extracted-dir (binding [overlayment/*extract-dir* install-dir
                                          overlayment/*verify-sha1-sum* true]
                                  (overlayment/download-and-extract url))]
           (link-current extracted-dir)
           (println "Please try the install again."))))))

(defn overlay
  "Overlays a feature set onto the current Immutant

This turns the Immutant into a hybrid application server, which acts as
an Immutant and whatever feature sets are overlayed onto it. Currently,
the only supported feature set is 'torquebox'."
  ([]
     (println "No feature set provided, assuming 'torquebox'")
     (overlay "torquebox" nil))
  ([feature-set]
     (overlay feature-set nil))
  ([feature-set version]
     (when-not (and (common/get-jboss-home) (.exists (common/get-jboss-home)))
       (println "No Immutant installed, installing the latest incremental")
       (install))
     (binding [overlayment/*verify-sha1-sum* true]
       (overlayment/overlay (common/get-immutant-home) feature-set))))

(defn version
  "Prints version info for the current Immutant"
  []
  (if-let [props (util/current-immutant-build-properties
                  (common/get-jboss-home))]
    (println (format "Immutant %s (revision: %s, built on AS7 %s)"
                     (.getProperty props "Immutant.version")
                     (.getProperty props "Immutant.build.revision")
                     (.getProperty props "JBossAS.version")))
    (println "Unable to determine the Immutant version at"
             (common/get-jboss-home))))
