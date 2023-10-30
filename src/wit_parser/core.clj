(ns wit-parser.core
  (:gen-class)
  (:require [clojurewerkz.machine-head.client :as mqtt]
            [taoensso.timbre :as log]
            [cheshire.core :as json]
            [wit-parser.parser :refer [decode-wit-data]]
            [wit-parser.utils :as u]))

(def mqtt-url "tcp://weihua-iot.cn:1883")

(defn topic->wit-id
  [topic]
  (let [parts (clojure.string/split topic #"/")]
    (if (= (nth parts 1) "wit")
      (nth parts 2)
      nil)))



(defn handle-message
  "
  Handle a message received from the MQTT broker.
   * The MQTT connection
   * The topic message was received on
   * Immutable map of message metadata
   * Byte array of message payload\n
  "
  [conn ^String topic meta-data ^bytes payload]
  (let [id (topic->wit-id topic)
        result (decode-wit-data (byte-array payload))
        error (:error result)]
    (if-not (nil? error)
      (log/error error)
      (let [p (json/encode (u/keys-dash-underscore result))
            ks (keys result)
            t (name (if (> (count ks) 1) :gyro (first ks)))
            topic (u/format "/wit/{}/{}", id t)]
        (do (log/info "topic" topic "payload" p)
            (try (mqtt/publish conn topic p)
                 (catch Exception e (log/error e))))))))

(defn -main
  [& args]
  (let [conn (mqtt/connect mqtt-url
                           {:opts                {:auto-reconnect     true
                                                  :connection-timeout 1
                                                  :keep-alive-interval 5}
                            :on-connect-complete (fn [conn _ _]
                                                   (log/info "mqtt connected to" mqtt-url)
                                                   (mqtt/subscribe conn {"/wit/+/data" 1} (fn [t m p] (handle-message conn t m p))))
                            :on-connection-lost  (fn [cause] (log/error "mqtt connection lost" cause))})]
    nil))
