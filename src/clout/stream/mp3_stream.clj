(ns clout.stream.mp3-stream
  (:require [clout.stream.stream :refer [OutStream] :as s]))

(defn bit-extract
  "Extract certain bits from an integer returning them as an
  integer. start is how many bits from the right to look at, length is
  the number of bits from this position to extract."
  [x start length]
  (let [anchor (bit-shift-left 1 (dec length))
        mask (bit-xor anchor (dec anchor))]
    (bit-and (bit-shift-right x (- start length)) mask)))

(defn combine-bytes [b1 b2 b3 b4]
  (when (and b1 b2 b3 b4)
    (unchecked-int
     (bit-or (bit-shift-left b1 24)
             (bit-shift-left b2 16)
             (bit-shift-left b3 8)
             b4))))

(defn get-depth [m]
  (letfn [(rec [m cnt]
            (if-not (map? m)
              cnt
              (recur (second (first m)) (inc cnt))))]
    (rec m 0)))

(defmacro deflookup
  ([lname transforms table]
     (let [gensyms (apply vector (for [_ (range (get-depth table))] (gensym)))
           name (symbol (apply str (concat "lookup-" (name lname))))
           ts (map #(if (= % '_) 'identity %) transforms)]
       `(defn ~name ~gensyms
          (get-in ~table                ;evaluated more than once
                  [~@(map list (concat ts (repeat identity)) gensyms)])))))

(deflookup version [] {0 25, 2 2, 3 1})

(deflookup layer [] {1 3, 2 2, 3 1})

(deflookup bitrate [_
                    #(condp = % 1 :v1 2 :v2 25 :v2 nil)
                    #(condp = % 1 :l1 2 :l2 3 :l3 nil)]
  {1   {:v1 {:l1 32  :l2 32  :l3 32}  :v2 {:l1 32  :l2 8   :l3 8}}
   2   {:v1 {:l1 64  :l2 48  :l3 40}  :v2 {:l1 48  :l2 16  :l3 16}}
   3   {:v1 {:l1 96  :l2 56  :l3 48}  :v2 {:l1 56  :l2 24  :l3 24}}
   4   {:v1 {:l1 128 :l2 64  :l3 56}  :v2 {:l1 64  :l2 32  :l3 32}}
   5   {:v1 {:l1 160 :l2 80  :l3 64}  :v2 {:l1 80  :l2 40  :l3 40}}
   6   {:v1 {:l1 192 :l2 96  :l3 80}  :v2 {:l1 96  :l2 48  :l3 48}}
   7   {:v1 {:l1 224 :l2 112 :l3 96}  :v2 {:l1 112 :l2 56  :l3 56}}
   8   {:v1 {:l1 256 :l2 128 :l3 112} :v2 {:l1 128 :l2 64  :l3 64}}
   9   {:v1 {:l1 288 :l2 160 :l3 128} :v2 {:l1 144 :l2 80  :l3 80}}
   10  {:v1 {:l1 320 :l2 192 :l3 160} :v2 {:l1 160 :l2 96  :l3 96}}
   11  {:v1 {:l1 352 :l2 224 :l3 192} :v2 {:l1 176 :l2 112 :l3 112}}
   12  {:v1 {:l1 384 :l2 256 :l3 224} :v2 {:l1 192 :l2 128 :l3 128}}
   13  {:v1 {:l1 416 :l2 320 :l3 256} :v2 {:l1 224 :l2 144 :l3 144}}
   14  {:v1 {:l1 448 :l2 384 :l3 320} :v2 {:l1 256 :l2 160 :l3 160}}})

(deflookup samplerate [] {0 { 1 44100 2 22050 25 11025 }
                          1 { 1 48000 2 24000 25 12000 }
                          2 { 1 32000 2 16000 25 8000 }})

(defn validate-header [header]
  (or (and (:valid-sync? header)
           (not (nil? (:version header)))
           (not (nil? (:layer header)))
           (not (nil? (:bitrate header)))
           (not (nil? (:samplerate header)))
           header)
      nil))

(defn maybe-parse-header [[b1 b2 b3 b4]]
  (when-let [head (combine-bytes b1 b2 b3 b4)]
    (let [sync (bit-extract head 32 11)
          version (lookup-version (bit-extract head 21 2))
          layer (lookup-layer (bit-extract head 19 2))]
      (validate-header
       {:valid-sync? (= sync 2047)       ;11 bits set
        :version version
        :layer layer
        :error-protection? (= (bit-extract head 17 1) 0)
        :bitrate (lookup-bitrate version layer (bit-extract head 16 4))
        :samplerate (lookup-samplerate version (bit-extract head 12 2))
        :padded? (= (bit-extract head 10 1) 1)
        :stereo? (not= (bit-extract head 8 2) 3)
        :mode-ext-code (bit-extract head 6 2) ;raw
        :copyright? (= (bit-extract head 4 1) 1)
        :original? (= (bit-extract head 4 1) 0)
        :emphasis? (not= (bit-extract head 4 1) 0)}))))

(defn lookup-samples-per-frame [{:keys [layer]}]
  (if (= layer 1) 384 1152))

(defn frame-length
  "Calculate the frame length in milliseconds"
  [{:keys [samplerate] :as header}]
  (when-let [samples (lookup-samples-per-frame header)]
    (int (* (/ samples samplerate) 1000))))

(defn frame-size
  "Calculate the frame size in bytes"
  [{:keys [bitrate samplerate padded? layer] :as header}]
  (let [samples (lookup-samples-per-frame header)
        slot-length (if (= layer 1) 4 1)]
    (* (+ (quot (* samples (/ (* bitrate 1000) 8))
                samplerate)
          (if padded? 1 0))
       slot-length)))

(defn buffer-built? [buffer]
  ;; 4095 is arbitrary
  (> (count buffer) 4095))

(defn build-buffer [bytes]
  (letfn [(rec [bytes buffer pause]
            (if (or (buffer-built? buffer) (empty? bytes))
              {:buffer buffer :more bytes :pause pause}
              (if-let [header (maybe-parse-header (take 4 bytes))]
                (let [frame-size (frame-size header)]
                  (recur (drop frame-size bytes)
                         (concat buffer (take frame-size bytes))
                         (+ pause (frame-length header))))
                ;; error, skip this byte
                (recur (rest bytes) buffer pause))))]
    (rec bytes [] 0)))

(defmacro with-duration [start pause & body]
  `(let [res# (do ~@body)
         length# (- (System/currentTimeMillis) ~start)
         s# (- ~pause length#)]
     (if-not (pos? s#)
       (Math/abs s#)                    ;return excess
       (do (Thread/sleep s#) 0))))

(defn create-seq
  ([bytes]
     (create-seq bytes build-buffer))
  ([bytes build-buffer]
     (lazy-seq
      (when-let [bs (seq bytes)]
        (let [{:keys [buffer more pause]} (build-buffer bs)]
          (cons {:buffer buffer
                 :pause pause}
                (create-seq more build-buffer)))))))

(defn build-byte-array [bytes]
  (->> bytes
       (map unchecked-byte)
       (byte-array)))

(defn write-bytes [writer bytes]
  (letfn [(rec [buffers excess-pause]
            (let [start (System/currentTimeMillis)]
              (when-let [{:keys [buffer pause]} (first buffers)]
                (let [excess-pause (writer (build-byte-array buffer)
                                           (- pause excess-pause) start)]
                  (recur (rest buffers) excess-pause)))))]
    (rec (create-seq bytes) 0)))

(deftype Mp3OutStream [stream]
  OutStream

  (write [_ bytes]
    ;; takes a (potentially infininte) sequence of bytes and writes
    ;; these to the supplied stream in discrete byte array
    ;; chunks. Useful mostly for debugging or flooding a server
    (write-bytes (fn [buffer _ _] (s/write stream buffer)) bytes))

  (close [_]
    (s/close stream)))

(deftype RealTimeMp3OutStream [stream]
  OutStream

  (write [_ bytes]
    ;; takes a (potentially infininte) sequence of bytes and writes
    ;; these to the supplied stream in discrete byte array
    ;; chunks. Parses the bytes for mp3 headers and sleeps the thread
    ;; so as to write in 'real time'
    (write-bytes (fn [buffer pause start]
                   (with-duration start pause
                     (s/write stream buffer)))
                 bytes))

  (close [_]
    (s/close stream)))
