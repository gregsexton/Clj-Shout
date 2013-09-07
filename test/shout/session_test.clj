(ns shout.session-test
  (:use midje.sweet
        shout.session))

(facts "about creating a new session"
       (fact "should provide all defaults"
             (create-shout-session) => (contains
                                        {:hostname "localhost"
                                         :port 8000
                                         :stream-format :mp3
                                         :protocol :http
                                         :user "source"
                                         :agent "shout"
                                         :name "no name"
                                         :audio-info {}
                                         :public? false
                                         :mount "/example.mp3"}))

       (fact "should provide defaults for missing builder values"
              (create-shout-session
               (with-port 1234)) => (contains
                                     {:hostname "localhost"
                                      :port 1234
                                      :stream-format :mp3
                                      :protocol :http
                                      :user "source"
                                      :agent "shout"
                                      :mount "/example.mp3"
                                      :public? false
                                      :audio-info {}
                                      :name "no name"}))

       (fact "should provide a readable builder syntax for setting every possible option at once"
             (create-shout-session
              (with-host "host")
              (with-port 1234)
              (with-format :mp3)
              (with-protocol :http)
              (with-user "foo")
              (with-password "hackme")
              (with-mount "stream.mp3")
              (with-name "name changed")
              (is-public? true)
              (with-audio-info {"bitrate" 128})
              (with-genre "blues")
              (with-url "http://www.gregsexton.org")
              (with-description "awesome stream")
              (with-agent "agent")) => (contains
                                        {:hostname "host"
                                         :port 1234
                                         :stream-format :mp3
                                         :protocol :http
                                         :user "foo"
                                         :agent "agent"
                                         :mount "/stream.mp3"
                                         :name "name changed"
                                         :public? true
                                         :audio-info {"bitrate" 128}
                                         :url "http://www.gregsexton.org"
                                         :genre "blues"
                                         :description "awesome stream"
                                         :password "hackme"})))



(facts "about connecting"
       (fact "should connect without error"
             ))
