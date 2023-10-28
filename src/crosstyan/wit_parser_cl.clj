(ns crosstyan.wit-parser-cl
  (:gen-class)
  (:require [clojurewerkz.machine-head.client :as mh]
            [taoensso.timbre :as log]))

(def mqtt-url "tcp://weihua-iot.cn:1883")

(defn print-message
  [^String topic _ ^bytes payload]
  (log/info {:topic topic :payload (String. payload)}))



(defn -main
  [& args]
  (let [conn (mh/connect mqtt-url)]
    (mh/subscribe conn {"/test/#" 0} print-message)))
