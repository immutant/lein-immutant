(ns leiningen.new.immutant
  "Generate an Immutant project."
  (:use [leiningen.new.templates :only [renderer sanitize year ->files]]))

(defn immutant
  "An Immutant application project layout."
  [name]
  (let [default-render (renderer "default")
        render (renderer "immutant")
        data {:name name
              :sanitized (sanitize name)
              :year (year)}]
    (println "Generating a project called" name "based on the 'immutant' template.")
    (->files data
             ["project.clj" (default-render "project.clj" data)]
             ["README.md" (default-render "README.md" data)]
             [".gitignore" (default-render "gitignore" data)]
             ["src/{{sanitized}}/core.clj" (default-render "core.clj" data)]
             ["test/{{sanitized}}/core_test.clj" (default-render "test.clj" data)]
             ["immutant.clj" (render "immutant.clj" data)])))
