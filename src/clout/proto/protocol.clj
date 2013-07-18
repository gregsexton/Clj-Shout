(ns clout.proto.protocol)

(defprotocol Protocol
  "Represents a protocol used to transfer data to an icecast server."
  (connect [this])
  (stream [this data])
  (close [this]))                       ;TODO: make this autocloseable?
