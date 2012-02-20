(ns leiningen.immutant.install
  (:require [clojure.java.io           :as io]
            [clojure.java.shell        :as shell]
            [clojure.data.json         :as json]
            [leiningen.util.file       :as lfile]
            [overlay.core              :as overlayment]
            [leiningen.immutant.common :as common]))

(alter-var-root #'overlayment/*output-dir*
                (constantly (lfile/unique-lein-tmp-dir)))

(defn releases-dir []
  (doto (io/file (common/immutant-storage-dir) "releases")
    .mkdirs))

(defn link-current [target]
  (.delete common/current-path)
  (shell/sh "ln" "-s" (.getAbsolutePath target) (.getAbsolutePath common/current-path))
  (println  "Linking" (.getAbsolutePath common/current-path) "to" (.getAbsolutePath target)))

(defn version-exists [url dest-dir]
  (try
    (let [version (with-open [r (io/reader (overlayment/metadata-url url))]
                    (:build_number (json/read-json (slurp r))))
          dir (io/file dest-dir (format "immutant-1.x.incremental.%s" version))]
      (if (.exists dir) [dir version]))
    (catch Exception e
            nil)))

(defn install
  "Downloads and installs Immutant"
  ([]
     (install nil nil))
  ([version]
     (install version nil))
  ([version dest-dir]
     (let [incr-url (overlayment/incremental :immutant :bin (and version (.toUpperCase version)))
           install-dir (or dest-dir (releases-dir))]
       (if-let [[existing-dir true-version] (version-exists incr-url install-dir)]
         (do
          (println (str "Version " true-version " already installed to " install-dir ", not downloading."))
          (link-current existing-dir))
         (link-current (binding [overlayment/*extract-dir* (releases-dir)]
                         (overlayment/download-and-extract incr-url)))))))

(defn overlay
  "Overlays features onto ~/.lein/immutant/current or $IMMUTANT_HOME"
  ([]
     (println "No feature set provided, assuming 'torquebox'")
     (overlay "torquebox" nil))
  ([feature-set]
     (overlay feature-set nil))
  ([feature-set version]
     (when-not (and (common/get-jboss-home) (.exists (common/get-jboss-home)))
       (println "No Immutant installed, installing the latest")
       (install))
     (overlayment/overlay (common/get-immutant-home) feature-set)))
