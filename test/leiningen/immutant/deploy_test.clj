(ns leiningen.immutant.deploy-test
  (:use midje.sweet
        leiningen.immutant.test-helper)
  (:require [clojure.java.io :as io]))

(setup-test-env)

(println "\n==> Testing deploy/undeploy")

(defn setup-env-dir-dd
  ([]
     (setup-env-dir-dd "test-project"))
  ([project]
     (setup-env-dir-dd project project))
  ([project name]
     {:env (assoc base-lein-env "JBOSS_HOME" *tmp-jboss-home*)
      :project-dir (copy-resource-to-tmp project)
      :dd (io/file *tmp-deployments-dir* (str name ".clj"))
      :archive (io/file *tmp-deployments-dir* (str name ".ima"))}))

(defn verify-root [dd expected]
  (= (-> dd slurp read-string :root io/file .getCanonicalPath)
     (.getCanonicalPath expected)))

(facts "deploy"
  (facts "in a project"
    (fact "with no args should work"
      (with-tmp-jboss-home
        (let [{:keys [env project-dir dd]} (setup-env-dir-dd)]
          (run-lein "immutant" "deploy"
                    :dir project-dir
                    :env env)              => 0
          (.exists dd)                     => true
          (verify-root dd project-dir)     => true)))

    (fact "with non-existent path arg should print a warning"
      (with-tmp-jboss-home
        (let [{:keys [env project-dir dd]} (setup-env-dir-dd)
              result 
              (run-lein "immutant" "deploy" "yarg"
                        :dir project-dir
                        :env env
                        :return-result? true)]
          (re-find #"Error: path 'yarg' does not exist" (:err result)) =not=> nil
          (:exit result)                   => 1
          (.exists dd)                     => false)))

    (fact "with a path arg to a different app"
      (with-tmp-jboss-home
        (let [{:keys [env project-dir dd]} (setup-env-dir-dd)
              other-project (setup-env-dir-dd "jar-options-project")]
          (run-lein "immutant" "deploy" (.getAbsolutePath project-dir)
                    :dir (:project-dir other-project)
                    :env env)                  => 0
          (.exists dd) => true
          (.exists (:dd other-project)) => false)))
        
    (fact "with a --name arg should work"
      (with-tmp-jboss-home
        (let [{:keys [env project-dir dd]} (setup-env-dir-dd "test-project" "ham")]
          (run-lein "immutant" "deploy" "--name" "ham"
                    :dir project-dir
                    :env env)              => 0
          (.exists dd)                     => true
          (verify-root dd project-dir)     => true)))
    
    (fact "with a --context-path and --virtual-host should work"
      (with-tmp-jboss-home
        (let [{:keys [env project-dir dd]} (setup-env-dir-dd)]
          (run-lein "immutant" "deploy" "--virtual-host" "host" "--context-path" "path"
                    :dir project-dir
                    :env env)      => 0
          (.exists dd)             => true
          (verify-root dd project-dir)     => true
          (read-string (slurp dd)) => (contains
                                       {:context-path "path"
                                        :virtual-host "host"}))))

    (fact "profiles should be noticed and written to the dd"
      (with-tmp-jboss-home
        (let [{:keys [env project-dir dd]} (setup-env-dir-dd)]
          (run-lein "with-profile" "foo" "immutant" "deploy"
                    :dir project-dir
                    :env env)      => 0
          (.exists dd)             => true
          (verify-root dd project-dir)     => true
          (let [profiles (:lein-profiles (read-string (slurp dd)))]
            profiles => [:foo]
            profiles => vector?))))

    (fact "profiles should be noticed and written to the dd when switching context"
      (with-tmp-jboss-home
        (let [{:keys [env project-dir dd]} (setup-env-dir-dd)
              other-project (setup-env-dir-dd "jar-options-project")]
          (run-lein "with-profile" "foo" "immutant" "deploy" (.getAbsolutePath project-dir)
                    :dir (:project-dir other-project)
                    :env env)      => 0
          (.exists dd)             => true
          (verify-root dd project-dir)     => true
          (let [profiles (:lein-profiles (read-string (slurp dd)))]
            profiles => [:foo]
            profiles => vector?))))
        
    (fact "profiles passed via --with-profiles should be in dd, and print a dep warning"
      (with-tmp-jboss-home
        (let [{:keys [env project-dir dd]} (setup-env-dir-dd)
              {:keys [out err exit]}
              (run-lein "immutant" "deploy" "--lein-profiles" "foo,bar"
                        :dir project-dir
                        :env env
                        :return-result? true)]
          exit => 0
          (re-find #"via --lein-profiles is deprecated" err) =not=> nil
          (re-find #"lein with-profile foo,bar immutant deploy" err) =not=> nil
          (.exists dd)             => true
          (verify-root dd project-dir) => true
          (let [profiles (:lein-profiles (read-string (slurp dd)))]
            profiles => ["foo" "bar"]
            profiles => vector?))))

    (fact "--archive should work"
      (with-tmp-jboss-home
        (let [{:keys [env project-dir archive]} (setup-env-dir-dd)]
          (run-lein "immutant" "deploy" "--archive"
                    :dir project-dir
                    :env env)                    => 0
          (.exists archive)                      => true
          (verify-archive archive
                          base-project-archive-contents) => true)))

    (fact "--archive with jar options should work"
      (with-tmp-jboss-home
        (let [{:keys [env project-dir archive]} (setup-env-dir-dd "jar-options-project")]
          (run-lein "immutant" "deploy" "--archive" 
                    :dir project-dir
                    :env env)                    => 0
          (.exists archive)                      => true
          (verify-archive archive
                          (disj base-project-archive-contents
                                "src/test_project/core.clj")) => true)))
        
    (fact "--archive with --name should work"
      (with-tmp-jboss-home
        (let [{:keys [env project-dir archive]} (setup-env-dir-dd "test-project" "bam")]
          (run-lein "immutant" "deploy" "--archive" "--name" "bam"
                    :dir project-dir
                    :env env)                    => 0
          (.exists archive)                      => true
          (verify-archive archive
                          base-project-archive-contents) => true)))
    
    (fact "--archive with options should work"
      (with-tmp-jboss-home
        (let [{:keys [env project-dir archive]} (setup-env-dir-dd)]
          (run-lein "immutant" "deploy" "--archive" "--context-path" "ham"
                    :dir project-dir
                    :env env)                    => 0
          (.exists archive)                      => true
          (verify-archive archive
                          (conj
                           base-project-archive-contents
                           ".immutant.clj")) => true
          (read-string
           (slurp (file-from-archive archive ".immutant.clj"))) => {:context-path "ham"})))

    (fact "--archive with options and profiles should work"
      (with-tmp-jboss-home
        (let [{:keys [env project-dir archive]} (setup-env-dir-dd)]
          (run-lein "with-profile" "biscuit" "immutant" "deploy" "--archive" "--context-path" "ham"
                    :dir project-dir
                    :env env)                    => 0
          (.exists archive)                      => true
          (verify-archive archive
                          (conj
                           base-project-archive-contents
                           ".immutant.clj")) => true
          (read-string
           (slurp (file-from-archive archive ".immutant.clj"))) => {:lein-profiles [:biscuit]
                                                                    :context-path "ham"}))))
  
  (facts "not in a project"
    (fact "with a path arg should work"
      (with-tmp-jboss-home
        (let [{:keys [env project-dir dd]} (setup-env-dir-dd)]
          (run-lein "immutant" "deploy" (.getAbsolutePath project-dir)
                    :dir *tmp-dir*
                    :env env)              => 0
          (.exists dd)                 => true
          (verify-root dd project-dir) => true)))

    (fact "with a non-existent path arg should work"
      (with-tmp-jboss-home
        (let [{:keys [env]} (setup-env-dir-dd)
              {:keys [err exit]}
              (run-lein "immutant" "deploy" "/tmp/hAmBisCuit"
                        :dir *tmp-dir*
                        :env env
                        :return-result? true)]
          exit                                                     => 1
          (re-find #"Error: path '/tmp/hAmBisCuit' does not exist" err) =not=> nil)))
    
    (fact "with a --name arg and a path arg should work"
      (with-tmp-jboss-home
        (let [{:keys [env project-dir dd]} (setup-env-dir-dd "test-project" "ham")]
          (run-lein "immutant" "deploy" "--name" "ham" (.getAbsolutePath project-dir)
                    :dir *tmp-dir*
                    :env env)              => 0
          (.exists dd)                     => true
          (verify-root dd project-dir) => true)))

    (fact "with a --context-path, --virtual-host, and an path arg should work"
      (with-tmp-jboss-home
        (let [{:keys [env project-dir dd]} (setup-env-dir-dd)]
          (run-lein "immutant" "deploy"
                    "--virtual-host" "host"
                    "--context-path" "path"
                    (.getAbsolutePath project-dir)
                    :dir *tmp-dir*
                    :env env)      => 0
          (.exists dd)             => true
          (verify-root dd project-dir) => true
          (read-string (slurp dd)) => (contains
                                       {:context-path "path"
                                        :virtual-host "host"}))))

    (fact "profiles should be noticed and written to the dd"
      (with-tmp-jboss-home
        (let [{:keys [env project-dir dd]} (setup-env-dir-dd)]
          (run-lein "with-profile" "foo" "immutant" "deploy" (.getAbsolutePath project-dir)
                    :dir *tmp-dir*
                    :env env)      => 0
                    (.exists dd)             => true
          (verify-root dd project-dir) => true          
          (let [profiles (:lein-profiles (read-string (slurp dd)))]
            profiles => [:foo]
            profiles => vector?))))


    (fact "--archive should work"
      (with-tmp-jboss-home
        (let [{:keys [env project-dir archive]} (setup-env-dir-dd)]
          (run-lein "immutant" "deploy" "--archive" (.getAbsolutePath project-dir)
                    :dir *tmp-dir*
                    :env env)                    => 0
          (.exists archive)                      => true
          (verify-archive archive
                          base-project-archive-contents) => true)))
        
    (fact "--archive with --name should work"
      (with-tmp-jboss-home
        (let [{:keys [env project-dir archive]} (setup-env-dir-dd "test-project" "bam")]
          (run-lein "immutant" "deploy" "--archive" "--name" "bam" (.getAbsolutePath project-dir)
                    :dir project-dir
                    :env env)                    => 0
          (.exists archive)                      => true
          (verify-archive archive
                          base-project-archive-contents) => true)))
    
    (fact "--archive with options should work"
      (with-tmp-jboss-home
          (let [{:keys [env project-dir archive]} (setup-env-dir-dd)]
            (run-lein "immutant" "deploy" "--archive" "--context-path" "ham" (.getAbsolutePath project-dir)
                      :dir *tmp-dir*
                      :env env)                    => 0
            (.exists archive)                      => true
            (verify-archive archive
                            (conj
                             base-project-archive-contents
                             ".immutant.clj")) => true
            (read-string
             (slurp (file-from-archive archive ".immutant.clj"))) => {:context-path "ham"})))

    (fact "--archive with options and profiles should work"
      (with-tmp-jboss-home
        (let [{:keys [env project-dir archive]} (setup-env-dir-dd)]
          (run-lein "with-profile" "biscuit" "immutant" "deploy" "--archive" "--context-path" "ham" (.getAbsolutePath project-dir)
                    :dir *tmp-dir*
                    :env env)                    => 0
                    (.exists archive)                      => true
          (verify-archive archive
                          (conj
                           base-project-archive-contents
                           ".immutant.clj")) => true
          (read-string
           (slurp (file-from-archive archive ".immutant.clj"))) => {:lein-profiles [:biscuit]
                                                                    :context-path "ham"})))))

(facts "undeploy"
  (facts "in a project"
    (fact "with no args should work"
      (with-tmp-jboss-home
        (create-tmp-deploy "test-project")
        (let [{:keys [env project-dir]} (setup-env-dir-dd)]
          (run-lein "immutant" "undeploy"
                    :dir project-dir
                    :env env)              => 0
          (tmp-deploy-removed? "test-project") => true)))

    (fact "with a path arg to a different app"
      (with-tmp-jboss-home
        (create-tmp-deploy "test-project")
        (create-tmp-deploy "jar-options-project")
        (let [{:keys [env project-dir]} (setup-env-dir-dd)
              other-project (setup-env-dir-dd "jar-options-project")]
          (run-lein "immutant" "undeploy" (.getAbsolutePath (:project-dir other-project))
                    :dir project-dir
                    :env env)                  => 0
          (tmp-deploy-removed? "test-project") => false
          (tmp-deploy-removed? "jar-options-project") => true)))
        
    (fact "with no args should work for an archive"
      (with-tmp-jboss-home
        (create-tmp-deploy "test-project" ".ima")
        (let [{:keys [env project-dir]} (setup-env-dir-dd)]
          (run-lein "immutant" "undeploy"
                    :dir project-dir
                    :env env)              => 0
          (tmp-deploy-removed? "test-project" ".ima") => true)))

    (facts "with a name argument or glob"
      (let [deployments #{"ham" "hama-blam" "foo" "food" "bar"}]
        (doseq [[arg undeployed]
                {"ham" ["ham"]
                 "ham*" ["ham" "hama-blam"]
                 "foo?" ["foo" "food"]
                 "foo.clj" ["foo"]}]
          (fact (format "with %s" arg)
            (with-tmp-jboss-home
              (doseq [d deployments]
                (create-tmp-deploy d))
              (let [{:keys [env project-dir]} (setup-env-dir-dd)]
                (run-lein "immutant" "undeploy" arg
                          :dir project-dir
                          :env env)              => 0
                (doseq [d undeployed]
                  (fact
                    (tmp-deploy-removed? d) => true))
                (doseq [d (apply disj deployments undeployed)]
                  (fact
                    (tmp-deploy-removed? d) => false)))))))))
  
  (facts "not in a project"
    (fact "with a path arg should work"
      (with-tmp-jboss-home
        (create-tmp-deploy "test-project")
        (let [{:keys [env project-dir]} (setup-env-dir-dd)]
          (run-lein "immutant" "undeploy" (.getAbsolutePath project-dir)
                    :dir *tmp-dir*
                    :env env)                  => 0
          (tmp-deploy-removed? "test-project") => true)))

    (fact "with a non-existent path arg should work"
      (with-tmp-jboss-home
        (let [{:keys [env]} (setup-env-dir-dd)
              {:keys [exit err]}
              (run-lein "immutant" "undeploy" "/tmp/hAmBisCuit"
                        :dir *tmp-dir*
                        :env env
                        :return-result? true)]
          exit                                                     => 1
          (re-find #"Error: path '/tmp/hAmBisCuit' does not exist" err) =not=> nil)))))

