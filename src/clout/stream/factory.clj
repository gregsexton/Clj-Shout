(ns clout.stream.factory
  (:require [clout.stream.http-ice :as http]
            [clout.stream.mp3-stream :as mp3]
            [clout.stream.stream :refer :all]))

(defmethod create-format-stream :mp3 [session stream]
  (mp3/->DelayedMp3OutStream stream))

(defmethod create-protocol-stream :http [session]
  (http/create-protocol-stream session))
