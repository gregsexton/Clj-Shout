(ns clout.stream.stream)

(defprotocol Stream
  "An output stream used to stream files to a server. Implementations
  may make use of a Protocol to actually send the data."

  (stream [this bytes]))
