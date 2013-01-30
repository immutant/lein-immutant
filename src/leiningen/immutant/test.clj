(ns leiningen.immutant.test
  (:refer-clojure :exclude [test])
  (:require [fntest.core                :as fntest]
            [leiningen.immutant.common  :as common]
            [leinjacker.utils           :as lj]
            [immutant.deploy-tools.util :as util]))

(def test-options
  [["-d" "--dir"]
   ["-p" "--port"]])

(defn test
  "Runs a project's tests inside the current Immutant

Runs the current Immutant, if necessary, deploys the project to it,
runs all tests found beneath the test/ directory, undeploys the app,
and then shuts down the Immutant it started. The --port option
specifies the nREPL service port through which the tests are invoked
inside the running Immutant.

By default, the plugin will locate the current Immutant by looking at
~/.lein/immutant/current. This can be overriden by setting the
$IMMUTANT_HOME environment variable."
  [project root opts]
  (when-not (common/mapply
              fntest/test-in-container
              (util/app-name project root)
              root
              (assoc opts :jboss-home (common/get-jboss-home)))
    (lj/abort "Tests failed")))

