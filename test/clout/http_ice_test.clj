(ns clout.http-ice-test
  (:use midje.sweet
        clout.http-ice))

(facts "about basic auth"
       (fact "should create correct header given a user and pass"
             (create-basic-auth-header "Aladdin" "open sesame") =>
             "Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="))


(facts "about map url encoding"
       (fact "should serialize a single key and value"
             (url-encode-map {"foo" "bar"} ";") =>
             "foo=bar")

       (fact "should serialize multiple key value pairs using a delimiter"
             (url-encode-map {"foo" "bar", "baz" "bam"} ";") =>
             "foo=bar;baz=bam")

       (fact "should url-encode if necessary"
             (url-encode-map {"key with spaces" "value@#$"} ";") =>
             "key%20with%20spaces=value%40%23%24")

       (fact "handles empty map"
             (url-encode-map {} ";") => ""))
