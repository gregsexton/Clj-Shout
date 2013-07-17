(ns clout.session)

(def default-host "localhost")
(def default-port 8000)
(def default-format :mp3)
(def default-protocol :http)
(def default-user "source")
(def default-useragent "clout")
(def default-mount "/example.mp3")
(def default-name "no name")

(defmacro defvalue
  ([name key default & [transform]]
     `(defn ~name [val#]
        (if val#
          [~key ((or ~transform identity) val#)]
          [~key ~default]))))
(defvalue with-host :hostname default-host)
(defvalue with-port :port default-port)
(defvalue with-format :stream-format default-format)
(defvalue with-protocol :protocol default-protocol)
(defvalue with-user :user default-user)
(defvalue with-agent :agent default-useragent)
(defvalue with-mount :mount default-mount #(if (re-find #"^/" %)
                                             %
                                             (apply
                                              str (concat "/" %))))
(defvalue with-name :name default-name)
(defvalue with-password :password
  (throw (IllegalArgumentException. "No password provided")))
(defvalue is-public? :public? false)
(defvalue with-audio-info :audio-info {})
(defvalue with-genre :genre nil)
(defvalue with-url :url nil)
(defvalue with-description :description nil)

(defn create-clout-session [& pairs]
  (let [default-map (into {} (list (with-host nil)
                                   (with-port nil)
                                   (with-format nil)
                                   (with-protocol nil)
                                   (with-user nil)
                                   (with-agent nil)
                                   (with-mount nil)
                                   (with-name nil)
                                   (with-audio-info nil)
                                   (is-public? nil)))]
    (reduce conj default-map pairs)))
