(ns immutant.war
  "Generate war files for WildFly."
  (:refer-clojure :exclude [add-classpath])
  (:require [leiningen.core.classpath :as cp]
            [leiningen.core.main :refer [abort warn]]
            [leiningen.uberjar :as uberjar]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [immutant.util :as u]
            [immutant.deploy-tools.war :as dt-war]))

(defn resolve-path [project path]
  (if path
    (let [dir (io/file path)
          deployments-dir (io/file dir "standalone/deployments")]
      (when-not (.exists dir)
        (abort (format "Path '%s' does not exist." path)))
      (when-not (.isDirectory dir)
        (abort (format "Path '%s' is not a directory." path)))
      (if (.exists deployments-dir)
        deployments-dir
        path))
    (io/file (:target-path project))))

(def option-specs
  [["-d" "--dev"
    "Generate a 'dev' war"
    :id :dev?]
   [nil  "--no-dev"
    "Generate an uberwar (default)"
    :id :no-dev?]
   ["-c" "--context-root CONTEXT"
    "Deploy to this context root"]
   ["-v" "--virtual-host HOST"
    "Deploy to the named host defined in the WildFly config"]
   ["-o" "--destination DIR"
    "Write the generated war to DIR"]
   ["-n" "--name NAME"
    "Override the name of the war (sans the .war suffix)"]
   ["-r" "--resource-paths DIR1,DIR2"
    "Paths to file trees to include in the top level of the war"
    :parse-fn #(str/split % #",")]
   [nil  "--nrepl-host HOST"
    "Host for nrepl to bind to"]
   [nil  "--nrepl-port PORT"
    "Port for nrepl to bind to"
    :parse-fn read-string]
   [nil  "--nrepl-port-file FILE"
    "File to write actual nrepl port to"]
   [nil  "--nrepl-start"
    "Request nrepl to start (default for 'dev' wars)"]
   [nil  "--no-nrepl-start"
    "Don't request nrepl to start (default for uberwars)"]])

(defn absolutize [root path]
  (.getAbsolutePath
    (let [file (io/file path)]
      (if (.isAbsolute file)
        file
        (io/file root file)))))

(defn coerce-options [options]
  (reduce
    (fn [m [path coerce-fn]]
      (if (get-in m path)
        (update-in m path coerce-fn)
        m))
    options
    (let [absolutize-fn (partial absolutize (:root options))]
      {[:war-resource-paths] (partial map absolutize-fn)
       [:nrepl :port-file] absolutize-fn})))

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

(defn add-init-fn [project options]
  (if-let [main-ns (:main project)]
    (assoc options
      :init-fn (symbol (str main-ns) "-main"))
    (do
      (warn "No :main specified in project.clj, no app initialization will be performed.")
      options)))

(defn add-uberjar
  "Builds and adds the uberjar to the options if we're building an uberwar."
  [project options]
  (if (:dev? options)
    options
    (assoc options
      :uberjar (io/file (uberjar/uberjar project)))))

(defn add-classpath
  [project options]
  (if (:dev? options)
    (assoc options
      :classpath (cp/get-classpath project))
    options))

(defn add-alias [src-path dest-path m]
  (assoc-in m dest-path (get-in m src-path)))

(defn help-war []
  (format "%s\n\n%s\n\n%s\n\n%s\n"
    "Generates a war file suitable for deploying to a WildFly container."
    "Valid options are:"
    (u/options-summary option-specs)
    "For detailed help, see `lein help immutant deployment`."))

(defn war
  "Generates a war file suitable for deploying to a WildFly container."
  [project args]
  (let [options (-> args
                  (u/parse-options option-specs help-war)
                  (merge-options project)
                  (->> (add-uberjar project)
                    (add-init-fn project)
                    (add-classpath project)
                    (merge project)
                    (add-alias [:immutant :war :resource-paths] [:war-resource-paths])
                    (add-alias [:repl-options] [:nrepl :options])
                    coerce-options))
        file (io/file (resolve-path project (:destination options))
               (war-name project options))]
    (try
      (dt-war/create-war file options)
      (catch Exception e
        (abort (str "Error: " (.getMessage e)))))
    (println "Created" (.getAbsolutePath file))
    file))
