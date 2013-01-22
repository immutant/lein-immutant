(ns leiningen.immutant.deploy-test
  (:use midje.sweet
        leiningen.immutant.test-helper)
  (:require [clojure.java.io :as io]))

;; TODO:
;; * deploy --archive
;; * undeploy in all its permutations

(setup-test-env)

(for-all-generations
  (println "\n==> Testing with lein generation" *generation*)

  (let [project-dir (io/file (io/resource "test-project"))]
    
    (with-tmp-dir)
    
    (facts "deploy"
      (facts "in a project"
        (fact (str "with no args should work for lein " *generation*)
          (with-tmp-jboss-home
            (let [env (assoc base-lein-env "JBOSS_HOME" *tmp-jboss-home*)
                  dd (io/file *tmp-deployments-dir* "test-project.clj")]
              (run-lein *generation* "immutant" "deploy"
                        :dir project-dir
                        :env env)              => 0
              (.exists dd)                     => true
              (:root (read-string (slurp dd))) => (.getAbsolutePath project-dir))))

        (fact (str "with path arg should print a warning for lein " *generation*)
          (with-tmp-jboss-home
            (let [env (assoc base-lein-env "JBOSS_HOME" *tmp-jboss-home*)
                  dd (io/file *tmp-deployments-dir* "test-project.clj")
                  result 
                  (run-lein *generation* "immutant" "deploy" "yarg"
                            :dir project-dir
                            :env env
                            :return-result? true)]
              (re-find #"specified a root path of 'yarg'" (:out result)) =not=> nil
              (:exit result)                   => 0
              (.exists dd)                     => true
              (:root (read-string (slurp dd))) => (.getAbsolutePath project-dir))))
                
        (fact (str "with a --name arg should work for lein " *generation*)
          (with-tmp-jboss-home
            (let [env (assoc base-lein-env "JBOSS_HOME"  *tmp-jboss-home*)
                  dd (io/file *tmp-deployments-dir* "ham.clj")]
              (run-lein *generation* "immutant" "deploy" "--name" "ham"
                        :dir project-dir
                        :env env)              => 0
              (.exists dd)                     => true
              (:root (read-string (slurp dd))) => (.getAbsolutePath project-dir))))
        
        (fact (str "with a --context-path and --virtual-host should work for lein " *generation*)
          (with-tmp-jboss-home
            (let [env (assoc base-lein-env "JBOSS_HOME"  *tmp-jboss-home*)
                  dd (io/file *tmp-deployments-dir* "test-project.clj")]
              (run-lein *generation* "immutant" "deploy" "--virtual-host" "host" "--context-path" "path"
                        :dir project-dir
                        :env env)      => 0
              (.exists dd)             => true
              (read-string (slurp dd)) => {:root (.getAbsolutePath project-dir)
                                           :context-path "path"
                                           :virtual-host "host"}))))
      
      (facts "not in a project"
        (fact (str "with a path arg should work for lein " *generation*)
          (with-tmp-jboss-home
            (let [env (assoc base-lein-env "JBOSS_HOME" *tmp-jboss-home*)
                  dd (io/file *tmp-deployments-dir* "test-project.clj")]
              (run-lein *generation* "immutant" "deploy" (.getAbsolutePath project-dir)
                        :dir "/tmp"
                        :env env)              => 0
              (.exists dd)                     => true
              (:root (read-string (slurp dd))) => (.getAbsolutePath project-dir))))

        (fact (str "with a non-existent path arg should work for lein " *generation*)
          (with-tmp-jboss-home
            (let [env (assoc base-lein-env "JBOSS_HOME" *tmp-jboss-home*)
                  dd (io/file *tmp-deployments-dir* "test-project.clj")]
              (run-lein *generation* "immutant" "deploy" "/tmp/hAmBisCuit"
                        :dir "/tmp"
                        :env env)              => 1
              (.exists dd)                     => false)))
                
        (fact (str "with a --name arg and a path argshould work for lein " *generation*)
          (with-tmp-jboss-home
            (let [env (assoc base-lein-env "JBOSS_HOME"  *tmp-jboss-home*)
                  dd (io/file *tmp-deployments-dir* "ham.clj")]
              (run-lein *generation* "immutant" "deploy" "--name" "ham" (.getAbsolutePath project-dir)
                        :dir "/tmp"
                        :env env)              => 0
              (.exists dd)                     => true
              (:root (read-string (slurp dd))) => (.getAbsolutePath project-dir))))

        (fact (str "with a --context-path, --virtual-host, and an path arg should work for lein " *generation*)
          (with-tmp-jboss-home
            (let [env (assoc base-lein-env "JBOSS_HOME"  *tmp-jboss-home*)
                  dd (io/file *tmp-deployments-dir* "test-project.clj")]
              (run-lein *generation* "immutant" "deploy"
                        "--virtual-host" "host"
                        "--context-path" "path"
                        (.getAbsolutePath project-dir)
                        :dir "/tmp"
                        :env env)      => 0
              (.exists dd)             => true
              (read-string (slurp dd)) => {:root (.getAbsolutePath project-dir)
                                           :context-path "path"
                                           :virtual-host "host"})))))))

