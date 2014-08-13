(ns leiningen.immutant
  "Tasks for managing Immutant 2.x projects in a WildFly container."
  (:require [immutant.war :refer [war]]
            [leiningen.help :as help]))

(defn immutant
  {:subtasks [#'war]}
  ([project subtask & options]
     (case subtask
       "war"  (war project options)
       (help/help project "immutant"))
     (shutdown-agents)))
