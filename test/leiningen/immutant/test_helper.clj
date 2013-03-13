(ns leiningen.immutant.test-helper
  (:require [leinjacker.lein-runner :as runner]
            [leinjacker.utils       :as utils]
            [leiningen.install      :as install]
            [clojure.java.io        :as io]
            [clojure.java.shell     :as sh]
            [clojure.set            :as set]))

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

(defn run-lein [& args]
  (let [[_ {:keys [return-result?]}] (split-with string? args)
        result (apply runner/run-lein 2 args)]
    (if return-result?
      result
      (do
        ;(print (:out result))
        (print (:err result))
        (:exit result)))))

(defn setup-test-env []
  (when (test-or-src-changed?)
    (println "\n==> Performing test env setup...")
    (println "====> Installing project...")
    (install/install (utils/read-lein-project))
    (println "====> Creating lein profiles.clj...")
    (spit (io/file lein-home "profiles.clj")
          (pr-str {:user {:plugins [['lein-immutant plugin-version]]}}))
    (println "==> Test env setup complete.\n")))

(def ^:dynamic *tmp-dir* nil)
(def ^:dynamic *tmp-jboss-home* nil)
(def ^:dynamic *tmp-deployments-dir* nil)

(defn with-tmp-dir* [f]
  (binding [*tmp-dir* (doto (io/file (System/getProperty "java.io.tmpdir")
                                      (str "lein-immutant-test-" (System/nanoTime)))
                         .mkdirs)]
     (try
       (f)
       (finally
         (delete-file-recursively *tmp-dir*)))))

(defmacro with-tmp-dir [& body]
  `(with-tmp-dir* (fn [] ~@body)))

(defmacro with-tmp-jboss-home [& body]
  `(with-tmp-dir*
     (fn []
       (binding [*tmp-jboss-home* (io/file *tmp-dir* "jboss-home")]
         (binding [*tmp-deployments-dir* (io/file *tmp-jboss-home* "standalone/deployments")]
           (.mkdirs *tmp-deployments-dir*)
           (let [bin-dir# (io/file *tmp-jboss-home* "bin")]
             (.mkdirs bin-dir#)
             ;; we have to use cp here to preserve perms
             (sh/sh "cp"
                    (.getAbsolutePath (io/file (io/resource "standalone.sh")))
                    (.getAbsolutePath (io/file bin-dir# "standalone.sh"))))
           ~@body)))))

(defn create-tmp-deploy
  ([name]
     (create-tmp-deploy name ".clj"))
  ([name suffix]
     (mapv
      #(spit (io/file *tmp-deployments-dir* (str name suffix %)) "")
      ["" ".deployed" ".dodeploy" ".failed"])))

(defn tmp-deploy-removed?
  ([name]
     (tmp-deploy-removed? name ".clj"))
  ([name suffix]
     (reduce
      (fn [acc f]
        (or acc (.exists (io/file *tmp-jboss-home* (str name suffix f)))))
      false
      ["" ".deployed" ".dodeploy" ".failed"])))

(defn copy-resource-to-tmp [r]
  (let [f (-> r io/resource io/file)]
    (sh/sh "cp" "-r"
           (.getAbsolutePath f)
           (.getAbsolutePath *tmp-dir*))
    (io/file *tmp-dir* (.getName f))))

(defn archive-set [f]
  (with-open [zip (java.util.zip.ZipFile. f)]
    (->> zip
         .entries
         enumeration-seq
         (map (memfn getName))
         set)))

(defn verify-archive [f expected]
  (let [a (archive-set f)]
    (or (= a expected)
        (println "FAIL archive diff: missing -"
                 (set/difference expected a)
                 "extra -"
                 (set/difference a expected)))))

(defn file-from-archive [archive name]
  (with-open [zip (java.util.zip.ZipFile. archive)]
    (let [f (io/file *tmp-dir* name)]
      (when-let [entry (some #(and (= (.getName %) name) %)
                             (enumeration-seq (.entries zip)))]
        (io/copy (.getInputStream zip entry) f)
        f))))


(def base-archive-contents #{"project.clj"
                             "resources/resource.txt"
                             "src/test_project/core.clj"})

(def base-project-archive-contents
  (conj base-archive-contents
        "target/classes/Foo.class"
        "target/native/foo.so"))

