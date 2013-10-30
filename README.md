# clj-shout

A Clojure port of the [libshout](http://www.icecast.org/download.php)
library. You can use this library to write a source client, like
[ices](http://www.icecast.org/ices.php), for sending media to a
streaming server.

## Releases and Dependency Information

Leiningen

    [shout "0.1.3"]

Maven

    <dependency>
      <groupId>shout</groupId>
      <artifactId>shout</artifactId>
      <version>0.1.3</version>
    </dependency>

## Status

This library should be treated as beta.

It currently only has support for streaming MP3 format files over
HTTP.

It has been tested and works well with Icecast 2.3.3.

## Usage

This is the basic usage. See shout.session for further options that
can be provided to create-shout-session. See shout.core for functions
that can be used to actually send data to a server.

    (ns shout.test
      (:require [shout.session :refer :all]
                [shout.core :as core]))

    (def session (create-shout-session
                  (with-host "localhost")
                  (with-port 8000)
                  (with-password "hackme")
                  (with-protocol :http)
                  (with-format :mp3)))

    (core/send-source session "/path/to/some.mp3")

## License

Distributed under the Eclipse Public License, the same as Clojure.
