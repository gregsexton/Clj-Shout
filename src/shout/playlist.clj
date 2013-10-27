(ns shout.playlist)

;;; Playlists must be mutable as they are closed over and read on a
;;; separate thread. Stopping and starting threads will disconnect
;;; clients, so changes in state must be done on the fly.

;;; This is implemented as cons cells wrapped in refs for coordination
;;; purposes. Consing (the result of conj) mutably appends. Ref
;;; dereferencing is handled transparently. If this turns out to be
;;; too inefficient maybe a rope would make a better datastructure?
;;; This is purposely not chunked so as not to create extra
;;; input-streams unnecessarily.

(deftype MutableList [head]
  clojure.lang.Sequential
  clojure.lang.ISeq
  (first [_] (first (deref head)))
  (next [_] (next (deref head)))
  (more [_] (rest (deref head)))
  (cons [this x]
    (letfn [(last-ref-cons [coll]
              (if (seq (next coll))
                (recur (next coll))
                (.head coll)))]
      (dosync
       (let [last-ref-cons (last-ref-cons this)]
         (ref-set last-ref-cons
                  (if (nil? @last-ref-cons)
                    (MutableList. (ref (cons x (empty this))))
                    (MutableList.
                     (ref (cons (first (deref last-ref-cons))
                                (MutableList. (ref (cons x (empty this)))))))))
         this))))
  (count [_] (count (deref head)))
  (empty [_] (MutableList. (ref nil)))
  (equiv [this x] (identical? this x))
  (seq [this] (when (deref head) this)))
