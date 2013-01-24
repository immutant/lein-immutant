(ns leiningen.immutant.test
  (:refer-clojure :exclude [test])
  (:require [fntest.nrepl               :as fntest]
            [leiningen.immutant.common  :as common]
            [leinjacker.utils           :as lj]
            [immutant.deploy-tools.util :as util]))

(def test-options
  [["-d" "--dir"]
   ["-p" "--port"]])

(defn test
  "Runs tests inside an Immutant, after starting one (if necessary) and deploying the project"
  [project root opts]
  (when-not (fntest/run-in-container (util/app-name project root)
                                     root
                                     (assoc opts :jboss-home (common/get-jboss-home)))
    (lj/abort "Tests failed")))

