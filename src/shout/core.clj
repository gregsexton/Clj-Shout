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
  source may be anything that clojure.java.io/input-stream can
  take. This sends the source in realtime, i.e. it will take as long
  as the song lasts to send it. This method is synchronous."
  [session source]
  (with-open [stream (create-out-stream session)]
    (stream/write stream (byte-seq source))))

(defn create-playlist
  "Create a playlist. Sources should be a seq of source maps. A source
  map is defined as {:source source :metadata metadata}, where source
  is anything that clojure.java.io/input-stream can take and metadata
  is anything useful that you wish to attach to this item in the
  playlist. The metadata is meant to be useful to the client and will
  be ignored by Shout."
  ([] (create-playlist []))
  ([sources]
     {:pre [(sequential? sources) (every? #(contains? % :source) sources)]}
     (atom (seq sources))))

(defn create-context
  "Create a context. Contexts are used to send multiple sources as a
  playlist to a session and maintain control over sending."
  [playlist session]
  {:playlist playlist
   :session session
   :current-source (atom 0)})

(defn send-context
  "Idempotently begin sending a context's data to the context's
  session. This will happen asynchronously and return a new context."
  [context]
  (with-open [stream (create-out-stream (:session context))]
    (doseq [source (->> context :playlist deref (map :source))]
      (stream/write stream (byte-seq source)))))

(def resume-context
  "Idempotently resume sending the context's data to the context's
  session. This will happen asynchronously and return a new context."
  send-context)

(defn append-to-playlist
  "Append source to playlist."
  [playlist source])
