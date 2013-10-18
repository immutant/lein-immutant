(ns leiningen.immutant.install
  (:require [clojure.java.io            :as io]
            [clojure.java.shell         :as shell]
            [clojure.data.json          :as json]
            [clojure.string             :as str]
            [overlay.core               :as overlayment]
            [overlay.filesystem         :as fs]
            [leiningen.immutant.common  :as common]
            [immutant.deploy-tools.util :as util])
  (:import org.apache.commons.io.FileUtils))

(alter-var-root #'overlayment/*output-dir*
                (constantly (io/file (System/getProperty "java.io.tmpdir")
                                     (str "lein-immutant-" (java.util.UUID/randomUUID)))))

(def install-options
  [["-f" "--full" :flag true]])

(defn releases-dir []
  (doto (io/file (common/immutant-storage-dir) "releases")
    .mkdirs))

(declare list-installs)

(defn link-current [target]
  (.delete (common/current-path))
  (let [current-path (.getAbsolutePath (common/current-path))
        target-path (.getAbsolutePath target)]
    (if common/windows?
      (do
        (println "Storing path to" target-path "in" current-path)
        (spit current-path target-path))
      (do
        (println  "Linking" current-path "to" target-path)
        (shell/sh "ln" "-s" target-path current-path))))
  (list-installs))

(defn latest-release []
  (try
    (with-open [r (io/reader "http://immutant.org/latest-release.txt")]
      (-> r slurp clojure.string/trim))
    (catch Exception _
      (println "Unable to determine latest versioned release, installing latest incremental")
      "LATEST")))

(defn version-dir-exists [dest-dir version dist-type]
    (let [legacy-dir (io/file dest-dir dist-type (format "immutant-%s" version))
          dir        (io/file dest-dir (format "immutant-%s-%s" version dist-type))]
      (cond
       (.exists legacy-dir) [legacy-dir version]
       (.exists dir)        [dir version])))

(defn version-exists [dest-dir version dist-type]
  (version-dir-exists dest-dir (if (overlayment/released-version? version)
                                 version
                                 (str "1.x.incremental." version))
                      dist-type))

(defn check-for-and-use-existing-version [install-dir version dist-type]
  (when-let [[existing-dir true-version] (version-exists install-dir version dist-type)]
    (println (format "Version %s (%s) already installed to %s, not downloading."
                     true-version dist-type install-dir))
    (link-current existing-dir)
    true))

(defn normalize-version [version]
  (if (nil? version)
    (latest-release)
    (if (.startsWith version ":")
      (.toUpperCase (.substring version 1))
      version)))

(defn suss-dist-type
  ([full? version]
     (suss-dist-type full? version "bin"))
  ([full? version default]
      (let [release? (overlayment/released-version? version)]
        (if (or
             (nil? version)
             (= "LATEST" version)
             (and release?
                  (or (= "0.9.0" version)
                      (= "0.10.0" version)
                      (= "1" (re-find #"^\d" version))))  
             (and (not release?)
                  (< 750 (-> version (str/split #"_") first Integer/parseInt))))
          (if full? "full" "slim")
          default))))

(defn adjust-legacy-dist [dir dist-type]
  (if (.endsWith (.getName dir) dist-type)
    dir
    (do
      ;; we're installing a version from before IMMUTANT-276
      (let [new-dir (io/file (.getParentFile dir)
                     (format "%s-%s" (.getName dir) dist-type))]
        (println (format "Renaming %s to %s"
                         (.getName dir)
                         (.getName new-dir)))
        (if (.renameTo dir new-dir)
          new-dir
          (do
            (println
             "WARNING: Rename failed - the plugin won't be able to detect this version was installed in the future")
            dir))))))

(defn extract-version-and-type [name]
  (let [[_ version _ type] (re-find #"immutant-(.*?)($|-([^-]*)$)" name)]
    {:type type :version version}))

(defn generate-overlay-version [current-name overlay-artifact]
  (let [overlay-fragment (format "%s-%s"
                                 (name (overlayment/feature overlay-artifact))
                                 (overlayment/version overlay-artifact))
        current-version (:version (extract-version-and-type current-name))]
    (if (re-find (re-pattern (format "_%s[_-]" overlay-fragment)) current-name)
      current-version
      (format "%s_%s" current-version overlay-fragment))))

(defn generate-overlay-dir-name [current-name overlay-artifact]
  (format "immutant-%s-%s"
          (generate-overlay-version current-name overlay-artifact)
          (:type (extract-version-and-type current-name))))

(defn installed-versions
  ([]
     (into {}
           (concat
            (installed-versions (releases-dir))
            (installed-versions (io/file (releases-dir) "bin") true)
            (installed-versions (io/file (releases-dir) "full") true)
            (installed-versions (io/file (releases-dir) "slim") false))))
  ([parent-dir full?]
     (map (fn [[file {:keys [version] :as data}]]
            [file (assoc data :type (suss-dist-type full? version "full"))])
          (installed-versions parent-dir)))
  ([parent-dir]
     (->> (.listFiles parent-dir)
          (filter
           #(re-find #"immutant-.*$" (.getName %)))
          (map (fn [f]
                 [f (extract-version-and-type (.getName f))])))))

(defn list-installs
  "Lists currently installed versions of Immutant"
  []
  (println
   (format "The following versions of Immutant are installed to %s\n(* is currently active via %s):\n"
           (common/immutant-storage-dir)
           (if (System/getenv "IMMUTANT_HOME")
             "$IMMUTANT_HOME"
             (.getAbsolutePath (common/current-path)))))
  (->>
   (for [[file {:keys [type version]}] (installed-versions)]
     [(if (and (common/get-immutant-home)
               (= (.getCanonicalPath file)
                  (.getCanonicalPath (common/get-immutant-home))))
         "*"
         " ")
      version
      (or type "?")])
   (sort-by second)
   (map #(apply format " %s %-50s (type: %s)" %))
   (clojure.string/join "\n")
   println))

(defn install
  "Downloads and installs an Immutant version

By default, it will download the latest versioned release and put it
in ~/.immutant/releases/. You can override the version (which must be
an incremental build number from http://immutant.org/builds/, a
released version, or LATEST for the most recent incremental build)
and the install directory. Wherever it gets installed, the most
recently installed version will be linked to ~/.immutant/current. If
this link is present (and points to a valid Immutant install), you
won't need to set $IMMUTANT_HOME to notify plugin tasks of the
location of Immutant.

This task will install the 'slim' distribution unless given the
'--full' option.

By default, the install places its files (installed Immutants, the
current link) under ~/.immutant/. You can override this by setting
$LEIN_IMMUTANT_BASE_DIR or by adding :lein-immutant {:base-dir
\"/path\"} to your user profile in .lein/profiles.clj or to your
project.clj. Setting the base directory in project.clj will override
the setting in .lein/profiles.clj. Using the environment variable will
override both.

On Windows, ~/.immutant/current is actually a text file
containing the path to the current Immutant instead of a link."
  ([options]
     (install options nil nil))
  ([options version]
     (install options version nil))
  ([options version dest-dir]
     (let [version (normalize-version version)
           dist-type (suss-dist-type (:full options) version)
           artifact (overlayment/artifact "immutant" version dist-type)
           install-dir (or dest-dir (releases-dir))]
       (when-not (check-for-and-use-existing-version install-dir
                                                     (overlayment/version artifact)
                                                     dist-type)
         (if-let [extracted-dir (binding [overlayment/*extract-dir* install-dir
                                          overlayment/*verify-sha1-sum* true]
                                  (-> artifact
                                      overlayment/download-and-extract
                                      (adjust-legacy-dist dist-type)))]
           (link-current extracted-dir)
           (println "Please try the install again."))))))

(defn overlay
  "Overlays a feature set onto the current Immutant

This turns the Immutant into a hybrid application server, which acts as
an Immutant and whatever feature sets are overlayed onto it.

By default, the plugin will locate the current Immutant by looking at
~/.immutant/current. This can be overriden by setting the
$IMMUTANT_HOME environment variable."
  ([]
     (common/abort (str "Feature spec required: feature[-version]."
                        "\nRun `lein help immutant overlay` for more information.")))
  ([feature version]
     (overlay (str feature (when-not (nil? version) (str "-" version)))))
  ([feature-spec]
     (when-not (and (common/get-jboss-home) (.exists (common/get-jboss-home)))
       (println "No Immutant installed, installing the latest versioned release")
       (install nil))
     (let [artifact (overlayment/artifact feature-spec)
           current-home (-> (common/get-immutant-home)
                            .getCanonicalFile)]
       (when-not (check-for-and-use-existing-version
                  (releases-dir)
                  (generate-overlay-version (.getName current-home)
                                            artifact)
                  (:type (extract-version-and-type (.getName current-home))))
         (let [new-dir (io/file (releases-dir) 
                                (generate-overlay-dir-name (.getName current-home)
                                                           artifact))]
           (FileUtils/copyDirectory current-home new-dir true)
           (fs/+x-sh-scripts new-dir)
           (binding [overlayment/*verify-sha1-sum* true]
             (overlayment/overlay (.getAbsolutePath new-dir)
                                  feature-spec
                                  "--overwrite"))
           (link-current new-dir))))))

(defn version
  "Prints version info for the current Immutant

By default, the plugin will locate the current Immutant by looking at
~/.immutant/current. This can be overriden by setting the
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
