# clj-shout

A Clojure port of the [libshout](http://www.icecast.org/download.php)
library. You can use this library to write a source client, like
[ices](http://www.icecast.org/ices.php), for sending media to a
streaming server.

## Status

This library should be treated as beta.

It currently only has support for streaming MP3 format files over
HTTP.

It has been tested and works well with Icecast 2.3.3.

## Usage

This is the basic usage. See clout.session for further options that
can be provided to create-clout-session. See clout.core for functions
that can be used to actually send data to a server.

    (ns clout.test
      (:require [clout.session :refer :all]
                [clout.core :as core]))

    (def session (create-clout-session
                  (with-host "localhost")
                  (with-port 8000)
                  (with-password "hackme")
                  (with-protocol :http)
                  (with-format :mp3)))

    (core/send-source session "/path/to/some.mp3")

## License

Distributed under the Eclipse Public License, the same as Clojure.
