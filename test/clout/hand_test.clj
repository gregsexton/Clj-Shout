;;; currently only for hand testing
;;; do not check this in!

(ns clout.hand-test
  (:require [clout.proto.factory :as conn]
            [clout.session :refer :all]
            [clout.proto.protocol :as proto]))

(def session (create-clout-session
              (with-host "localhost")
              (with-port 8000)
              (with-password "music")
              (with-protocol :http)))

(def bad-pass-session (create-clout-session
                       (with-host "localhost")
                       (with-port 8000)
                       (with-password "")
                       (with-protocol :http)))

(defn create-protocol [session]
  (conn/create-protocol session))

(defn test-connection []
  (let [p (create-protocol session)]
    (proto/connect p)))

(defn test-bad-conn []
  (let [p (create-protocol bad-pass-session)]
    (proto/connect p)))
