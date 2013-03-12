(ns leiningen.immutant.install-test
  (:use midje.sweet
        leiningen.immutant.test-helper)
  (:require [clojure.java.io :as io]))

(println "\n==> Testing install")

;; htf are going to do these?
(future-fact "install")
(future-fact "install incremental")
(future-fact "install version")



