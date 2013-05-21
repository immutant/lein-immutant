(ns leiningen.immutant.env
  (:use leiningen.immutant.common))

(defn env-var-set? [var-name]
  (if (not (nil? (System/getenv var-name)))
    var-name))

(defn get-env []
  (array-map "immutant-home" {:value (get-immutant-home)
                             :from-env-var (env-var-set? "IMMUTANT_HOME") }
             "jboss-home" {:value (get-jboss-home)
                          :from-env-var (env-var-set? "JBOSS_HOME")}))

(defn display-entry [key {:keys [value from-env-var]}]
  (println (format "%14s: %s %s"
                   key
                   (if value value "NOT FOUND")
                   (if from-env-var
                     (str "(from: $" from-env-var ")")
                     ""))))

(defn env
  "Displays paths to the Immutant that the plugin is currently using

If called with no arguments, it will print out the immutant-home and
jboss-home values it is using:

  $ lein immutant env
   immutant-home: /home/hambone/.lein/immutant/current 
      jboss-home: /home/hambone/.lein/immutant/current/jboss 

If given a key argument (one of 'immutant-home' or 'jboss-home'), it
prints out just that path, suitable for usage in a shell script:

  $ lein immutant env immutant-home
  /home/hambone/.lein/immutant/current"
  ([]
     (doall
      (for [[key entry] (get-env)]
        (display-entry key entry))))
  ([key]
     (let [e (get-env)]
       (if (contains? e key)
         (when-let [{:keys [value]} (e key)]
           (print (.getAbsolutePath value))
           (flush))
         (binding [*out* *err*]
           (println key "is an unknown env key. Valid keys are:" (keys e)))))))
