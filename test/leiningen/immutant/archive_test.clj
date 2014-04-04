(ns leiningen.immutant.archive-test
  (:use midje.sweet
        leiningen.immutant.test-helper)
  (:require [clojure.java.io :as io]))

(setup-test-env)

(println "\n==> Testing archive")

(facts "archive"
  (facts "in a project"
    (fact "with no args should work"
      (with-tmp-dir
        (let [project-dir (copy-resource-to-tmp "test-project")
              archive (io/file project-dir "target/test-project.ima")]
          (run-lein "immutant" "archive"
                    :dir project-dir
                    :env base-lein-env)          => 0
          (.exists archive)                      => true
          (verify-archive archive
                          base-project-archive-contents) => true)))

    (fact "with no args and non-default project settings should work"
      (with-tmp-dir
        (let [project-dir (copy-resource-to-tmp "non-defaults-project")
              archive (io/file project-dir "target/test-project.ima")]
          (run-lein "immutant" "archive"
                    :dir project-dir
                    :env base-lein-env)          => 0
          (.exists archive)                      => true
          (verify-archive
           archive
           (conj base-archive-contents
                 "lib/tools.nrepl-0.2.3.jar"
                 "lib/clojure-complete-0.2.3.jar"
                 "lib/clojure-1.4.0.jar"
                 "schmative/foo.so"
                 "prod-resources/resource2.txt"
                 "schmasses/Foo.class"
                 "schmrc/test_project/ham.clj")) => true)))

    (fact "with no args and no project.clj should work"
      (with-tmp-dir
        (let [project-dir (copy-resource-to-tmp "projectless-project")
              archive (io/file project-dir "projectless-project.ima")]
          (run-lein "immutant" "archive"
                    :dir project-dir
                    :env base-lein-env)    => 0
          (.exists archive)                => true
          (verify-archive
           archive
           (-> base-project-archive-contents
               (disj "project.clj"
                     "lib/tools.nrepl-0.2.3.jar"
                     "lib/clojure-complete-0.2.3.jar"
                     "lib/clojure-1.4.0.jar")
               (conj
                "native/bar.so"
                "classes/Bar.class")))   => true)))
        
    (fact "with non-existent path arg should print a warning"
      (with-tmp-dir
        (let [project-dir (copy-resource-to-tmp "test-project")
              archive (io/file project-dir "test-project.ima")
              result 
              (run-lein "immutant" "archive" "yarg"
                        :dir project-dir
                        :env base-lein-env
                        :return-result? true)]
          (re-find #"Error: path 'yarg' does not exist" (:err result)) =not=> nil
          (:exit result)                   => 1
          (.exists archive)                => false)))
    
    (fact "with a --name arg should work"
      (with-tmp-dir
        (let [project-dir (copy-resource-to-tmp "test-project")
              archive (io/file project-dir "target/blarg.ima")]
          (run-lein "immutant" "archive" "--name" "blarg"
                    :dir project-dir
                    :env base-lein-env)  => 0
          (.exists archive)              => true
          (verify-archive archive
                          base-project-archive-contents) => true)))


    (fact "with options should add them to the internal descriptor"
      (with-tmp-dir
        (let [project-dir (copy-resource-to-tmp "test-project")
              archive (io/file project-dir "target/test-project.ima")]
          (run-lein "immutant" "archive" "--context-path" "/" "--virtual-host" "ham"
                    :dir project-dir
                    :env base-lein-env)  => 0
          (.exists archive)              => true
          (verify-archive
            archive
            base-project-archive-contents)       => true
           (read-string
            (slurp (file-from-archive archive ".immutant.clj"))) => {:context-path "/"
                                                                     :virtual-host "ham"
                                                                     :resolve-dependencies false
                                                                     :resolve-plugin-dependencies false})))

    (fact "plugin deps should be included"
      (with-tmp-dir
        (let [project-dir (copy-resource-to-tmp "test-project")
              archive (io/file project-dir "target/test-project.ima")]
          (run-lein "with-profile" "plugin" "immutant" "archive"
                    :dir project-dir
                    :env base-lein-env)  => 0
          (.exists archive)              => true
          (verify-archive
            archive
            (-> base-project-archive-contents
              (disj
                "lib/tools.nrepl-0.2.3.jar"
                "lib/clojure-complete-0.2.3.jar")
              (conj
                "plugin-deps/immutant-dependency-exclusions-0.1.0.jar")))       => true)))
    
    (fact "excluding dependencies should put nothing in the descriptor"
      (with-tmp-dir
        (let [project-dir (copy-resource-to-tmp "test-project")
              archive (io/file project-dir "target/test-project.ima")]
          (run-lein "immutant" "archive" "--context-path" "/" "--exclude-dependencies"
            :dir project-dir
            :env base-lein-env)  => 0
          (.exists archive)              => true
          (verify-archive
           archive
           (disj base-project-archive-contents
             "lib/tools.nrepl-0.2.3.jar"
             "lib/clojure-complete-0.2.3.jar"
             "lib/clojure-1.4.0.jar"))       => true
           (read-string
             (slurp (file-from-archive archive ".immutant.clj"))) => {:context-path "/"})))
    
    (fact "with profiles should add an internal descriptor"
      (with-tmp-dir
        (let [project-dir (copy-resource-to-tmp "test-project")
              archive (io/file project-dir "target/test-project.ima")]
          (run-lein "with-profile" "ham" "immutant" "archive"
                    :dir project-dir
                    :env base-lein-env)  => 0
          (.exists archive)              => true
          (verify-archive
           archive
           (disj base-project-archive-contents
             "lib/tools.nrepl-0.2.3.jar"
             "lib/clojure-complete-0.2.3.jar"))       => true
           (read-string
             (slurp (file-from-archive archive ".immutant.clj"))) => {:lein-profiles [:ham]
                                                                      :resolve-dependencies false
                                                                      :resolve-plugin-dependencies false})))

    (fact "with profiles and options should add an internal descriptor"
      (with-tmp-dir
        (let [project-dir (copy-resource-to-tmp "test-project")
              archive (io/file project-dir "target/test-project.ima")]
          (run-lein "with-profile" "ham" "immutant" "archive" "--context-path" "biscuit"
                    :dir project-dir
                    :env base-lein-env)  => 0
          (.exists archive)              => true
          (verify-archive
           archive
            (disj base-project-archive-contents
              "lib/tools.nrepl-0.2.3.jar"
              "lib/clojure-complete-0.2.3.jar"))       => true
           (read-string
            (slurp (file-from-archive archive ".immutant.clj"))) => {:lein-profiles [:ham]
                                                                     :context-path "biscuit"
                                                                     :resolve-dependencies false
                                                                     :resolve-plugin-dependencies false})))
        
    (fact "with a --exclude-dependencies arg should work"
      (with-tmp-dir
        (let [project-dir (copy-resource-to-tmp "test-project")
              archive (io/file project-dir "target/test-project.ima")]
          (run-lein "immutant" "archive" "--exclude-dependencies"
                    :dir project-dir
                    :env base-lein-env)    => 0
          (.exists archive)                => true
          (verify-archive
           archive
           (disj base-project-archive-contents
             ".immutant.clj"
             "lib/tools.nrepl-0.2.3.jar"
             "lib/clojure-complete-0.2.3.jar"
             "lib/clojure-1.4.0.jar")) => true)))

    (fact ":omit-source should be honored"
      (with-tmp-dir
        (let [project-dir (copy-resource-to-tmp "jar-options-project")
              archive (io/file project-dir "target/jar-options-project.ima")]
          (run-lein "immutant" "archive"
                    :dir project-dir
                    :env base-lein-env)          => 0
          (.exists archive)                      => true
          (verify-archive archive
                          (-> base-project-archive-contents
                              (disj "src/test_project/core.clj"))) => true)))

    (fact ":jar-exclusions should be honored"
      (with-tmp-dir
        (let [project-dir (copy-resource-to-tmp "jar-options-project")
              archive (io/file project-dir "target/jar-options-project.ima")]
          (run-lein "immutant" "archive"
                    :dir project-dir
                    :env base-lein-env)          => 0
          (.exists archive)                      => true
          (verify-archive archive
                          (disj base-project-archive-contents
                                "src/test_project/core.clj")) => true))))

  
  (facts "not in a project"
    (fact "with a path arg should work"
      (with-tmp-dir
        (let [project-dir (copy-resource-to-tmp "test-project")
              archive (io/file project-dir "target/test-project.ima")]
          (run-lein "immutant" "archive" "test-project"
                    :dir *tmp-dir*
                    :env base-lein-env)    => 0
          (.exists archive)                => true
          (verify-archive archive
                          base-project-archive-contents)   => true)))

    (fact "with a path and non-default project settings should work"
      (with-tmp-dir
        (let [project-dir (copy-resource-to-tmp "non-defaults-project")
              archive (io/file project-dir "target/test-project.ima")]
          (run-lein "immutant" "archive" "non-defaults-project"
                    :dir *tmp-dir*
                    :env base-lein-env)              => 0
          (.exists archive)                => true
          (verify-archive
           archive
           (conj base-archive-contents
                 "lib/tools.nrepl-0.2.3.jar"
                 "lib/clojure-complete-0.2.3.jar"
                 "lib/clojure-1.4.0.jar"
                 "schmative/foo.so"
                 "prod-resources/resource2.txt"
                 "schmasses/Foo.class"
                 "schmrc/test_project/ham.clj"))   => true)))

    (fact "with a path and no project.clj should work"
      (with-tmp-dir
        (let [project-dir (copy-resource-to-tmp "projectless-project")
              archive (io/file project-dir "projectless-project.ima")]
          (run-lein "immutant" "archive" "projectless-project"
                    :dir *tmp-dir*
                    :env base-lein-env)              => 0
          (.exists archive)                => true
          (verify-archive
           archive
           (-> base-project-archive-contents
               (disj
                "project.clj"
                "lib/tools.nrepl-0.2.3.jar"
                "lib/clojure-complete-0.2.3.jar"
                "lib/clojure-1.4.0.jar")
               (conj
                "native/bar.so"
                "classes/Bar.class")))   => true)))
    
    (fact "with a non-existent path arg should work"
      (let [{:keys [err exit]}
            (run-lein "immutant" "archive" "/tmp/hAmBisCuit"
                      :dir "/tmp"
                      :env base-lein-env
                      :return-result? true)]
        exit                                                     => 1
        (re-find #"Error: path '/tmp/hAmBisCuit' does not exist" err) =not=> nil))
    
    (fact "with a --name arg and a path arg should work"
      (with-tmp-dir
        (let [project-dir (copy-resource-to-tmp "test-project")
              archive (io/file project-dir "target/blarg.ima")]
          (run-lein "immutant" "archive" "--name" "blarg" "test-project"
                    :dir *tmp-dir*
                    :env base-lein-env)  => 0
          (.exists archive)              => true
          (verify-archive archive
                          base-project-archive-contents) => true)))

    (fact "with a --name arg, --exclude-dependencies, and a path arg should work"
      (with-tmp-dir
        (let [project-dir (copy-resource-to-tmp "test-project")
              archive (io/file project-dir "target/blarg.ima")]
          (run-lein "immutant" "archive" "--name" "blarg" "--exclude-dependencies" "test-project"
                    :dir *tmp-dir*
                    :env base-lein-env)  => 0
          (.exists archive)              => true
          (verify-archive
           archive
           (disj base-project-archive-contents
             ".immutant.clj"
             "lib/tools.nrepl-0.2.3.jar"
             "lib/clojure-complete-0.2.3.jar"
             "lib/clojure-1.4.0.jar")) => true)))

    (fact "with a path, profiles, and options should add an internal descriptor"
      (with-tmp-dir
        (let [project-dir (copy-resource-to-tmp "test-project")
              archive (io/file project-dir "target/test-project.ima")]
          (run-lein "with-profile" "ham" "immutant" "archive" "--context-path" "biscuit" "test-project"
                    :dir *tmp-dir*
                    :env base-lein-env)  => 0
          (.exists archive)              => true
          (verify-archive
           archive
           (disj base-project-archive-contents
             "lib/tools.nrepl-0.2.3.jar"
             "lib/clojure-complete-0.2.3.jar"))       => true
           (read-string
            (slurp (file-from-archive archive ".immutant.clj"))) => {:lein-profiles [:ham]
                                                                     :context-path "biscuit"
                                                                     :resolve-dependencies false
                                                                     :resolve-plugin-dependencies false})))))

