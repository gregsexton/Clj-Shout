(ns shout.core
  (:require
   [shout.stream.stream :as stream]
   [shout.stream.factory]
   [clojure.java.io :as io])
  (:import [java.io InputStream]))

(defn- create-out-stream [session]
  (stream/create-format-stream
   session
   (stream/create-protocol-stream session)))

(defn- create-byte-seq-from-stream
  "Create a lazy sequence of bytes from an input stream. Once the
  sequence has been fully realized the source stream is closed."
  [^InputStream stream]
  (lazy-seq
   (let [b (.read stream)]
     (if (= -1 b)
       (do (.close stream) nil)
       (cons b (create-byte-seq-from-stream stream))))))

(defn- create-byte-seq
  "Create a lazy byte seq from an input stream factory function. This
  will be evaluated lazily to allow creating the input stream only on
  demand."
  [f]
  (lazy-seq
   (when-let [stream (f)]
     (create-byte-seq-from-stream stream))))

(defn- byte-seq [source]
  (create-byte-seq (fn [] (io/input-stream source))))

(defn send-source
  "Send a single source to the server as specified by session. Session
  should be created using shout.session/create-shout-session. The
  source may be anything that clojure.java.io/input-stream can take."
  [session source]
  (with-open [stream (create-out-stream session)]
    (stream/write stream (byte-seq source))))
