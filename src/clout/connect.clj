(ns clout.connect
  (:require [clout.http-ice :as http]))

(defmulti create-protocol :protocol)

(defmethod create-protocol :http [session]
  (http/create-protocol session))
