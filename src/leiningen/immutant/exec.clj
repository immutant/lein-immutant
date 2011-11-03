(ns leiningen.immutant.exec
    (:import (org.apache.commons.exec CommandLine DefaultExecuteResultHandler DefaultExecutor PumpStreamHandler)
             (java.io PipedInputStream PipedOutputStream))
    (:require [clojure.java.io :as io]))

(defn exec [command]
  (with-open [output-stream  (PipedOutputStream.)
              input-stream (PipedInputStream. output-stream)]
    (.execute (doto (DefaultExecutor.)
                (.setExitValue 0)
                (.setStreamHandler (PumpStreamHandler. output-stream)))
              (CommandLine/parse command)
              (DefaultExecuteResultHandler.))
    (doall (map println
                (line-seq
                 (io/reader input-stream))))))
