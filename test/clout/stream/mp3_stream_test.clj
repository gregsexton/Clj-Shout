(ns clout.stream.mp3-stream-test
  (:use midje.sweet
        clout.stream.mp3-stream))

(facts "about bit-extraction"
       (fact "should deal with the low end"
             (bit-extract 1 1 1) => 1
             (bit-extract 10 4 4) => 10
             (bit-extract 10 4 1) => 1
             (bit-extract 10 3 1) => 0
             (bit-extract 10 2 1) => 1
             (bit-extract 10 1 1) => 0)
       (fact "should deal with the high end"
             (bit-extract 0xaaaaaaaa 32 8) => 0xaa
             (bit-extract 0xaaaaaaaa 32 1) => 1
             (bit-extract 0xfffba040 20 1) => 1
             (bit-extract 0xfffba040 19 2) => 1

             (bit-extract 0xfffba040 32 8) => 0xff
             (bit-extract 0xfffba040 24 8) => 0xfb
             (bit-extract 0xfffba040 16 8) => 0xa0
             (bit-extract 0xfffba040 8 8)  => 0x40))

(facts "about combining bytes"
       (fact "should combine correctly"
             (combine-bytes (unchecked-int 0xff)
                            (unchecked-int 0xfb)
                            (unchecked-int 0xa0)
                            (unchecked-int 0x40)) => (unchecked-int 0xfffba040)))

(facts "about attempting to parse a header"

       ;; http://mpgedit.org/mpgedit/mpeg_format/MP3Format.html

       (fact "should return nil given an invalid header"
             (maybe-parse-header [(unchecked-int 0xfd)
                                  (unchecked-int 0x0)
                                  (unchecked-int 0xc0)
                                  (unchecked-int 0x00)]) => nil)

       (let [header [(unchecked-int 0xff)
                     (unchecked-int 0xfb)
                     (unchecked-int 0xa0)
                     (unchecked-int 0x40)]]
         (fact "should successfully parse a full example header"
               (maybe-parse-header header) => (contains {:valid-sync? true})
               (maybe-parse-header header) => (contains {:version 1})
               (maybe-parse-header header) => (contains {:layer 3})
               (maybe-parse-header header) => (contains {:error-protection? false})
               (maybe-parse-header header) => (contains {:bitrate 160})
               (maybe-parse-header header) => (contains {:samplerate 44100})
               (maybe-parse-header header) => (contains {:padded? false})
               (maybe-parse-header header) => (contains {:stereo? true})
               (maybe-parse-header header) => (contains {:copyright? false})
               (maybe-parse-header header) => (contains {:original? true})
               (maybe-parse-header header) => (contains {:emphasis? false})))

       (fact "should lookup the version"
             (maybe-parse-header [(unchecked-int 0xff)
                                  (unchecked-int 0xe7)
                                  (unchecked-int 0xc0)
                                  (unchecked-int 0x00)]) => (contains {:version 25}))

       (fact "should lookup the layer"
             (maybe-parse-header [(unchecked-int 0xff)
                                  (unchecked-int 0xe7)
                                  (unchecked-int 0xc0)
                                  (unchecked-int 0x00)]) => (contains {:layer 1}))

       (fact "should lookup the bitrate correctly"
             (lookup-bitrate 1 2 7) => 112
             (lookup-bitrate 25 1 7) => 112
             (lookup-bitrate 1 3 8) => 112
             (lookup-bitrate 2 2 11) => 112
             ;; version out of range
             (lookup-bitrate 4 2 1) => nil
             ;; layer out of range
             (lookup-bitrate 1 4 1) => nil
             (lookup-bitrate 1 2 0) => nil
             (lookup-bitrate 1 2 15) => nil
             (lookup-bitrate 1 2 16) => nil)

       (fact "should lookup the samplesize correctly"
             (lookup-samplerate 1 0) => 44100
             (lookup-samplerate 2 1) => 24000
             (lookup-samplerate 25 2) => 8000
             (lookup-samplerate 12 2) => nil
             (lookup-samplerate 2 3) => nil))

(facts "about calculating frame length from header"
       (fact "should calculate frame length"
             (frame-length {:samplerate 44100
                            :layer 3}) => 26))


(facts "about calculating frame size from header"
       (fact "should calculate frame-size"
             (frame-size {:samplerate 44100
                          :padding false
                          :layer 3
                          :bitrate 128}) => 417))
