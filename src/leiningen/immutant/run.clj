(ns leiningen.immutant.run
  (:use leiningen.immutant.common
        leiningen.immutant.exec))

(defn run [project]
  "Launches Immutant and displays its console output"
  (with-jboss-home
    (if-not (.exists (descriptor-file project))
      (err "WARNING: The current app is not deployed - deploy with 'lein immutant deploy'"))
    (let [script (str (.getAbsolutePath *jboss-home*) "/bin/standalone.sh")]
      (println "Starting Immutant via" script)
      (exec script))))

