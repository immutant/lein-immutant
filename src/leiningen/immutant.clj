(ns leiningen.immutant
  (:use leiningen.immutant.deploy
        leiningen.immutant.env
        leiningen.immutant.init
        leiningen.immutant.archive
        leiningen.immutant.install
        leiningen.immutant.run)
  (:require [clojure.java.io           :as io]
            [leiningen.immutant.common :as common]))

(defn immutant
  "Manage the deployment lifecycle of an Immutant application."
  {:no-project-needed true
   :help-arglists '([subtask]
                    [new project-name]
                    [install [version [destination-dir]]
                    [overlay [feature-set [version]]]]
                    [env [key]]
                    [archive [path/to/project]]    
                    [deploy [--archive] [--profiles ":p1,:p2"] [path/to/project]]
                    [undeploy [path/to/project]])
   :subtasks [#'leiningen.immutant.init/new #'install #'overlay #'env #'init #'archive #'deploy #'undeploy #'run]}
  ([] 
     (common/print-help)) ;; lein1
  ([subtask]
     (if common/lein2?
       (common/print-help)
       (immutant nil subtask)))
  ([project-or-nil subtask & args]
     (let [root-dir (common/get-application-root args)]
       (case subtask
         "install"      (apply install args)
         "overlay"      (apply overlay args)
         "env"          (apply env args)
         "new"          (leiningen.immutant.init/new (first args))
         "init"         (init project-or-nil)
         "archive"      (apply archive
                               (common/resolve-project project-or-nil root-dir))
         "deploy"       (apply deploy
                               (concat (common/resolve-project project-or-nil root-dir) args))
         "undeploy"     (apply undeploy
                               (common/resolve-project project-or-nil root-dir))
         "run"          (apply run project-or-nil args)
         (common/unknown-subtask subtask)))
     (shutdown-agents)))

