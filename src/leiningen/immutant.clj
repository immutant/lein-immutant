(ns leiningen.immutant
  "Tasks for managing Immutant 2.x projects in a WildFly container."
  (:refer-clojure :exclude [test])
  (:require [immutant.war :refer [war]]
            [immutant.test :refer [test]]
            [leiningen.help :as help]))

(defn immutant
  {:subtasks [#'war #'test]}
  ([project subtask & options]
     (case subtask
       "war"  (war project options)
       "test" (test project options)
       (help/help project "immutant"))
     (shutdown-agents)))
