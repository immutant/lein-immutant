(ns leiningen.immutant.test
  (:refer-clojure :exclude [test])
  (:require [fntest.core                :as fntest]
            [leiningen.immutant.common  :as common]
            [immutant.deploy-tools.util :as util]
            [clojure.java.io            :as io]
            [clojure.string             :as str]))

(defn test
  "Runs a project's tests inside the current Immutant

Runs the current Immutant, if necessary, deploys the project to it,
runs all tests found beneath the test/ directory, undeploys the app,
and then shuts down the Immutant it started. All tests specified in
the :test-paths from project.clj will be executed.

If passed a bare argument, the task will assume it is a path to a
project to be tested, and will switch to the context of that
project. This works when lein is invoked in or out of a project.

By default, the plugin will locate the current Immutant by looking at
~/.lein/immutant/current. This can be overriden by setting the
$IMMUTANT_HOME environment variable."
  [project root opts]
  (println "Running tests inside Immutant...")
  (when-not (common/mapply
              fntest/test-in-container
              (util/app-name project root)
              root
              (assoc opts
                :jboss-home (common/get-jboss-home)
                :dirs (:test-paths project)))
    (common/abort "Tests failed")))

