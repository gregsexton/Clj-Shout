(ns clout.proto.factory
  (:require [clout.proto.http-ice :as http]))

(defmulti create-protocol :protocol)

(defmethod create-protocol :http [session]
  (http/create-protocol session))
