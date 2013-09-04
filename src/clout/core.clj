(ns clout.core
  (:require
   [clout.stream.stream :as stream]
   [clout.stream.factory]
   [clojure.java.io :as io])
  (:import [java.io InputStream]))

(defn create-out-stream [session]
  (stream/create-format-stream
   session
   (stream/create-protocol-stream session)))

;;; todo: implement using chunked sequences? Will this help performance?
;;; (take 1 (map #(do (print \.) %) (range 32)))
;;; (take 1 (map #(do (print \.) %) (create-byte-seq (create-m))))
;;; dorun on this spikes cpu => not i/o bound: room for improvement
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

;;; todo: concatenating here kills performance, which seems not to
;;; grow linearly. Why?
(defn byte-seq [sources]
  (reduce #(concat %1 (create-byte-seq
                       (fn [] (io/input-stream %2))))
          nil sources))
