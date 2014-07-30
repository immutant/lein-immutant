(ns leiningen.immutant
  (:require [leiningen.core.classpath :as cp]
            [leiningen.core.main :refer [abort]]
            [leiningen.uberjar :as uberjar]
            [leiningen.jar :as jar]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.cli :as opts])
  (:import [java.io BufferedOutputStream FileOutputStream]
           java.util.Properties
           java.util.zip.ZipOutputStream
           [java.util.jar JarEntry JarOutputStream]))

(defn main-fn [project]
  (if-let [main-ns (:main project)]
    (symbol (str main-ns) "-main")
    (abort "No :main specified in project.clj.")))

(defn build-init [project options]
  (pr-str
    `(do
       (require 'immutant.wildfly)
       (immutant.wildfly/init-deployment (quote ~(main-fn project))
         ~(if (-> options :nrepl :start?)
            {:nrepl (merge {:host "localhost" :port 0}
                      (:nrepl options))}
            {})))))

(defn extract-keys
  ([m]
     (extract-keys [] m))
  ([acc m]
     (if (map? m)
       (concat (keys m) (mapcat (fn [[_ v]] (extract-keys acc v)) m)))))

(defn extract-deps [project]
  (->> project
    (cp/dependency-hierarchy :dependencies)
    extract-keys))

(defn locate-version [project ns]
  (if-let [version (some (fn [[dep version]]
                           (if (= ns (namespace dep))
                             version))
                     (extract-deps project))]
    version
    (abort (format "No %s dependency found in the project's dependency tree." ns))))

(defn classpath [project]
  ;; TODO: don't displace a version that may be there
  (-> project
    (update-in [:dependencies]
      conj ['org.immutant/wildfly (locate-version project "org.immutant")
            :exclusions ['org.projectodd.wunderboss/wunderboss-wildfly]])
    cp/get-classpath))

(defn build-descriptor [project options]
  (cond-> {:language "clojure"
           :init (build-init project options)}
    (:dev? options)   (merge
                        {:root (:root project)
                         :classpath (str/join ":"
                                      (classpath project))})
    (-> options
      :nrepl :start?) (merge
                        {"config.repl-options"
                         (pr-str (:repl-options project))})))

(defn map->properties [m]
  (reduce (fn [p [k v]]
            (doto p
              (.setProperty (name k) v)))
    (Properties.)
    m))

(defn build-war [project dest specs]
  (with-open [out (-> dest FileOutputStream. BufferedOutputStream. JarOutputStream.)]
    (doseq [[path content] specs]
      (.putNextEntry out (JarEntry. path))
      (io/copy content out))))

(defn resolve-path [project path]
  (if path
    (let [deployments-dir (io/file path "standalone/deployments")]
      (when-not (.exists path)
        (abort (format "Path '%s' does not exist." path)))
      (when-not (.isDirectory path)
        (abort (format "Path '%s' is not a directory." path)))
      (if (.exists deployments-dir)
        deployments-dir
        path))
    (io/file (:target-path project))))

(defn parse-options [args]
  (let [{:keys [options errors]}
        (opts/parse-opts args
          [["-d" "--dev"                  :id :dev?]
           ["-o" "--dest DIR"             :parse-fn io/file]
           ["-n" "--name NAME"]
           ["-r" "--resource-dir DIR"     :parse-fn io/file]
           [nil  "--nrepl-host HOST"]
           [nil  "--nrepl-port PORT"      :parse-fn read-string]
           [nil  "--nrepl-port-file FILE"]
           [nil  "--nrepl-start"]])]
    (when errors
      (abort (str/join "\n" errors)))
    options))

(defn merge-options
  [{:keys [nrepl-host nrepl-port nrepl-port-file nrepl-start] :as options}
   project]
  (let [merged-opts
        (merge-with
          #(if (map? %1)
             (merge %1 %2)
             %2)
          {:name "%p%t"}
          (-> project :immutant :war)
          (update-in options [:nrepl]
            #(cond-> %
               nrepl-host                       (assoc :host nrepl-host)
               nrepl-port                       (assoc :port nrepl-port)
               nrepl-port-file                  (assoc :port-file nrepl-port-file)
               (contains? options :nrepl-start) (assoc :start? nrepl-start))))]
    (if (and (:dev? merged-opts)
          (not (-> merged-opts :nrepl (contains? :start?))))
      (assoc-in merged-opts [:nrepl :start?] true)
      merged-opts)))

(defn war-name [project options]
  (-> (:name options)
    (str/replace #"%p" (:name project))
    (str/replace #"%v" (str "-" (:version project)))
    (str/replace #"%t" (if (:dev? options) "-dev" ""))
    (str ".war")))

(defn add-app-properties [m project options]
  (assoc m
    "META-INF/app.properties"
    (with-out-str
      (-> project
        (build-descriptor options)
        map->properties
        (.store *out* "")))))

(defn add-file-spec [specs path file]
  (let [path (format "%s%s%s"
               path
               (if (empty? path) "" "/")
               (.getName file))]
    (assoc specs
      (if (.startsWith path "/")
        (.substring path 1)
        path)
      file)))

(defn find-required-wboss-dependencies [project]
  (let [deps (mapv first (extract-deps project))
        immutant-deps (filter #(= "org.immutant" (namespace %)) deps)
        match #(some #{'org.immutant/immutant %} immutant-deps)]
    (cond-> ['org.projectodd.wunderboss/wunderboss-wildfly]
      (match 'org.immutant/caching)   (conj 'org.projectodd.wunderboss/wunderboss-caching)
      (match 'org.immutant/messaging) (conj 'org.projectodd.wunderboss/wunderboss-messaging)
      (match 'org.immutant/web)       (conj 'org.projectodd.wunderboss/wunderboss-web))))

(defn wboss-jars-for-dev [project]
  (let [wboss-version (locate-version project "org.projectodd.wunderboss")]
    (->> (assoc project
           :dependencies (mapv (fn [dep]
                                 [dep wboss-version])
                           (find-required-wboss-dependencies project)))
      (cp/resolve-dependencies :dependencies))))

(defn all-wildfly-jars [project]
  (->> (assoc project
         :dependencies [['org.immutant/wildfly (locate-version project "org.immutant")
                         :exclusions
                         ['org.immutant/core
                          'org.clojure/clojure
                          'org.projectodd.wunderboss/wunderboss-clojure]]])
    (cp/resolve-dependencies :dependencies)))

(defn add-dependency-jars [specs project options]
  (reduce #(add-file-spec %1 "WEB-INF/lib" %2)
    specs
    (if (:dev? options)
      (wboss-jars-for-dev project)
      (all-wildfly-jars project))))

(defn add-top-level-resources [specs _ options]
  (if-let [resource-dir (:resource-dir options)]
    (if (.exists resource-dir)
      (reduce
        (fn [m file]
          (if (.isFile file)
            (add-file-spec m
              (.substring (.getParent (.getAbsoluteFile file))
                (.length (.getAbsolutePath resource-dir)))
              file)
            m))
        specs
        (file-seq resource-dir))
      (abort (format ":resource-dir '%s' does not exist."
               (.getPath resource-dir)))))
  specs)

(defn add-web-xml [specs project _]
  (if (specs "WEB-INF/web.xml")
    specs
    (let [content (slurp (io/resource "web.xml"))]
      (spit (io/file (:target-path project) "web.xml") content)
      (assoc specs "WEB-INF/web.xml" content))))

(defn add-uberjar [specs project options]
  (if (:dev? options)
    specs
    (add-file-spec specs "WEB-INF/lib"
      (io/file (uberjar/uberjar project)))))

(defn war [project args]
  (let [options (-> args
                  parse-options
                  (merge-options project))
        file (io/file (resolve-path project (:dest options))
               (war-name project options))]
    (build-war project file
      (-> {}
        (add-uberjar project options)
        (add-app-properties project options)
        (add-dependency-jars project options)
        (add-top-level-resources project options)
        (add-web-xml project options)))
    (println "Created" (.getAbsolutePath file))
    file))

(defn immutant
  "Manage the deployment lifecycle of an Immutant application."
  {:subtasks [#'war]}
  ([project subtask & args]
     (case subtask
       "war"  (war project args)
       ;;TODO: print help for default
       )
     (shutdown-agents)))
