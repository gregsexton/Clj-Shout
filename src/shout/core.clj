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

(defn- byte-seq [mutable-generator]
  (lazy-seq
   (when-let [val (@mutable-generator)]
     (cons val (byte-seq mutable-generator)))))

(defn- send-bytes [stream bytes]
  (with-open [s stream] (stream/write s bytes)))

(defn- reset-current-source!
  "Reset the index of the currently playing source to idx."
  [context idx]
  {:pre [(>= 0 idx)]}
  (reset! (:current-source context) idx)
  context)

(defn- sync-send-playlist [stream playlist idx]
  (letfn [(seq1 [s]
            (lazy-seq
             (when-let [[x] (seq s)]
               (cons x (seq1 (rest s))))))]
    (send-bytes stream (->> playlist
                            deref
                            (drop idx)
                            (map :source)
                            seq1        ;stop source-generator from
                                        ;creating unnecessary input streams
                            (map source-generator)
                            composite-generator
                            atom
                            byte-seq))))

;;; public interface

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
  playlist to a session and maintain control over the sending stream."
  [playlist session]
  {:playlist playlist
   :session session
   :current-source 0})

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

;; (defn send-context
;;   "Begin sending a context's data to the context's session. This will
;;   happen asynchronously and return a new context. If the context is
;;   already streaming it will be stopped first."
;;   ([context]
;;      (send-context @(:current-source context)))
;;   ([context idx]
;;      (when (future? (:future context)) (future-cancel (:future context)))
;;      (assoc context
;;        :future (future (sync-send-playlist (:playlist context)
;;                                            (:session context)
;;                                            (:current-source
;;                                             (reset-current-source! context idx)))))))
