(ns leiningen.immutant.run
  (:require [clojure.java.io            :as io]
            [leiningen.immutant.common  :as c]
            [leiningen.immutant.install :as install]
            [leiningen.immutant.deploy  :as deploy]
            [jboss-as.management        :as api]
            [immutant.deploy-tools.util :as util]
            [clojure.string             :as str]
            [clojure.java.browse        :as browse]))

(def ^:dynamic *pump-should-sleep* true)

(defn- pump
  "Borrowed from leiningen.core/eval and modified."
  [reader out line-fn]
  (let [line-fn (or line-fn identity)]
    (loop [line (.readLine reader)]
      (when line
        (.write out (str (line-fn line) "\n"))
        (.flush out)
        (when *pump-should-sleep*
          (Thread/sleep 10))
        (recur (.readLine reader))))))

(defn- add-shutdown-hook! [f]
  (.addShutdownHook (Runtime/getRuntime) (Thread. f)))

(defn- sh
  "A version of clojure.java.shell/sh that streams out/err, and returns the process created.
It also allows applying a fn to each line of the output. Borrowed from leiningen.core/eval
and modified."
  [cmd & [line-fn]]
  (let [proc (.exec (Runtime/getRuntime)
                    (into-array cmd))]
    (.start
     (Thread.
      (bound-fn []
        (with-open [out (io/reader (.getInputStream proc))
                    err (io/reader (.getErrorStream proc))]
          (doto (Thread. (bound-fn []
                           (pump out *out* line-fn)))
            .start
            .join)
          (doto (Thread. (bound-fn []
                           (pump err *err* line-fn)))
            .start
            .join)))))
    proc))

(defn expand-options
  "Replace convenient plugin options with their JBoss counterparts"
  [opts]
  (remove empty?
    (-> (str/join " " opts)
      (str/replace "--clustered" "--server-config=standalone-ha.xml")
      (str/replace #"(--offset|-o)(?:=|\s+)(\S+)" "-Djboss.socket.binding.port-offset=$2")
      (str/replace #"(--node-name|-n)(?:=|\s+)(\S+)" "-Djboss.node.name=$2")
      (str/replace #"(--log-level)(?:=|\s+)(\S+)" "-Djboss.logging.level=$2")
      (str/split #" "))))

(defn munge-logging-properties [params]
  (when-let [[_ level] (some #(re-find #"-Djboss.logging.level=(\S+)$" %) params)]
    (let [propfile (io/file (c/get-jboss-home) "standalone/configuration/logging.properties")]
      (when (.exists propfile)
        (try
          (-> propfile
            slurp
            (str/replace #"logger\.level=.*\n"
              (format "logger.level=%s\n" level))
            (str/replace #"handler\.CONSOLE\.level=.*\n"
              (format "handler.CONSOLE.level=%s\n" level))
            (as-> content (spit propfile content)))
          (catch java.io.IOException _))))))

(let [log-data (atom nil)]
  (defn- standalone-sh []
    (str (.getAbsolutePath (c/get-jboss-home)) "/bin/standalone."
         (if c/windows? "bat" "sh")))

  (defn- find-mgt-url [line]
    (when-let [match (re-find #"Http management interface listening on (.*/management)" line)]
      (swap! log-data assoc :mgt-url (last match)))
    line)

  (defn- find-handler-urls [line]
    (when-let [match (re-find #"Starting handler for (.*) at: (.*)$" line)]
      (swap! log-data update-in [:handler-urls (second match)] #(conj % (last match))))
    line)

  (defn- find-deployment-completion [line]
    (when-let [match (re-find #"JBAS018559: Deployed \"([^.]*)\.clj\"" line)]
      (swap! log-data update-in [:deployed] #(conj % (last match))))
    line)
    
  (defn- shutdown []
    (if-let [url (:mgt-url @log-data)]
      (try
        (println "Sending a shutdown message to" url)
        (api/stop (api/->Standalone url nil))
        (catch Exception _))))

  (def options-that-exit-immediately
    #{"-h" "--help" "-v" "-V" "--version"})
  
  ;;TODO: fix run to use cli for options
  (defn run
    "Starts up the current Immutant, displaying its console output

The Immutant to start is specified by the ~/.immutant/current
link or $IMMUTANT_HOME, with the environment variable taking
precedence. You can shutdown the Immutant with ^C.

This task delegates to $JBOSS_HOME/bin/standalone.[sh|bat], and
takes any arguments the standalone script accepts. To see a full
list, run `lein immutant run --help`.

It also takes some additional convenience arguments:

 --clustered       Starts the Immutant in clustered mode. Equivalent
                   to passing `--server-config=standalone-ha.xml`
 --node-name=x     (-n=x) To provide unique name when running multiple on
                   same host. Equivalent to `-Djboss.node.name=x`
 --offset=100      (-o=100) To avoid port conflicts when running multiple on
                   same host. Equivalent to `-Djboss.socket.binding.port-offset=100`
 --log-level=DEBUG Sets the default logging level to DEBUG (or any other
                   given valid level). Equivalent to
                   `-Djboss.logging.level=DEBUG`.
 
By default, the plugin will locate the current Immutant by looking at
~/.immutant/current. This can be overriden by setting the
$IMMUTANT_HOME environment variable. If no Immutant install can be
located, the latest stable release will be installed."
    ([]
       (run nil))
    ([project & opts]
       (install/auto-install)
       (util/with-jboss-home (c/get-jboss-home)
         (when project
           (if-not (util/application-is-deployed? project nil nil)
             (c/err "Warning: The current app may not be deployed - deploy with 'lein immutant deploy'"))
           (if-let [profiles (c/extract-profiles project)]
             (c/err
              (format
               "Warning: You specified profiles, but they cannot be applied to deployed
         applications from the run task. You will instead need to deploy
         the application with those profiles set:
           %s\n"
               (c/deploy-with-profiles-cmd profiles)))))
         (let [script (standalone-sh)
               params (expand-options opts)]
           (munge-logging-properties params)
           (apply println "Starting Immutant:" script params)
           (binding [*pump-should-sleep*
                     (not (some options-that-exit-immediately params))]
             (let [proc (sh (into [script] params)
                          (comp find-mgt-url find-handler-urls
                            find-deployment-completion))]
               (add-shutdown-hook! shutdown)
               (.waitFor proc)))))))

  (defn- open-in-browser [app-name]
    (future
      (loop [attempts 1000]
        (if (some #{app-name} (:deployed @log-data))
          (when-let [urls (get-in @log-data [:handler-urls app-name])]
            (println "Opening" (last urls))
            (browse/browse-url (last urls)))
          (do
            (Thread/sleep 100)
            (if (> attempts 0)
              (recur (dec attempts))))))))
  
  (defn server
    "Deploys the current app to the current Immutant and runs it

Analogous to `lein ring server`. Takes an optional http port
parameter.

By default, the plugin will locate the current Immutant by looking at
~/.immutant/current. This can be overriden by setting the
$IMMUTANT_HOME environment variable. If no Immutant install can be
located, the latest stable release will be installed."
    [project port]
    (try
      (if port (Integer/parseInt port))
      (catch NumberFormatException _
        (c/abort "Argument must be a port.")))   
    (-> (deploy/deploy project nil nil)
      .getName
      (str/split #"\.")
      first
      open-in-browser)
    (let [run-opts (if port (str "-Dhttp.port=" port))]
      (run project run-opts))))
