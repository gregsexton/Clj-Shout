(ns clout.proto.http-ice-test
  (:use midje.sweet
        clout.proto.http-ice))

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

(facts "about http response parsing"
       (fact "should parse status line"
             (parse-status "HTTP/1.0 200 OK") =>
             {:status {:code 200
                       :message "OK"}}

             (parse-status "HTTP/1.0 401 Authentication Required") =>
             {:status {:code 401
                       :message "Authentication Required"}}))

