(ns leiningen.immutant.list
  (:refer-clojure :exclude [list])
  (:require [leiningen.immutant.deploy  :as deploy]
            [leiningen.immutant.install :as install]))

(def list-options
  [["-i" "--installs" :flag true]])

(defn list
  "Lists deployments or Immutant installs

If called with no options, lists deployed applications. If given
--installs (or -i), lists installed versions of immutant instead.

When listing deployments, it will only list disk-based ones, and won't
list anything deployed via the JBoss CLI or management interface."
[opts]
  (if (:installs opts)
    (install/list-installs)
    (deploy/list-deployments)))
