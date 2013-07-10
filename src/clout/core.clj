(ns clout.core)

(def default-host "localhost")
(def default-port 8000)
(def default-format :mp3)
(def default-protocol :http)
(def default-user "source")
(def default-useragent "clout")

(defmacro defvalue [name key default]
  `(defn ~name [val#]
     (if val# [~key val#] [~key ~default])))
(defvalue with-host :hostname default-host)
(defvalue with-port :port default-port)
(defvalue with-format :format default-format)
(defvalue with-protocol :protocol default-protocol)
(defvalue with-user :user default-user)
(defvalue with-agent :agent default-useragent)
(defvalue with-password :password
  (throw (IllegalArgumentException. "No password provided")))

(defn create-clout-session [& pairs]
  (let [default-map (into {} (list (with-host nil)
                                   (with-port nil)
                                   (with-format nil)
                                   (with-protocol nil)
                                   (with-user nil)
                                   (with-agent nil)))]
    (reduce conj default-map pairs)))
