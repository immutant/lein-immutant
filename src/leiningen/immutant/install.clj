(ns leiningen.immutant.install
  (:use leiningen.immutant.common)
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.data.json :as json]
            [leiningen.util.file :as lfile]
            [overlay.core :as overlayment]))

;; short-circuit the future pool so we don't have to wait 60s for it to exit
(.setKeepAliveTime clojure.lang.Agent/soloExecutor 100 java.util.concurrent.TimeUnit/MILLISECONDS)

(alter-var-root #'overlayment/*output-dir*
                (constantly (lfile/unique-lein-tmp-dir)))

(defn releases-dir []
  (doto (io/file (immutant-storage-dir) "releases")
    .mkdirs))

(defn link-current [target]
  (.delete current-path)
  (shell/sh "ln" "-s" (.getAbsolutePath target) (.getAbsolutePath current-path))
  (println  "Linking" (.getAbsolutePath current-path) "to" (.getAbsolutePath target)))

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
         (doall
          (println "Version" true-version "already installed to" install-dir ", not downloading." )
          (link-current existing-dir))
         (link-current (binding [overlayment/*extract-dir* (releases-dir)]
                         (overlayment/download-and-extract incr-url)))))))

(defn overlay
  "Overlays layee onto ~/.lein/immutant/current or $IMMUTANT_HOME"
  ([]
     (println "No layee provided, assuming 'torquebox'")
     (overlay "torquebox" nil))
  ([layee]
     (overlay layee nil))
  ([layee version]
     (when-not (and (get-jboss-home) (.exists (get-jboss-home)))
       (println "No Immutant installed, installing the latest")
       (install))
     (overlayment/overlay (get-immutant-home) layee)))
