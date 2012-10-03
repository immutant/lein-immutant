(ns leiningen.immutant
  (:require [leiningen.immutant.deploy  :as deploy]
            [leiningen.immutant.env     :as env]
            [leiningen.immutant.test    :as test]
            [leiningen.immutant.init    :as init]
            [leiningen.immutant.archive :as archive]
            [leiningen.immutant.install :as install]
            [leiningen.immutant.run     :as run]
            [leiningen.immutant.common  :as common]
            [clojure.java.io            :as io]
            [clojure.tools.cli          :as cli]))

(def cli-options
  {"deploy"  deploy/deploy-options
   "undeploy" deploy/undeploy-options
   "archive" archive/archive-options})

(defn immutant
  "Manage the deployment lifecycle of an Immutant application."
  {:no-project-needed true
   :subtasks [#'init/new #'install/install #'install/overlay #'env/env #'init/init #'archive/archive #'deploy/deploy #'deploy/undeploy #'run/run]}
  ([] 
     (common/print-help)) ;; lein1
  ([subtask]
     (if common/lein2?
       (common/print-help)
       (immutant nil subtask)))
  ([project-or-nil subtask & args]
     (if (= "run" subtask)
       ;; run currently handles its own options
       (apply run/run project-or-nil args)
       (let [[options other-args banner] (apply cli/cli args (cli-options subtask))
             root-dir (common/get-application-root other-args)]
         (case subtask
           "install"      (apply install/install other-args)
           "overlay"      (apply install/overlay other-args)
           "env"          (apply env/env other-args)
           "new"          (init/new (first other-args))
           "init"         (init/init project-or-nil)
           "archive"      (apply archive/archive
                                 (conj (common/resolve-project project-or-nil root-dir) options other-args))
           "deploy"       (apply deploy/deploy
                                 (conj (common/resolve-project project-or-nil root-dir) options))
           "undeploy"     (apply deploy/undeploy
                                 (conj (common/resolve-project project-or-nil root-dir) options))
           "test"         (apply test/test
                                 (common/resolve-project project-or-nil root-dir))
           (common/unknown-subtask subtask))))
     (shutdown-agents)))
