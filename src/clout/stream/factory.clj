(ns clout.stream.factory
  (:require [clout.stream.mp3-stream :as mp3]))

(defmulti create-stream :stream-format)

(defmethod create-stream :mp3 [session protocol]
  (mp3/->Mp3Stream protocol))
