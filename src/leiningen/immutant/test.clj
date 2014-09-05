(ns leiningen.immutant.test
  (:refer-clojure :exclude [test])
  (:require [fntest.core                :as fntest]
            [leiningen.immutant.common  :as common]
            [leiningen.immutant.install :as install]
            [immutant.deploy-tools.util :as util]
            [clojure.java.io            :as io]
            [clojure.string             :as str]))

(def test-options
  [["--offset" :parse-fn read-string]
   ["--log-level"]
   ["-f" "--format"]
   ["-o" "--output-file"]])

(defn test
  "Runs a project's tests inside the current Immutant

  Runs the current Immutant, if necessary, deploys the project to it,
  runs all tests found beneath the test/ directory, undeploys the app,
  and then shuts down the Immutant it started. All tests specified in
  the :test-paths from project.clj will be executed.

  If passed a bare argument, the task will assume it is a path to a
  project to be tested, and will switch to the context of that
  project. This works when lein is invoked in or out of a project.

  The test Immutant instance is started with offset ports so it won't
  collide with a dev Immutant you may be running on the standard
  ports. By default, the ports are offset by 67, but you can override
  that with the --offset option.

  You can override the default logging level of INFO with the
  --log-level option. The server.log is avaiable in
  target/isolated-immutant/standalone/log/.

  If your tests are using clojure.test, you can specify an alternate
  output format with --format (or -f), and/or output the results to a
  file with --output-file (or -o). Options for --format are \"tap\" or
  \"junit\".

  If you only want to run a subset of your tests inside or outside of
  the container, you can separate the tests in to different
  directories, and use profiles to activate the correct tests
  in-container. Given the following project.clj snippet:

      :test-paths [\"outside-tests\"]
      :profiles {:inside {:test-paths ^:replace [\"inside-tests\"]}}

  `lein test` will only run the outside tests, while
  `lein with-profile inside immutant test` will only run the inside
  tests within Immutant.

  If you want to run all of the tests within Immutant, remove the
  `:replace` metadata to have `inside-tests` added to the existing
  `:test-paths`.

  By default, the plugin will locate the current Immutant by looking
  at ~/.immutant/current. This can be overriden by setting the
  $IMMUTANT_HOME environment variable. If no Immutant install can be
  located, the latest stable release will be installed."
  [project root opts]
  (when-not (contains? #{"tap" "junit" nil} (:format opts))
    (common/abort (format "'%s' isn't a valid format. Valid formats are: 'tap', 'junit'" (:format opts))))
  (println "Running tests inside Immutant (log output available in target/isolated-immutant/standalone/log/server.log)...")
  (install/auto-install)
  (common/prep-tasks project)
  (when-not (common/mapply
              fntest/test-in-container
              (util/app-name project root)
              root
              (assoc opts
                :profiles (or
                            (common/extract-profiles project)
                            [:dev :test])
                :jboss-home (common/get-jboss-home)
                :dirs (:test-paths project)))
    (common/abort "Tests failed")))
