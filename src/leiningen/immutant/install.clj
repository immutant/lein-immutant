(ns leiningen.immutant.install
  (:use leiningen.immutant.common
        [overlay.core :only [download-and-extract incremental metadata-url]])
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.data.json :as json]))

;; short-circuit the future pool so we don't have to wait 60s for it to exit
(.setKeepAliveTime clojure.lang.Agent/soloExecutor 100 java.util.concurrent.TimeUnit/MILLISECONDS)

(defn releases-dir []
  (doto (io/file (immutant-storage-dir) "releases")
    .mkdirs))

(defn artifacts-dir []
  (doto (io/file (immutant-storage-dir) "artifacts")
    .mkdirs))

(defn link-current [target]
  (.delete current-path)
  (shell/sh "ln" "-s" (.getAbsolutePath target) (.getAbsolutePath current-path))
  (println  "Linking" (.getAbsolutePath current-path) "to" (.getAbsolutePath target)))

(defn version-exists [url dest-dir]
  (let [version (with-open [r (io/reader (metadata-url url))]
                  (:build_number (json/read-json (slurp r))))
        dir (io/file dest-dir (format "immutant-1.x.incremental.%s" version))]
    (if (.exists dir) [dir version])))

(defn install
  "download and install Immutant"
  ([]
     (install nil nil))
  ([version]
     (install version nil))
  ([version dest-dir]
     (let [incr-url (incremental :immutant :bin (and version (.toUpperCase version)))
           install-dir (or dest-dir (releases-dir))]
       (if-let [[existing-dir true-version] (version-exists incr-url install-dir)]
         (doall
          (println "Version" true-version "already installed to" install-dir ", not downloading." )
          (link-current existing-dir))
         (link-current (download-and-extract incr-url
                                             (artifacts-dir)
                                             install-dir))))))
