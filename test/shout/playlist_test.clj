(ns shout.playlist-test
  (:use midje.sweet
        shout.playlist))

(facts "about mutable playlists"
       (let [emp (->MutableList (ref nil))]
         (fact "should work on an empty colletcion"
               (first emp) => nil
               (next emp) => nil
               (rest emp) => '()
               (count emp) => 0
               (empty? emp) => true
               (= emp emp) => true
               (= emp (->MutableList (ref nil))) => false
               (seq emp) => nil))
       (let [one (conj (->MutableList (ref nil)) 1)]
         (fact "should work on a collection of one value"
               (first one) => 1
               (next one) => nil
               (rest one) => '()
               (count one) => 1
               (empty? one) => false
               (= one one) => true
               (seq one) => one))
       (let [two (conj
                  (conj (->MutableList (ref nil)) 1)
                  2)]
         (fact "should work on a collection of multiple values"
               (first two) => 1
               (next two) => '(2)
               (rest two) => '(2)
               (count two) => 2
               (empty? two) => false
               (= two two) => true
               (seq two) => two)
         (fact "should be mutable"
               (identical? (conj two 3) two) => truthy)
         (fact "should not be chunked"
               (chunked-seq? two) => falsey)))
