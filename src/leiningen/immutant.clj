(ns leiningen.immutant
  (:require [leiningen.core.classpath :as cp]
            [leiningen.core.main :refer [abort]]
            [leiningen.uberjar :as uberjar]
            [leiningen.jar :as jar]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]])
  (:import [java.io BufferedOutputStream FileOutputStream]
           java.util.Properties
           java.util.zip.ZipOutputStream
           [java.util.jar JarEntry JarOutputStream]))

(defn- main-fn [project]
  (if-let [main-ns (:main project)]
    (symbol (str main-ns) "-main")))

(defn- ring-fn [project]
  ;;TODO: implement
  )

(defn- immutant-init-fn [project]
  (-> project :immutant :init))

(defn- build-init [project uber?]
  ;;TODO: make repl optional
  (if-let [init-fn (or (immutant-init-fn project)
                     (main-fn project)
                     (ring-fn project))]
    (pr-str `(do
               (require 'immutant.wildfly)
               (immutant.wildfly/init-deployment (quote ~init-fn)
                 ~(if uber? {} {:nrepl {:host "localhost" :port 0}}))))))

(defn extract-keys
  ([m]
     (extract-keys [] m))
  ([acc m]
     (if (map? m)
       (concat (keys m) (mapcat (fn [[_ v]] (extract-keys acc v)) m)))))

(defn- extract-deps [project]
  (->> project
    (cp/dependency-hierarchy :dependencies)
    extract-keys))

(defn- locate-version [project ns]
  (if-let [version (some (fn [[dep version]]
                           (if (= ns (namespace dep))
                             version))
                     (extract-deps project))]
    version
    (abort (format "No %s dependency found in the project's dependency tree." ns))))

(defn- classpath [project]
  ;; TODO: don't displace a version that may be there
  (-> project
    (update-in [:dependencies]
      conj ['org.immutant/wildfly (locate-version project "org.immutant")
            :exclusions ['org.projectodd.wunderboss/wunderboss-wildfly]])
    cp/get-classpath))

(defn- build-descriptor [project uber?]
  (cond-> {:language "clojure"
           :init (build-init project uber?)}
    (not uber?) (merge
                  {:root (:root project)
                   :classpath (str/join ":" (classpath project))
                   "config.repl-options" (pr-str (:repl-options project))})))

(defn- map->properties [m]
  (reduce (fn [p [k v]]
            (doto p
              (.setProperty (name k) v)))
    (Properties.)
    m))

(defn- find-all-wildfly-jars [project]
  (->> (assoc project
         :dependencies [['org.immutant/wildfly (locate-version project "org.immutant")
                         :exclusions
                         ['org.immutant/core
                          'org.clojure/clojure
                          'org.projectodd.wunderboss/wunderboss-clojure]]])
    (cp/resolve-dependencies :dependencies)))

(defn- relevant-wboss-dependencies [project]
  (let [deps (mapv first (extract-deps project))
        immutant-deps (filter #(= "org.immutant" (namespace %)) deps)
        match #(some #{'org.immutant/immutant %} immutant-deps)]
    (cond-> ['org.projectodd.wunderboss/wunderboss-wildfly]
      (match 'org.immutant/caching)   (conj 'org.projectodd.wunderboss/wunderboss-caching)
      (match 'org.immutant/messaging) (conj 'org.projectodd.wunderboss/wunderboss-messaging)
      (match 'org.immutant/web)       (conj 'org.projectodd.wunderboss/wunderboss-web))))

(defn- relevant-wboss-jars [project]
  (let [wboss-version (locate-version project "org.projectodd.wunderboss")]
    (->> (assoc project
           :dependencies (mapv (fn [dep]
                                 [dep wboss-version])
                           (relevant-wboss-dependencies project)))
      (cp/resolve-dependencies :dependencies))))

(defn- build-war [project dest entries]
  (with-open [out (-> dest FileOutputStream. BufferedOutputStream. JarOutputStream.)]
    (doseq [[path content] entries]
      (.putNextEntry out (JarEntry. path))
      (io/copy content out))))

(defn resolve-path [project path]
  (if path
    (let [path-dir (io/file path)
          deployments-dir (io/file path-dir "standalone/deployments")]
      (when-not (.exists path-dir)
        (abort (format "Path '%s' does not exist." path)))
      (when-not (.isDirectory path-dir)
        (abort (format "Path '%s' is not a directory." path)))
      (if (.exists deployments-dir)
        deployments-dir
        path-dir))
    (io/file (:target-path project))))

(defn war [project path]
  (let [resolved-path (resolve-path project path)
        war-entries
        (concat
          (mapv (fn [jar]
                  [(str "WEB-INF/lib/" (.getName jar))
                   jar])
            (relevant-wboss-jars project))
          [["META-INF/app.properties"
            (with-out-str
              (-> project
                (build-descriptor false)
                map->properties
                (.store *out* "")))]
           ["WEB-INF/web.xml"
            (slurp (io/resource "web.xml"))]])
        file (io/file resolved-path (format "%s-dev.war" (:name project)))]
    (println "Creating" (.getAbsolutePath file))
    (build-war project file war-entries)
    file))

(defn uberwar [project path]
  (let [resolved-path (resolve-path project path)
        war-entries
        (concat
          [[(format "WEB-INF/lib/%s.jar" (:name project))
            (io/file (uberjar/uberjar project))]
           ["META-INF/app.properties"
            (with-out-str
              (-> project
                (build-descriptor true)
                map->properties
                (.store *out* "")))]
           ["WEB-INF/web.xml"
            (slurp (io/resource "web.xml"))]]
          (mapv (fn [f] [(format "WEB-INF/lib/%s" (.getName f))
                        f])
            (find-all-wildfly-jars project)))
        file (io/file resolved-path (format "%s.war" (:name project)))]
    (println "Creating" (.getAbsolutePath file))
    (build-war project file war-entries)
    file))

(defn immutant
  "Manage the deployment lifecycle of an Immutant application."
  {:subtasks [#'uberwar #'war]}
  ([project subtask & [path]]
     (case subtask
       "war"  (war project path)
       "uberwar" (uberwar project path)
       ;;TODO: print help for default
       )
     (shutdown-agents)))
