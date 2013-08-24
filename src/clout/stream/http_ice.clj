(ns clout.stream.http-ice
  (:require [aleph.tcp :refer [tcp-client]]
            [aleph.formats :as formats]
            [clojure.data.codec.base64 :as b64]
            [lamina.core :as l]
            [gloss.core :as gloss]
            [clojure.string :as str]
            [clout.stream.stream :refer [OutStream]])
  (:import [java.net URLEncoder]))

(def content-types {:mp3 "audio/mpeg"})

(defn get-content-type [f]
  (or (get content-types f)
      (throw (RuntimeException. (format "Unsupported format: %s" f)))))

(defn base-64 [content]
  (String. (b64/encode (.getBytes content "UTF-8"))))

(defn create-basic-auth-header [user pass]
  (let [pre-enc (format "%s:%s" user pass)]
    (format "Authorization: Basic %s" (base-64 pre-enc))))

(defn url-encode-map [coll delim]
  (let [enc #(-> %
                 (URLEncoder/encode "UTF-8")
                 (str/replace "+" "%20"))] ;not form encoding
    (str/join delim
              (map (fn [[k v]] (format "%s=%s" (enc k) (enc v))) coll))))

(defn create-connection-request-frames
  [{:keys [mount user password agent
           stream-format name public? audio-info
           url genre description]}]
  (->> [(format "SOURCE %s HTTP/1.0" mount)
        (create-basic-auth-header user password)
        (format "User-Agent: %s" agent)
        (format "Content-Type: %s" (get-content-type stream-format))
        (format "ice-name: %s" (or name "no name"))
        (format "ice-public: %d" (if public? 1 0))
        (format "ice-audio-info: %s" (url-encode-map audio-info ";"))
        ;; optional:
        (when url
          (format "ice-url: %s" url))
        (when genre
          (format "ice-genre: %s" genre))
        (when description
          (format "ice-description: %s" description))
        ""]
       (filter (complement nil?))
       (map #(str % "\r\n"))))

(defn create-connection [{:keys [hostname port] :as session}]
  {:connected? false
   :session session
   :ch (l/wait-for-result
        (tcp-client
         {:host hostname
          :port port
          :decoder (gloss/string :utf-8 :delimiters ["\r\n"])}))})

(defn parse-status [status]
  (let [[version resp-code msg] (str/split status #"\s" 3)]
    {:status {:code (Integer/parseInt resp-code)
              :message msg}}))

(defn parse-headers [header & headers] {})  ;todo, maybe

(defn parse-response [[status & headers]]
  (merge (parse-status status)
         (parse-headers headers)))

(defn connect [{:keys [ch session] :as conn}]
  (apply l/enqueue ch (create-connection-request-frames session))
  (let [response (parse-response
                  (l/channel->lazy-seq ch 10000))
        code (get-in response [:status :code])]
    (if (and (>= code 200)
             (< code 300))
      (merge conn {:ch ch :connected? true})
      (throw (IllegalStateException.
              (format "Could not connect using session [%s]. Response: %s"
                      session response))))))

(defn close-connection [{:keys [ch]}]
  (l/close ch))

(defn build-byte-array [bytes]
  (let [[front back] (split-at 4096 bytes)]
    [(->> front
          (map unchecked-byte)
          (byte-array))
     back]))

(deftype IceHttpOutStream [conn]
  OutStream

  (write [_ bytes]
    (when-not (:connected? conn)
      (throw (IllegalStateException.
              (format "Connection [%s] has not been connected." conn))))
    (when (seq bytes)
      (let [[buffer more] (build-byte-array bytes)]
        (l/enqueue (:ch conn) buffer)
        (recur more))))

  (close [_]
    (close-connection conn)))

(defn create-protocol-stream [session]
  (-> session
      create-connection
      connect
      ->IceHttpOutStream))
