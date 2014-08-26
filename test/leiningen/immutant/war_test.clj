(ns leiningen.immutant.war-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.set :as set]))

(defn war-entry-set [f]
  (with-open [zip (java.util.zip.ZipFile. f)]
    (->> zip
         .entries
         enumeration-seq
         (map (memfn getName))
         set)))

(defn verify-war [f expected]
  (let [a (war-entry-set f)]
    (or (= a expected)
        (println "FAIL archive diff: missing -"
                 (set/difference expected a)
                 "extra -"
                 (set/difference a expected)))))

(defn file-from-war [archive name]
  (with-open [zip (java.util.zip.ZipFile. archive)]
    (let [f (io/file (System/getProperty "user.tmp.dir") name)]
      (when-let [entry (some #(and (= (.getName %) name) %)
                             (enumeration-seq (.entries zip)))]
        (io/copy (.getInputStream zip entry) f)
        f))))

(deftest foo)
