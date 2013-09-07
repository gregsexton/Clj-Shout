(ns shout.stream.factory
  (:require [shout.stream.http-ice :as http]
            [shout.stream.mp3-stream :as mp3]
            [shout.stream.stream :refer :all]))

(defmethod create-format-stream :mp3 [session stream]
  (mp3/->RealTimeMp3OutStream stream))

(defmethod create-protocol-stream :http [session]
  (http/create-protocol-stream session))
