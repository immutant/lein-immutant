(ns deploy-tools.deploy
  (:use deploy-tools.common)
  (:require [clojure.java.io      :as io]
            [deploy-tools.archive :as archive]))

(defn make-descriptor [root-dir]
  (prn-str {:root (.getAbsolutePath root-dir)}))

(defn deploy-archive [jboss-home project root-dir]
  (with-jboss-home jboss-home
    (let [archive-name (archive-name project root-dir)
          archive-file (io/file root-dir archive-name)
          deployed-file (deployment-file archive-name)]
      (if (.exists archive-file)
        (println archive-name "already exists, skipping archive step.")
        (archive/create project root-dir root-dir))
      (io/copy archive-file deployed-file)
      (spit (dodeploy-marker deployed-file) "")
      deployed-file)))

(defn deploy-dir [jboss-home project root-dir]
  (with-jboss-home jboss-home
    (let [deployed-file (deployment-file (descriptor-name project root-dir))]
      (spit deployed-file (make-descriptor root-dir))
      (spit (dodeploy-marker deployed-file) "")
      deployed-file)))

(defn undeploy
  [jboss-home project root-dir]
  (with-jboss-home jboss-home
    (let [files (filter #(.exists %)
                        (mapcat #(map % [(deployment-file (descriptor-name project root-dir))
                                         (deployment-file (archive-name project root-dir))])
                                [identity
                                 dodeploy-marker
                                 deployed-marker]))]
      (when-not (empty? files)
        (doseq [file files]
          (io/delete-file file))
        true))))
