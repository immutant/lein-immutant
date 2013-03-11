(ns leiningen.new.immutant
  "Generate an Immutant project."
  (:require [leiningen.new.templates :as templ]))

(defn immutant
  "An Immutant application project layout."
  [name]
  (let [default-render (templ/renderer "default")
        render (templ/renderer "immutant")
        main-ns (templ/multi-segment (templ/sanitize-ns name))
        data {:raw-name name
              :name (templ/project-name name)
              :namespace main-ns
              :nested-dirs (templ/name-to-path main-ns)
              :year (templ/year)}]
    (println "Generating a project called" name "based on the 'immutant' template.")
    (templ/->files data
             ["project.clj" (default-render "project.clj" data)]
             ["README.md" (default-render "README.md" data)]
             ["doc/intro.md" (default-render "intro.md" data)]
             [".gitignore" (default-render "gitignore" data)]
             ["src/{{nested-dirs}}.clj" (default-render "core.clj" data)]
             ["test/{{nested-dirs}}_test.clj" (default-render "test.clj" data)]
             ["src/immutant/init.clj" (render "init.tmpl" data)])))
