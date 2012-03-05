(ns immutant.deploy-tools.test.deploy
  (:use [immutant.deploy-tools.deploy])
  (:use [clojure.test])
  (:require [clojure.java.io :as io]
            [immutant.deploy-tools.util :as util])
  (:import [java.util.jar JarFile]))

(def ^{:dynamic true} *mock-jboss-home*)
(def ^{:dynamic true} *deployments-dir*)

(use-fixtures :each
              (fn [f]
                (binding [*mock-jboss-home* (io/file "/tmp/test-deployments")]
                  (binding [*deployments-dir* (doto (io/file *mock-jboss-home* "standalone/deployments")
                                                (.mkdirs))]
                    (f)
                    (doseq [file (file-seq *deployments-dir*)]
                      (io/delete-file file true))))))

(let [app-root (io/file (io/resource "app-root"))]
  (deftest test-make-descriptor
    (is (= (str "{:root \"" (.getAbsolutePath app-root) "\"}\n")
           (make-descriptor app-root))))

  (deftest test-deploy-dir-with-a-project
    (let [descriptor (deploy-dir *mock-jboss-home* {:name "ham-biscuit"} app-root)
          expected-descriptor (io/file *deployments-dir* "ham-biscuit.clj")]
      (is (= "ham-biscuit.clj" (.getName descriptor)))
      (is (= expected-descriptor descriptor))
      (is (.exists expected-descriptor))
      (is (.exists (io/file *deployments-dir* "ham-biscuit.clj.dodeploy")))))

  (deftest test-deploy-dir-without-a-project
    (let [descriptor (deploy-dir *mock-jboss-home* nil app-root)
          expected-descriptor (io/file *deployments-dir* "app-root.clj")]
      (is (= "app-root.clj" (.getName descriptor)))
      (is (= expected-descriptor descriptor))
      (is (.exists expected-descriptor))
      (is (.exists (io/file *deployments-dir* "app-root.clj.dodeploy")))))

  (deftest test-deploy-dir-should-remove-failed-markers
    (let [failed-clj (util/failed-marker (io/file *deployments-dir* "app-root.clj"))
          failed-ima (util/failed-marker (io/file *deployments-dir* "app-root.ima"))]
      (spit failed-clj "")
      (spit failed-ima "")
      (deploy-dir *mock-jboss-home* nil app-root)
      (is (not (.exists failed-clj)))
      (is (not (.exists failed-ima)))))
  
  (deftest test-deploy-archive-with-a-project
    (let [descriptor (deploy-archive *mock-jboss-home* {:name "ham-biscuit"} app-root)
          expected-descriptor (io/file *deployments-dir* "ham-biscuit.ima")]
      (is (= "ham-biscuit.ima" (.getName descriptor)))
      (is (= expected-descriptor descriptor))
      (is (.exists expected-descriptor))
      (is (.exists (io/file *deployments-dir* "ham-biscuit.ima.dodeploy")))))

  (deftest test-deploy-archive-without-a-project
    (let [descriptor (deploy-archive *mock-jboss-home* nil app-root)
          expected-descriptor (io/file *deployments-dir* "app-root.ima")]
      (is (= "app-root.ima" (.getName descriptor)))
      (is (= expected-descriptor descriptor))
      (is (.exists expected-descriptor))
      (is (.exists (io/file *deployments-dir* "app-root.ima.dodeploy")))))

 (deftest test-deploy-dir-should-remove-failed-markers
    (let [failed-clj (util/failed-marker (io/file *deployments-dir* "app-root.clj"))
          failed-ima (util/failed-marker (io/file *deployments-dir* "app-root.ima"))]
      (spit failed-clj "")
      (spit failed-ima "")
      (deploy-archive *mock-jboss-home* nil app-root)
      (is (not (.exists failed-clj)))
      (is (not (.exists failed-ima)))))

  (deftest test-undeploy
    (let [deployed-file (deploy-archive *mock-jboss-home* {:name "gravy"} app-root)
          dodeploy-marker (util/dodeploy-marker deployed-file)]
      (is (.exists deployed-file))
      (is (.exists dodeploy-marker))
      (is (= true (undeploy *mock-jboss-home* {:name "gravy"} app-root)))
      (is (not (.exists deployed-file)))
      (is (not (.exists dodeploy-marker)))))

  (deftest undeploy-returns-nil-if-nothing-was-done
    (is (= nil (undeploy *mock-jboss-home* {:name "gravy"} app-root)))))
