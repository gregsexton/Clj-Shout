(ns shout.playlist)

;;; Playlists must be mutable as they are closed over and read on a
;;; separate thread. Stopping and starting threads will disconnect
;;; clients, so changes in state must be done mutably.

(defn create-playlist
  "Create a mutable thread-safe (can be modified concurrently and
  should always be consistent) playlist. Sources should be a seq of
  anything that clojure.java.io/input-stream can take."
  ([] (create-playlist []))
  ([sources]
     {:pre [(sequential? sources)]}
     (reduce #(when (.add %1 %2) %1)
             (java.util.ArrayList.) sources)))

(defmacro with-playlist
  "Anaphoric macro (binds playlist to it) that ensures locking on the
  playlist."
  [playlist & body]
  `(locking ~playlist
     (let [~'it ~playlist]
       ~@body)))

(defn append-source!
  "Append a source to a playlist. A source is anything that
  clojure.java.io/input-stream can take. Returns the modified
  playlist."
  [playlist source]
  (with-playlist playlist
    (when (.add it source) it)))

(defn insert-source!
  "Insert a source in to a playlist. A source is anything that
  clojure.java.io/input-stream can take. If idx is greater than the
  length of the playlist, the source will be appended. Returns the
  modified playlist."
  [playlist source idx]
  {:pre [(>= idx 0)]}
  (with-playlist playlist
    (if (> idx (count it))
      (append-source! it source)
      (do (.add it idx source) it))))

(defn remove-source!
  "Remove a source from the playlist. This is a noop if idx isn't in
  the playlist. Returns the modified playlist."
  [playlist idx]
  {:pre [(>= idx 0)]}
  (with-playlist playlist
    (when (< idx (count it))
      (.remove #^java.util.List it (int idx)))
    it))

(defn move-source!
  "Move a source in the playlist from one position to another. This is
  a noop if the from-idx is outside the range of the playlist. The
  source will be moved to the end of the playlist if to-idx is greater
  than the length of the list. to-idx will be calculated as the index
  before the source is moved. This operation is atomic."
  [playlist from-idx to-idx]
  {:pre [(>= from-idx 0) (>= to-idx 0)]}
  (with-playlist playlist
    (if (>= from-idx (count it))
      it
      (let [source (nth it from-idx)]
        (insert-source! it source to-idx)
        (remove-source! it (if (< to-idx from-idx)
                                   (inc from-idx)
                                   from-idx))))))
