(ns shout.playlist)

;;; Playlists must be mutable as they are closed over and read on a
;;; separate thread. Stopping and starting threads will disconnect
;;; clients, so changes in state must be done mutably.

;;; The playlist will be iterated over while concurrently
;;; modified. The iterator should see these changes in a strongly
;;; consistent manner. This implementation should provide this
;;; behaviour.

(deftype ConcurrentArrayList [^java.util.List list]
  java.util.RandomAccess
  java.util.List
  (size [_] (locking list (.size list)))
  (add [_ x] (locking list (.add list x)))
  (add [_ idx x] (locking list (.add list idx x)))
  (remove [_ ^int idx] (locking list (.remove list idx)))
  (get [_ idx] (locking list (.get list idx)))
  (iterator [_]
    (letfn [(initial [cursor]
              (fn [op]
                (condp = op
                  :has-next (if (< cursor (.size list))
                              {:transition (cached cursor (.get list cursor))
                               :has-next true}
                              {:transition (ended)
                               :has-next false})
                  :next (if (< cursor (.size list))
                          {:transition (initial (inc cursor))
                           :next (.get list cursor)}
                          {:transition (ended)
                           :next (throw (java.util.NoSuchElementException.))}))))
            (cached [cursor val]
              (fn [op]
                (condp = op
                  :has-next {:transition (cached cursor val)
                             :has-next true}
                  :next {:transition (initial (inc cursor))
                         :next val})))
            (ended []
              (fn [_] {:transition (ended)
                       :has-next false
                       :next (throw (java.util.NoSuchElementException.))}))
            (transition [state op]
              ((:transition state) op))]
      (let [state (atom {:transition (initial 0)})]
        (reify
          java.util.Iterator
          (hasNext [_]
            (locking list
              (swap! state transition :has-next)
              (:has-next @state)))
          (next [_]
            (locking list
              (swap! state transition :next)
              (:next @state))))))))

(defn ->ConcurrentArrayList []
  (new ConcurrentArrayList (java.util.ArrayList.)))

(defn create-playlist
  "Create a mutable thread-safe (can be modified concurrently and
  should always be consistent) playlist. Sources should be a seq of
  anything that clojure.java.io/input-stream can take."
  ([] (create-playlist []))
  ([sources]
     {:pre [(sequential? sources)]}
     (reduce #(when (.add ^java.util.List %1 %2) %1)
             (->ConcurrentArrayList) sources)))

(defn append-source!
  "Append a source to a playlist. A source is anything that
  clojure.java.io/input-stream can take. Returns the modified
  playlist."
  [^java.util.List playlist source]
  (locking playlist
    (when (.add playlist source) playlist)))

(defn insert-source!
  "Insert a source in to a playlist. A source is anything that
  clojure.java.io/input-stream can take. If idx is greater than the
  length of the playlist, the source will be appended. Returns the
  modified playlist."
  [^java.util.List playlist source idx]
  {:pre [(>= idx 0)]}
  (locking playlist
    (if (> idx (count playlist))
      (append-source! playlist source)
      (do (.add playlist idx source) playlist))))

(defn remove-source!
  "Remove a source from the playlist. This is a noop if idx isn't in
  the playlist. Returns the modified playlist."
  [^java.util.List playlist idx]
  {:pre [(>= idx 0)]}
  (locking playlist
    (when (< idx (count playlist))
      (.remove #^java.util.List playlist (int idx)))
    playlist))

(defn move-source!
  "Move a source in the playlist from one position to another. This is
  a noop if the from-idx is outside the range of the playlist. The
  source will be moved to the end of the playlist if to-idx is greater
  than the length of the list. to-idx will be calculated as the index
  before the source is moved. This operation is atomic."
  [playlist from-idx to-idx]
  {:pre [(>= from-idx 0) (>= to-idx 0)]}
  (locking playlist
    (if (>= from-idx (count playlist))
      playlist
      (let [source (nth playlist from-idx)]
        (insert-source! playlist source to-idx)
        (remove-source! playlist (if (< to-idx from-idx)
                                   (inc from-idx)
                                   from-idx))))))
