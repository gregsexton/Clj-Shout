(defproject clout "0.1.0-SNAPSHOT"
  :description "A Clojure port of libshout"
  :url "http://www.gregsexton.org"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-http "0.7.1"]]
  :plugins [[lein-midje "3.0.0"]]
  :profiles {:dev {:dependencies [[midje "1.5.0"]]}})
