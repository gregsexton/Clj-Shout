(ns clout.stream.mp3-stream
  (:require [clout.stream.stream :refer [OutStream]]))

(deftype Mp3OutStream [stream]
  OutStream

  (write [this bytes])
  (close [this]))
