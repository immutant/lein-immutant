(ns leiningen.immutant.run
  (:require [clojure.java.io            :as io]
            [leiningen.immutant.common  :as c]
            [jboss-as.management        :as api]
            [fntest.sh                  :as sh]
            [immutant.deploy-tools.util :as util]))

(def ^:dynamic *pump-should-sleep* true)

(defn- add-shutdown-hook! [f]
  (.addShutdownHook (Runtime/getRuntime) (Thread. f)))

(let [mgt-url (atom nil)
      jboss-home (c/get-jboss-home)]
  (defn- standalone-sh []
    (str (.getAbsolutePath jboss-home) "/bin/standalone."
         (if c/windows? "bat" "sh")))

  (def find-mgt-url
    (partial sh/find-management-endpoint #(reset! mgt-url %)))

  (defn- shutdown []
    (if-let [url @mgt-url]
      (binding [api/*api-endpoint* url]
        (println "Sending a shutdown message to" url)
        (try
          (api/shutdown)
          (catch Exception _)))))

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

It also takes the additional convenience arguments:

 --clustered  Starts the Immutant in clustered mode. This is
              equivalent to passing '--server-config=standalone-ha.xml`

By default, the plugin will locate the current Immutant by looking at
~/.immutant/current. This can be overriden by setting the
$IMMUTANT_HOME environment variable."
    ([]
       (run nil))
    ([project & opts]
       (util/with-jboss-home jboss-home
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
               params (replace
                       {"--clustered" "--server-config=standalone-ha.xml"} opts)]
           (apply println "Starting Immutant:" script params)
           (binding [sh/*pump-should-sleep*
                     (not (some options-that-exit-immediately params))]
             (let [proc (sh/sh (into [script] params)
                               :line-fn find-mgt-url)]
               (add-shutdown-hook! shutdown)
               (.waitFor proc))))))))
