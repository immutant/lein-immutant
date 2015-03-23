(ns immutant.test
  (:refer-clojure :exclude [test])
  (:require [leiningen.core.main :refer [abort]]
            [leiningen.core.project :as project]
            [fntest.core :as fntest]
            [clojure.java.io :as io]
            [immutant.lein.util :as u]
            [immutant.war :refer [war]]))

(def option-specs
  [["-c" "--cluster"
    "Deploy the test application to a cluster"
    :id :cluster?]
   [nil  "--no-cluster"
    "Deploy the test application to a standalone server (default)"
    :id :no-cluster?]
   ["-d" "--debug"
    "Start the server with debugging enabled"
    :id :debug?]
   [nil "--no-debug"
    "Don't enable debugging on server start (default)"
    :id :no-debug?]
   ["-j" "--jboss-home PATH"
    "Use the WildFly at PATH"]
   ["-o" "--offset OFFSET"
    "Offset the WildFly network ports"
    :parse-fn read-string]])

(defn jboss-home [options]
  (if-let [home (:jboss-home options (System/getenv "JBOSS_HOME"))]
    (if (.exists (io/file home))
      home
      (abort (format "jboss home '%s' does not exist." home)))
    (abort "No jboss home specified. Specify via --jboss-home or $JBOSS_HOME.")))

(defn generate-war [project port-file]
  (war (update-in project [:immutant :war]
         #(-> %
            (assoc :dev? true)
            (update-in [:nrepl] assoc
              :port 0
              :host "localhost"
              :start? true
              :port-file (.getAbsolutePath port-file))))
    nil))

(defn help-test []
  (format "%s\n\n%s\n\n%s\n\n%s\n"
    "Runs a project's tests inside WildFly."
    "Valid options are:"
    (u/options-summary option-specs)
    "For detailed help, see `lein help immutant testing`."))

(defn test
  "Runs a project's tests inside WildFly."
  [project args]
  (let [project (project/merge-profiles project [:leiningen/test :test])
        options (merge
                  (-> project :immutant :test)
                  (u/parse-options args option-specs help-test))
        jboss-home (jboss-home options)
        port-file (io/file (:target-path project) "fntest-nrepl-port")]
    (println
      (format
        "Running tests inside WildFly (log output available in %s/isolated-wildfly/%s/log/server.log)..."
        (:target-path project)
        (if (:cluster? options) "domain/servers/*" "standalone")))
    (when-not (u/mapply
                fntest/test-in-container
                (str (:name project) ".war")
                (:root project)
                (-> options
                  (assoc
                      :profiles (or
                                  (u/extract-profiles project)
                                  [:dev :test])
                      :jboss-home jboss-home
                      :dirs (:test-paths project)
                      :port-file port-file
                      :war-file (generate-war project port-file)
                      :modes fntest/default-modes)
                  (cond->
                    (:cluster? options) (update-in [:modes] conj :domain)
                    (:debug? options)   (update-in [:modes] conj :debug))))
      (abort "Tests failed"))))
