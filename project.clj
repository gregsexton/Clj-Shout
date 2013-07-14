(defproject clout "0.1.0-SNAPSHOT"
  :description "A Clojure port of libshout"
  :url "http://www.gregsexton.org"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [aleph "0.3.0-rc2"]
                 [org.clojure/data.codec "0.1.0"]]
  :plugins [[lein-midje "3.0.0"]]
  :profiles {:dev {:dependencies [[midje "1.5.0"]]}})
