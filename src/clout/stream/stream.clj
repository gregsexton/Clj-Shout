(ns clout.stream.stream)

(defprotocol OutStream
  "An output stream."

  (write [this bytes])
  (close [this]))

(defmulti create-protocol-stream
  "Create a protocol OutStream given a session."
  :protocol)

(defmulti create-format-stream
  "Create a format OutStream given a session"
  :stream-format)
