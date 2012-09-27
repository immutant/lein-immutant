(ns leiningen.immutant.env
  (:use leiningen.immutant.common))

(defn env-var-set? [var-name]
  (if (not (nil? (System/getenv var-name)))
    var-name))

(def the-env 
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
  "Displays paths to the Immutant that the plugin can find"
  ([]
     (doall
      (for [[key entry] the-env]
        (display-entry key entry))))
  ([key]
     (if (contains? the-env key)
       (when-let [{:keys [value]} (the-env key)]
         (print (.getAbsolutePath value))
         (flush))
       (binding [*out* *err*]
         (println key "is an unknown env key. Valid keys are:" (keys the-env))))))
