(ns shout.playlist)

;;; Playlists must be mutable as they are closed over and read on a
;;; separate thread. Stopping and starting threads will disconnect
;;; clients, so changes in state must be done on the fly.

;;; This is implemented as cons cells wrapped in refs for coordination
;;; purposes. Ref dereferencing is handled transparently. Consing (the
;;; result of conj) mutably appends (O(n)). If this turns out to be
;;; too inefficient maybe a rope would make a better datastructure?
;;; This is purposely not chunked so as not to create extra
;;; input-streams unnecessarily.

(declare ->MutableList)

(defn- empty-mutable-list []
  (->MutableList (ref nil)))

(deftype MutableList [head]
  clojure.lang.Sequential
  clojure.lang.ISeq
  (first [_] (first (deref head)))
  (next [_] (next (deref head)))
  (more [_] (rest (deref head)))
  (cons [this x]
    (letfn [(last-ref-cons [coll]
              (if (seq coll)
                (recur (rest coll))
                (.head coll)))]
      (dosync
       (let [last-ref-cons (last-ref-cons this)]
         (ref-set last-ref-cons (MutableList. (ref (cons x (empty this)))))
         this))))
  (count [_] (count (deref head)))
  (empty [_] (empty-mutable-list))
  (equiv [this x] (identical? this x))
  (seq [this] (when (deref head) this)))

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
     (reduce conj (empty-mutable-list) sources)))

(defn append-source!
  "Append a source to a playlist. A source map is defined as {:source
  source :metadata metadata}, where source is anything that
  clojure.java.io/input-stream can take and metadata is anything
  useful that you wish to attach to this item in the playlist."
  [playlist source]
  {:pre [(instance? MutableList playlist) (contains? source :source)]}
  (conj playlist source))
