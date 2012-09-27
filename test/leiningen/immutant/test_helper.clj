(ns leiningen.immutant.test-helper
  (:require [leinjacker.lein-runner :as runner]
            [leinjacker.utils       :as utils]
            [leiningen.install      :as install]
            [clojure.java.io        :as io]))

(def lein-home
  (io/file (io/resource "lein-home")))

(def base-lein-env
  {"LEIN_HOME" (.getAbsolutePath lein-home)})

(def plugin-version
  (:version (utils/read-lein-project)))

(defn- most-recent-change []
  (apply max (map (memfn lastModified)
                  (apply concat (map #(file-seq (io/file %))
                                     ["src" "project.clj"])))))

(defn- test-or-src-changed? []
  (let [timestamp-file (io/file ".last-change")
        latest-change (most-recent-change)
        previous-chage (if (.exists timestamp-file)
                         (read-string (slurp timestamp-file))
                         0)]
    (spit timestamp-file (pr-str latest-change))
    (> latest-change previous-chage)))

(defn delete-file-recursively [file]
  (let [file (io/file file)]
    (if (.isDirectory file)
      (doseq [child (.listFiles file)]
        (delete-file-recursively child))
      (io/delete-file file true))))

(defn run-lein [generation & args]
  (let [[_ {:keys [return-result?]}] (split-with string? args)
        result (apply runner/run-lein generation args)]
    (if return-result?
      result
      (do
        (print (:out result))
        (print (:err result))
        (:exit result)))))

(defn setup-test-env []
  (when (test-or-src-changed?)
    (println "\n==> Performing test env setup...")
    (println "====> Installing project...")
    (install/install (utils/read-lein-project))
    (println "====> Installing as lein 1 plugin...")
    (run-lein 1 "plugin" "install" "lein-immutant" plugin-version :env base-lein-env)
    (println "====> Creating lein 2 profiles.clj...")
    (spit (io/file lein-home "profiles.clj")
          (pr-str {:user {:plugins [['lein-immutant plugin-version]]}}))
    (println "==> Test env setup complete.\n")))

(def ^:dynamic *tmp-dir* nil)

(defmacro with-tmp-dir [& body]
  `(binding [*tmp-dir* (doto (io/file (System/getProperty "java.io.tmpdir")
                                      (str "lein-immutant-test-" (System/nanoTime)))
                         .mkdirs)]
     (try
       ~@body
       (finally
         (delete-file-recursively *tmp-dir*)))))

(def ^:dynamic *generation* nil)

(defmacro for-all-generations [& body]
  `(doseq [gen# [1 2]]
     (binding [*generation* gen#]
       ~@body)))
