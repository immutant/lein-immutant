(ns leiningen.immutant
  (:require [leiningen.help :as help])
  (:use leiningen.immutant.deploy)
  (:use leiningen.immutant.init)
  (:use leiningen.immutant.undeploy)
  (:use leiningen.immutant.run))

(defn immutant
  "Manage the deployment lifecycle of an Immutant application."
  {:help-arglists '([deploy undeploy run])
   :subtasks [#'deploy #'undeploy #'run]}
  ([project]
     (println (help/help-for "immutant")))
  ([project subtask]
     (case subtask
       "init"         (init project)
       "deploy"       (deploy project)
       "undeploy"     (undeploy project)
       "run"          (run project))))
