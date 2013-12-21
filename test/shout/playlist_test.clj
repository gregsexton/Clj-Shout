(ns shout.playlist-test
  (:use midje.sweet
        shout.playlist))

(facts "about playlists"
       (fact "should be able to create a playlist"
             (create-playlist) => []
             (create-playlist ["foo"]) => ["foo"]
             (create-playlist ["foo1" "foo2"]) => ["foo1" "foo2"])

       (fact "should be able to mutably append to a playlist"
             (append-source! (create-playlist) "foo") => ["foo"]
             (append-source! (create-playlist ["foo1"]) "foo2") => ["foo1" "foo2"]
             (append-source! (create-playlist ["foo1" "foo2"]) "foo3") => ["foo1" "foo2" "foo3"])

       (fact "should be able to mutably insert a source"
             (insert-source! (create-playlist) "foo" 0) => ["foo"]
             (insert-source! (create-playlist) "foo" 1) => ["foo"]
             (insert-source! (create-playlist) "foo" -1) => (throws AssertionError)
             (insert-source! (create-playlist ["foo"]) "bar" 0) => ["bar" "foo"]
             (insert-source! (create-playlist ["foo"]) "bar" 1) => ["foo" "bar"]
             (insert-source! (create-playlist ["foo" "baz"]) "bar" 0) => ["bar" "foo" "baz"]
             (insert-source! (create-playlist ["foo" "baz"]) "bar" 1) => ["foo" "bar" "baz"]
             (insert-source! (create-playlist ["foo" "baz"]) "bar" 2) => ["foo" "baz" "bar"])

       (fact "should be able to mutably remove a source"
             (remove-source! (create-playlist) 0) => []
             (remove-source! (create-playlist) 1) => []
             (remove-source! (create-playlist) -1) => (throws AssertionError)
             (remove-source! (create-playlist ["foo"]) 0) => []
             (remove-source! (create-playlist ["foo"]) 1) => ["foo"]
             (remove-source! (create-playlist ["foo" "bar"]) 0) => ["bar"]
             (remove-source! (create-playlist ["foo" "bar"]) 1) => ["foo"]
             (remove-source! (create-playlist ["foo" "bar"]) 2) => ["foo" "bar"])

       (fact "should be able to move a source"
             (move-source! (create-playlist) 0 0) => []
             (move-source! (create-playlist) 1 0) => []
             (move-source! (create-playlist) 0 1) => []
             (move-source! (create-playlist) 1 1) => []
             (move-source! (create-playlist) -1 1) => (throws AssertionError)
             (move-source! (create-playlist) 1 -1) => (throws AssertionError)
             (move-source! (create-playlist ["foo" "bar"]) 0 0) => ["foo" "bar"]
             (move-source! (create-playlist ["foo" "bar"]) 0 1) => ["foo" "bar"]
             (move-source! (create-playlist ["foo" "bar"]) 0 2) => ["bar" "foo"]
             (move-source! (create-playlist ["foo" "bar"]) 1 0) => ["bar" "foo"]
             (move-source! (create-playlist ["foo" "bar"]) 1 1) => ["foo" "bar"]
             (move-source! (create-playlist ["foo" "bar"]) 1 2) => ["foo" "bar"]
             (move-source! (create-playlist ["foo" "bar"]) 2 1) => ["foo" "bar"]
             (move-source! (create-playlist ["foo" "bar"]) 0 7) => ["bar" "foo"]))
