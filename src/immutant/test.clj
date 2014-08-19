(ns immutant.test
  (:refer-clojure :exclude [test])
  (:require [leiningen.core.main :refer [abort]]
            [fntest.core :as fntest]
            [clojure.java.io :as io]
            [immutant.util :as u]
            [immutant.war :refer [war]]))

(def option-specs
  [["-c" "--cluster"         :id :cluster?]
   [nil  "--no-cluster"      :id :no-cluster?]
   ["-j" "--jboss-home PATH"]
   ["-o" "--offset OFFSET"   :parse-fn read-string]])

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

(defn test
  "Runs a project's tests inside WildFly."
  [project args]
  (let [options (merge
                  (-> project :immutant :test)
                  (u/parse-options args option-specs))
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
                      :war-file (generate-war project port-file))
                  (cond->
                    (:cluster? options)
                    (assoc
                        :modes (conj fntest/default-modes :domain)))))
      (abort "Tests failed"))))
