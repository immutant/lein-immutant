(ns leiningen.immutant)

(defn immutant
  "Manage the deployment lifecycle of an Immutant application."
  {:subtasks [;#'deploy
              ]}
  ([project-or-nil subtask & args]
     (println "nothin' doin'")
     (shutdown-agents)))
