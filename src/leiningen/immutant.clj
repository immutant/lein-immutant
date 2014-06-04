(ns leiningen.immutant
  (:require [leiningen.core.classpath :as cp]
            [leiningen.core.main :refer [abort]]
            [leiningen.uberjar :as uberjar]
            [leiningen.jar :as jar]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import java.io.FileOutputStream
           java.util.Properties
           java.util.zip.ZipOutputStream))

(defn- main-fn [project]
  (if-let [main-ns (:main project)]
    (symbol (str main-ns) "-main")))

(defn- ring-fn [project]
  ;;TODO: implement
  )

(defn- immutant-init-fn [project]
  (-> project :immutant :init))

(defn- build-init [project]
  ;;TODO: make repl optional
  (if-let [init-fn (or (immutant-init-fn project)
                     (main-fn project)
                     (ring-fn project))]
    (pr-str `(do
               (require 'immutant.wildfly)
               (immutant.wildfly/init-deployment (quote ~init-fn) {:nrepl {:host "localhost" :port 0}})))))

(defn extract-keys
  ([m]
     (extract-keys [] m))
  ([acc m]
     (if (map? m)
       (concat (keys m) (mapcat (fn [[_ v]] (extract-keys acc v)) m)))))

(defn- locate-version [project ns]
  (if-let [version (->> project
                     (cp/dependency-hierarchy :dependencies)
                     extract-keys
                     (some (fn [[dep version]]
                             (if (= ns (namespace dep))
                               version))))]
      version
      (abort (format "No %s dependency found in the project's dependency tree." ns))))

(defn- classpath [project]
  ;; TODO: don't displace a version that may be there
  (-> project
    (update-in [:dependencies]
      conj ['org.immutant/wildfly (locate-version project "org.immutant")])
    cp/get-classpath))

(defn- build-descriptor [project]
  {:root (:root project)
   :language "clojure"
   :classpath (str/join ":" (classpath project))
   ;; TODO: don't barf if :init is nil
   :init (build-init project)})

(defn- map->properties [m]
  (reduce (fn [p [k v]]
            (doto p
              (.setProperty (name k) v)))
    (Properties.)
    m))

(defn- find-base-jars [project]
  (let [version (locate-version project "org.projectodd.wunderboss")]
    (filter #(re-find #"^wunderboss-.*\.jar$" (.getName %))
      (cp/resolve-dependencies :dependencies
        (update-in project [:dependencies]
          conj ['org.projectodd.wunderboss/wunderboss-wildfly version])))))

;; We need to keep the manifest that comes from the wunderboss-wildfly.jar
;; a better solution is to probably read it as Properties and merge there
(defn- merge-manifests [new prev]
  (if (re-find #"Dependencies" new)
    new
    prev))

(defn- build-jar [project to uber?]
  ;; TODO: implement uber?
  (with-open [out (-> to FileOutputStream. ZipOutputStream.)]
    (uberjar/write-components (update-in project [:uberjar-merge-with]
                                assoc
                                #"(?i)^META-INF/MANIFEST.MF$"
                                `[slurp #'leiningen.immutant/merge-manifests spit])
      (->> project find-base-jars (sort-by #(.getName %))) out)))

(defn deploy [project wf-home]
  "Deploys the current project to the given WildFly home."
  [project wf-home]
  (when (nil? wf-home)
    (abort "Missing argument: deploy requires WildFly/EAP install path"))
  (when (nil? (build-init project))
    (abort "Project requires an entry point, e.g. -main, ring handler, immutant init"))
 (let [dep-dir (io/file wf-home "standalone/deployments")
        props-file (io/file dep-dir (str (:name project) ".properties"))
        jar-file (io/file dep-dir (str (:name project) ".jar"))]
    (println "Creating" (.getAbsolutePath jar-file))
    (build-jar project jar-file false)
    (println "Creating" (.getAbsolutePath props-file))
    (with-open [writer (io/writer props-file)]
      (-> project
        build-descriptor
        map->properties
        (.store writer "")))))

(defn immutant
  "Manage the deployment lifecycle of an Immutant application."
  {:subtasks [#'deploy]}
  ([project subtask & args]
     (case subtask
       "deploy" (deploy project (first args)))
     (shutdown-agents)))
