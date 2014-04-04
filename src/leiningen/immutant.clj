(ns leiningen.immutant
  (:require [leiningen.immutant.archive :as archive]
            [leiningen.immutant.common  :as common]
            [leiningen.immutant.deploy  :as deploy]
            [leiningen.immutant.env     :as env]
            [leiningen.immutant.install :as install]
            [leiningen.immutant.list    :as list]
            [leiningen.immutant.run     :as run]
            [leiningen.immutant.test    :as test]
            [clojure.tools.cli          :as cli]))

(def cli-options
  {"archive"   archive/archive-options
   "deploy"    deploy/deploy-options
   "install"   install/install-options
   "list"      list/list-options
   "test"      test/test-options})

(defn- subtask-with-resolved-project
  [subtask project-or-nil root-dir options]
  (let [project-and-root (common/resolve-project project-or-nil root-dir)]
    (common/bind-config
     (first project-and-root)
     (apply subtask
            (conj project-and-root options)))))

(defn- handle-undeploy [project-or-nil root-dir options other-args]
  (let [glob-or-path (first other-args)
        descriptors (seq (deploy/matching-deployments glob-or-path))
        path-undeploy #(subtask-with-resolved-project
                         deploy/undeploy project-or-nil
                         root-dir options)]
    (cond
     (and glob-or-path
          descriptors) (deploy/undeploy-descriptors descriptors)
     glob-or-path      (do
                         (println
                          (format "'%s' didn't match any deployments, assuming it's a path"
                                  glob-or-path))
                         (path-undeploy))
    :default           (path-undeploy))))

(defn immutant
  "Manage the deployment lifecycle of an Immutant application."
  {:no-project-needed true
   :subtasks [#'install/install
              #'install/overlay
              #'install/version
              #'env/env
              #'archive/archive
              #'deploy/deploy
              #'deploy/undeploy
              #'list/list
              #'run/run
              #'run/server
              #'test/test]}
  ([subtask]
     (common/print-help))
  ([project-or-nil subtask & args]
     (common/bind-config
      project-or-nil
      (case subtask
        ;; run & server currently handle their own options
        "run"    (apply run/run project-or-nil args)
        "server" (run/server project-or-nil (first args))
        
        (let [[options other-args banner] (apply cli/cli args (cli-options subtask))
              root-dir (common/get-application-root other-args)]
          (case subtask
            "install"  (apply install/install options other-args)
            "overlay"  (apply install/overlay other-args)
            "version"  (install/version)
            "env"      (apply env/env other-args)
            "archive"  (subtask-with-resolved-project
                        archive/archive project-or-nil root-dir options)
            "deploy"   (subtask-with-resolved-project
                        deploy/deploy project-or-nil root-dir options)
            "undeploy" (handle-undeploy project-or-nil root-dir options other-args)
            "list"     (list/list options)
            "test"     (subtask-with-resolved-project
                        test/test project-or-nil root-dir options)
                        (common/unknown-subtask subtask)))))
     (shutdown-agents)))
