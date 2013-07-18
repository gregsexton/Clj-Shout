(ns clout.stream.mp3-stream
  (:require [clout.stream.stream :refer [Stream]]))

(deftype Mp3Stream [protocol]
  Stream

  (stream [this bytes]))
