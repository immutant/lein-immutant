(ns deploy-tools.test.archive
  (:use [deploy-tools.archive])
  (:use [clojure.test])
  (:require [clojure.java.io :as io])
  (:import [java.util.jar JarFile]))


(defn contains-path? [pathifier entries path]
  (some #{(pathifier path)}
        entries))

(let [app-root (io/file (io/resource "app-root"))
      tmp-dir (io/file "/tmp")]
  (let [contains-file-path? (partial contains-path? #(.getAbsolutePath (io/file app-root %)))]
    (deftest test-entry-points
      (testing "with no project"
        (let [entry-points (entry-points nil (.getAbsolutePath app-root))]
          (are [path] (contains-file-path? entry-points path)
               "lib"
               "src"
               "resources"
               "project.clj"
               "immutant.clj")))

      (testing "with a project"
        (let [entry-points (entry-points {:source-path ["srca" "srcb"]} (.getAbsolutePath app-root))]
          (are [path] (contains-file-path? entry-points path)
               "lib"
               "srca"
               "srcb"
               "resources"
               "project.clj"
               "immutant.clj")
          (is (not (contains-file-path? entry-points "src")))))))

  (deftest test-create
    (testing "the dir name should be used to name the archive with no project"
      (is (= "app-root.ima" (.getName (create nil app-root tmp-dir)))))

    (testing "the name from the project should be used if given"
      (is (= "the-name.ima" (.getName (create {:name "the-name"} app-root tmp-dir)))))

    (testing "the resulting archive should have the proper contents"
      (let [entries (map (memfn getName)
                         (enumeration-seq (.entries (JarFile. (create nil app-root tmp-dir)))))]
        (are [path] (contains-path? (constantly path) entries path)
             "immutant.clj"
             "lib/foo.jar"
             "src/app_root/core.clj")))))
