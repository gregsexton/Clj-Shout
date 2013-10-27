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

(defn- input-stream-generator
  [^InputStream stream]
  (fn []
    (let [b (.read stream)]
      (if (= -1 b)
        (do (.close stream) nil)
        b))))

(defn- source-generator [source]
  (-> source
      io/input-stream
      input-stream-generator))

(defn- composite-generator [generators]
  (let [generators (ref generators)]
    (fn []
      (dosync
       (when-let [gen (first @generators)]
         (if-let [val (gen)]
           val
           (do (alter generators rest)
               (recur))))))))

(defn- playlist-generator [playlist idx]
  (->> playlist
       (drop idx)
       (map :source)
       (map source-generator)
       composite-generator))

(defn- byte-seq [mutable-generator]
  (lazy-seq
   (when-let [val (@mutable-generator)]
     (cons val (byte-seq mutable-generator)))))

(defn- send-bytes [stream bytes]
  (with-open [s stream] (stream/write s bytes)))

;;; public interface

(defn create-context
  "Create a context. Contexts are used to send multiple sources as a
  playlist to a session and maintain control over the sending
  stream. Session should be created using
  shout.session/create-shout-session. Playlist should be created using
  shout.playlist/create-playlist."
  [playlist session]
  {:playlist playlist
   :session session})

(defn send-source
  "Send a single source to the server as specified by session. Session
  should be created using shout.session/create-shout-session. The
  source may be anything that clojure.java.io/input-stream can
  take. This sends the source in realtime, i.e. it will take as long
  as the song lasts to send it. This method is synchronous."
  [session source]
  (send-bytes (create-out-stream session) (-> source
                                              io/input-stream
                                              input-stream-generator
                                              atom
                                              byte-seq)))

(defn send-context
  "Begin sending a context's data to the server as specified by the
  context's session. This will happen asynchronously and return a new
  context. If the context is already streaming it will immediately
  begin streaming the new data. idx is the index in to the playlist to
  begin streaming from."
  ([context]
     (send-context context 0))
  ([context idx]
     (letfn [(currently-streaming? [{:keys [future]}]
               (every? #(% future) [future? (complement future-done?)]))]
       (let [gen (playlist-generator (:playlist context) idx)]
         (if (currently-streaming? context)
           (do (reset! (:generator-atom context) gen)
               context)
           (let [mutable-gen (atom gen)]
             (assoc context
               :generator-atom mutable-gen
               :future (future (send-bytes (create-out-stream (:session context))
                                           (byte-seq mutable-gen))))))))))
