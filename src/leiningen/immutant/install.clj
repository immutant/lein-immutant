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

(def install-options
  [["-f" "--full" :flag true]])

(defn releases-dir [dist-type]
  (doto (io/file (common/immutant-storage-dir) "releases" dist-type)
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

(defn latest-release []
  (try
    (with-open [r (io/reader "http://immutant.org/latest-release.txt")]
      (-> r slurp clojure.string/trim))
    (catch Exception _
      (println "Unable to determine latest versioned release, installing latest incremental")
      "LATEST")))

(defn version-exists [url dest-dir version]
  (if (overlayment/released-version? version)
    (let [dir (io/file dest-dir (format "immutant-%s" version))]
      (and (.exists dir) [dir version]))
    (try
      (let [incr-version (with-open [r (io/reader (overlayment/metadata-url url))]
                           (:build_number (json/read-str (slurp r) :key-fn keyword)))
            dir (io/file dest-dir  (format "immutant-1.x.incremental.%s" incr-version))]
        (and (.exists dir) [dir (str "1.x.incremental." incr-version)]))
      (catch Exception _
        nil))))

(defn normalize-version [version]
  (if (nil? version)
    (latest-release)
    (.toUpperCase
     (if (.startsWith version ":")
       (.substring version 1)
       version))))

(defn suss-dist-type [full? version]
  (let [release? (overlayment/released-version? version)]
    (if (or
         (nil? version)
         (= "LATEST" version)
         (and release?
              (or (= "0.9.0" version)
                  (= "0.10.0" version)
                  (= "1" (re-find #"^\d" version))))  
         (and (not release?)
              (< 750 (Integer/parseInt version))))
      (if full? "full" "slim")
      "bin")))

(defn install
  "Downloads and installs an Immutant version

By default, it will download the latest versioned release and put it
in ~/.lein/immutant/releases/. You can override the version (which
must be an incremental build number from http://immutant.org/builds/,
a released version, or :latest for the most recent incremental
build) and the install directory. Wherever it gets installed, the most
recently installed version will be linked to
~/.lein/immutant/current. If this link is present (and points to a
valid Immutant install), you won't need to set $IMMUTANT_HOME to
notify plugin tasks of the location of Immutant.

This task will install the 'slim' distribution unless given the
'--full' option.

 On Windows, ~/.lein/immutant/current is actually a text file
containing the path to the current Immutant instead of a link."
  ([options]
     (install options nil nil))
  ([options version]
     (install options version nil))
  ([options version dest-dir]
     (let [version (normalize-version version)
           dist-type (suss-dist-type (:full options) version)
           artifact (overlayment/artifact "immutant" version dist-type)
           url (overlayment/url artifact)
           install-dir (or dest-dir (releases-dir dist-type))]
       (if-let [[existing-dir true-version] (version-exists url install-dir version)]
         (do
           (println (format "Version %s (%s) already installed to %s, not downloading."
                             true-version dist-type install-dir))
          (link-current existing-dir))
         (if-let [extracted-dir (binding [overlayment/*extract-dir* install-dir
                                          overlayment/*verify-sha1-sum* true]
                                  (overlayment/download-and-extract artifact))]
           (link-current extracted-dir)
           (println "Please try the install again."))))))

(defn overlay
  "Overlays a feature set onto the current Immutant

This turns the Immutant into a hybrid application server, which acts as
an Immutant and whatever feature sets are overlayed onto it. Currently,
the only supported feature set is 'torquebox'.

By default, the plugin will locate the current Immutant by looking at
~/.lein/immutant/current. This can be overriden by setting the
$IMMUTANT_HOME environment variable."
  ([]
     (println "No feature set provided, assuming 'torquebox'")
     (overlay "torquebox" nil))
  ([feature-set]
     (overlay feature-set nil))
  ([feature-set version]
     (when-not (and (common/get-jboss-home) (.exists (common/get-jboss-home)))
       (println "No Immutant installed, installing the latest versioned release")
       (install nil))
     (binding [overlayment/*verify-sha1-sum* true]
       (let [version-string (when-not (nil? version) (str "-" version))]
         (overlayment/overlay (common/get-immutant-home) (str feature-set version-string))))) )

(defn version
  "Prints version info for the current Immutant

By default, the plugin will locate the current Immutant by looking at
~/.lein/immutant/current. This can be overriden by setting the
$IMMUTANT_HOME environment variable."
  []
  (if-let [props (util/current-immutant-build-properties
                  (common/get-jboss-home))]
    (println (format "Immutant %s (revision: %s, built on AS7 %s)"
                     (.getProperty props "Immutant.version")
                     (.getProperty props "Immutant.build.revision")
                     (.getProperty props "JBossAS.version")))
    (println "Unable to determine the Immutant version at"
             (common/get-jboss-home))))
