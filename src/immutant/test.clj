(ns immutant.test
  (:refer-clojure :exclude [test])
  (:require [leiningen.core.main :refer [abort]]
            [fntest.core :as fntest]
            [clojure.java.io :as io]
            [immutant.util :as u]))

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

(defn test
  "Runs a project's tests inside WildFly."
  [project args]
  (let [options (merge
                  (-> project :immutant :test)
                  (u/parse-options args option-specs))
        jboss-home (jboss-home options)]
    (println
      (format
       "Running tests inside WildFly (log output available in target/isolated-wildfly/%s/log/server.log)..."
       (if (:cluster? options) "domain/servers/*" "standalone")))
    (when-not (u/mapply
                fntest/test-in-container
                (str (:name project) ".war")
                (:root project)
                (cond-> options
                  true (assoc
                           :profiles (or
                                       (u/extract-profiles project)
                                       [:dev :test])
                           :jboss-home jboss-home
                           :dirs (:test-paths project))
                  (:cluster? options) (assoc
                                          :modes (conj fntest/default-modes :domain))))
      (abort "Tests failed"))))
