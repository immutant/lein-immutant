(defproject lein-immutant "2.0.1-SNAPSHOT"
  :description "Leiningen plugin for managing an Immutant project."
  :url "https://github.com/immutant/lein-immutant"
  :mailing-list {:name "Immutant users list"
                 :post "immutant-users@immutant.org"
                 :subscribe "immutant-users-subscribe@immutant.org"
                 :unsubscribe "immutant-users-unsubscribe@immutant.org"}
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :dependencies [[org.clojure/tools.cli "0.3.1"]
                 [org.immutant/deploy-tools "2.0.4"
                  :exclusions [leiningen-core]]
                 [org.immutant/fntest "2.0.4"]]
  :exclusions [org.clojure/clojure]
  :eval-in-leiningen true
  :pedantic :warn
  :signing {:gpg-key "BFC757F9"}
  :lein-release {:deploy-via :clojars}
  :min-lein-version "2.4.0")
