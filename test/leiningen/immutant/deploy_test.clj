(ns leiningen.immutant.deploy-test
  (:use midje.sweet
        leiningen.immutant.test-helper)
  (:require [clojure.java.io :as io]))

;; TODO: deploy --archive

(setup-test-env)

(for-all-generations
  (println "\n==> Testing deploy/undeploy with lein generation" *generation*)

  (let [project-dir (io/file (io/resource "test-project"))]
    
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
              (re-find #"specified a root path of 'yarg'" (:err result)) =not=> nil
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

      (fact (str "profiles should be noticed and written to the dd for lein " *generation*)
          (with-tmp-jboss-home
            (let [env (assoc base-lein-env "JBOSS_HOME"  *tmp-jboss-home*)
                  dd (io/file *tmp-deployments-dir* "test-project.clj")]
              (run-lein *generation* "with-profile" "foo" "immutant" "deploy"
                        :dir project-dir
                        :env env)      => 0
              (.exists dd)             => true
              (let [dd-data (read-string (slurp dd))]
                dd-data                  => {:root (.getAbsolutePath project-dir)
                                             :lein-profiles [:foo]}
                (:lein-profiles dd-data) => vector?))))

      (fact (str "profiles passed via --with-profiles should be in dd, and print a dep warning for lein " *generation*)
          (with-tmp-jboss-home
            (let [env (assoc base-lein-env "JBOSS_HOME"  *tmp-jboss-home*)
                  dd (io/file *tmp-deployments-dir* "test-project.clj")
                  {:keys [out err exit]}
                  (run-lein *generation* "immutant" "deploy" "--lein-profiles" "foo,bar"
                            :dir project-dir
                            :env env
                            :return-result? true)]
              exit => 0
              (re-find #"via --lein-profiles is deprecated" err) =not=> nil
              (re-find #"lein with-profile foo,bar immutant deploy" err) =not=> nil
              (.exists dd)             => true
              (let [dd-data (read-string (slurp dd))]
                dd-data                  => {:root (.getAbsolutePath project-dir)
                                             :lein-profiles ["foo" "bar"]}
                (:lein-profiles dd-data) => vector?))))
            
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
                  {:keys [err exit]}
                  (run-lein *generation* "immutant" "deploy" "/tmp/hAmBisCuit"
                            :dir "/tmp"
                            :env env
                            :return-result? true)]
              exit                                                     => 1
              (re-find #"Error: '/tmp/hAmBisCuit' does not exist" err) =not=> nil)))
                
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
                                           :virtual-host "host"})))))
    (facts "undeploy"
      (facts "in a project"
        (fact (str "with no args should work for lein " *generation*)
          (with-tmp-jboss-home
            (create-tmp-deploy "test-project")
            (let [env (assoc base-lein-env "JBOSS_HOME" *tmp-jboss-home*)]
              (run-lein *generation* "immutant" "undeploy"
                        :dir project-dir
                        :env env)              => 0
              (tmp-deploy-removed? "test-project") => false)))

        (fact (str "with path arg should print a warning for lein " *generation*)
          (with-tmp-jboss-home
            (create-tmp-deploy "test-project")
            (let [env (assoc base-lein-env "JBOSS_HOME" *tmp-jboss-home*)
                  {:keys [err exit]}
                  (run-lein *generation* "immutant" "undeploy" "yarg"
                            :dir project-dir
                            :env env
                            :return-result? true)]
              exit                                             => 0
              (re-find #"specified a root path of 'yarg'" err) =not=> nil
              (tmp-deploy-removed? "test-project")             => false)))
                
        (fact (str "with a --name arg should work for lein " *generation*)
          (with-tmp-jboss-home
            (create-tmp-deploy "ham")
            (let [env (assoc base-lein-env "JBOSS_HOME"  *tmp-jboss-home*)]
              (run-lein *generation* "immutant" "undeploy" "--name" "ham"
                        :dir project-dir
                        :env env)                  => 0
              (tmp-deploy-removed? "test-project") => false))))
            
      (facts "not in a project"
        (fact (str "with a path arg should work for lein " *generation*)
          (with-tmp-jboss-home
            (create-tmp-deploy "test-project")
            (let [env (assoc base-lein-env "JBOSS_HOME" *tmp-jboss-home*)]
              (run-lein *generation* "immutant" "undeploy" (.getAbsolutePath project-dir)
                        :dir "/tmp"
                        :env env)                  => 0
              (tmp-deploy-removed? "test-project") => false)))

        (fact (str "with a non-existent path arg should work for lein " *generation*)
          (with-tmp-jboss-home
            (let [env (assoc base-lein-env "JBOSS_HOME" *tmp-jboss-home*)
                  {:keys [exit err]}
                  (run-lein *generation* "immutant" "undeploy" "/tmp/hAmBisCuit"
                            :dir "/tmp"
                            :env env
                            :return-result? true)]
              exit                                                     => 1
              (re-find #"Error: '/tmp/hAmBisCuit' does not exist" err) =not=> nil)))))))

