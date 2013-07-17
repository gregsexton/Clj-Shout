(ns clout.protocol)

(defprotocol Protocol
  "Represents a protocol used to transfer data to an icecast server."
  (connect [this]))
