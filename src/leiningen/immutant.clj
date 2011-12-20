(ns leiningen.immutant
  (:use [leiningen.immutant.common :only [print-help unknown-subtask]]
        leiningen.immutant.deploy
        leiningen.immutant.env
        leiningen.immutant.init
        leiningen.immutant.install
        leiningen.immutant.undeploy
        leiningen.immutant.run))

(defn immutant
  "Manage the deployment lifecycle of an Immutant application."
  {:help-arglists '([subtask]
                    [new project-name]
                    [install [version [destination-dir]]
                    [overlay [layee [version]]]]
                    [env [key]])
   :subtasks [#'install #'overlay #'env #'leiningen.immutant.init/new #'init #'deploy #'undeploy #'run]}
   ([]
      (print-help))
   ([subtask]
      (immutant nil subtask))
   ([project-or-nil subtask & args]
      (case subtask
        "install"      (apply install args)
        "overlay"      (apply overlay args)
        "env"          (apply env args)
        "new"          (leiningen.immutant.init/new (first args))
        "init"         (init project-or-nil)
        "deploy"       (deploy project-or-nil)
        "undeploy"     (undeploy project-or-nil)
        "run"          (run project-or-nil)
        (unknown-subtask subtask))))
