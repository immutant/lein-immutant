(ns leiningen.immutant
  (:use [leiningen.immutant.common :only [print-help unknown-subtask]]
        leiningen.immutant.deploy
        leiningen.immutant.init
        leiningen.immutant.install
        leiningen.immutant.undeploy
        leiningen.immutant.run))

(defn immutant
  "Manage the deployment lifecycle of an Immutant application."
  {:help-arglists '([subtask] [new project-name] [install [version [destination-dir]]])
   :subtasks [#'install #'leiningen.immutant.init/new #'init #'deploy #'undeploy #'run]}
   ([]
      (print-help))
   ([subtask]
      (case subtask
        "run" (run)
        (unknown-subtask subtask)))
   ([project-or-nil subtask & args]
      (case subtask
        "new"          (leiningen.immutant.init/new (first args))
        "install"      (apply install args)
        "init"         (init project-or-nil)
        "deploy"       (deploy project-or-nil)
        "undeploy"     (undeploy project-or-nil)
        "run"          (run project-or-nil)
        (unknown-subtask subtask))))
