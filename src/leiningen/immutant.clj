(ns leiningen.immutant
  (:require [leiningen.core.classpath :as cp]
            [leiningen.core.main :refer [abort]]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import java.util.Properties))

(defn- build-init [project]
  ;;TODO: support ring/main options
  ;;TODO: make repl optional
  (if-let [init-fn (-> project :immutant :init)]
    (pr-str `(do
               (require 'immutant.wildfly)
               (immutant.wildfly/init (quote ~init-fn) {:nrepl {:host "localhost" :port 0}})))))

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

(defn- copy-deploy-jar [project to]
  (-> (cp/resolve-dependencies :dependencies
        {:dependencies [['org.projectodd.wunderboss/wunderboss-wildfly
                         (locate-version project "org.projectodd.wunderboss")]]})
    (as-> files (filter #(re-find #"wunderboss-wildfly.*\.jar" (.getName %)) files))
    first
    (io/copy to)))

(defn deploy [project wf-home]
    "Deploys the current project to the given WildFly home."
  [project wf-home]
  (let [dep-dir (io/file wf-home "standalone/deployments")
        props-file (io/file dep-dir (str (:name project) ".properties"))
        jar-file (io/file dep-dir (str (:name project) ".jar"))]
    (println "Creating" (.getAbsolutePath jar-file))
    (copy-deploy-jar project jar-file)
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
