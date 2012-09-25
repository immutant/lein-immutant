(defproject lein-immutant "0.12.0-SNAPSHOT"
  :description "Leiningen plugin for managing an Immutant project."
  :url "https://github.com/immutant/lein-immutant"
  :mailing-list {:name "Immutant users list"
                 :post "immutant-users@immutant.org"
                 :subscribe "immutant-users-subscribe@immutant.org"
                 :unsubscribe "immutant-users-unsubscribe@immutant.org"}
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :eval-in-leiningen true
  :dependencies [[org.immutant/overlay      "1.2.2"]
                 [org.immutant/deploy-tools "0.9.2"]
                 [jboss-as-management       "0.1.1"]
                 [leinjacker                "0.2.0"]
                 [org.clojure/data.json     "0.1.1"]
                 [org.clojure/tools.cli     "0.2.1"]])
