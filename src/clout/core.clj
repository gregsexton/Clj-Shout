(ns clout.core)

(def default-host "localhost")
(def default-port 8000)
(def default-format :mp3)
(def default-protocol :http)
(def default-user "source")
(def default-useragent "clout")
(def default-mount "/example.mp3")

(defmacro defvalue
  ([name key default & [transform]]
     `(defn ~name [val#]
        (if val#
          [~key ((or ~transform identity) val#)]
          [~key ~default]))))
(defvalue with-host :hostname default-host)
(defvalue with-port :port default-port)
(defvalue with-format :format default-format)
(defvalue with-protocol :protocol default-protocol)
(defvalue with-user :user default-user)
(defvalue with-agent :agent default-useragent)
(defvalue with-mount :mount default-mount #(if (re-matches #"^/" %)
                                             % (apply
                                                str (concat "/" %))))
(defvalue with-password :password
  (throw (IllegalArgumentException. "No password provided")))

(defn create-clout-session [& pairs]
  (let [default-map (into {} (list (with-host nil)
                                   (with-port nil)
                                   (with-format nil)
                                   (with-protocol nil)
                                   (with-user nil)
                                   (with-agent nil)
                                   (with-mount nil)))]
    (reduce conj default-map pairs)))
