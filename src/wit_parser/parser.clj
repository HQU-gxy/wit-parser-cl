(ns wit-parser.parser
  (:require [org.clojars.smee.binary.core :as bin]
            [clojure.core.match :refer [match]]
            [clj-commons.byte-streams :as bs]
            [taoensso.timbre :as log])
  (:import (java.io EOFException)))

;; https://wit-motion.yuque.com/wumwnr/docs/gpare3
;; https://github.com/funcool/octet

(def accel-coef
  "unit is g (9.8 m*s^(-2)"
  (-> (/ 1 32768)
      (* 16)))
(def angle-vel-coef
  "unit is degree/s"
  (-> (/ 1 32768)
      (* 2000)))

(def degree-coef
  "unit is degree"
  (-> (/ 1 32768)
      (* 180)))

(defn take!
  "take n from a reference of collection. mutate the reference."
  [n ref-coll]
  (let [ret (take n @ref-coll)]
    (swap! ref-coll (fn [coll] (drop n coll)))
    ret))

(def gyro-codec
  (bin/ordered-map
    ;; to be multiplied by accel-coef
    :accel [:short-le :short-le :short-le]
    :angle-vel [:short-le :short-le :short-le]
    ;; roll pitch yaw
    :angle [:short-le :short-le :short-le]))

(defn map-map
  "mapper is a map of key and function. map the function to the value of the key in the map.
  `(map-map {:a #(+ % 5) :b #(map inc %)} {:a 1 :b [1 2 3]})`"
  [mapper m]
  (into {}
        (map (fn [[k v]]
               (let [f (mapper k)]
                 (if (nil? f)
                   [k v]
                   [k (f v)]))) m)))

(def header-codec
  "read two bytes: head flag"
  [:byte :byte])

(def register-codec
  "read two bytes: start-register, end (always 0x00)"
  [:byte :byte])

(def magnetic-reg (unchecked-byte 0x3A))
(def magnetic-coef 1)
(def magnetic-codec
  [:short-le :short-le :short-le])


(def quaternion-reg (unchecked-byte 0x51))
(def quaternion-coef (/ 1 32768))
(def quaternion-codec
  [:short-le :short-le :short-le :short-le])

(def temperature-reg (unchecked-byte 0x40))
(def temperature-coef (/ 1 100))
(def temperature-codec [:short-le])

(def data-header (unchecked-byte 0x55))
(def gyro-flag (unchecked-byte 0x61))
(def register-flag (unchecked-byte 0x71))

(defn byte->hex-str [x] (format "0x%02x" x))

(defn decode-wit-data
  "decode wit data from byte-array"
  [input]
  (let [split-at-as-is (fn [n coll] (->> coll
                                         (split-at n)
                                         (map (comp bs/to-input-stream byte-array))))
        [header rst] (split-at-as-is 2 input)
        [head flag] (bin/decode header-codec header)]
    (match [head flag]
           ;; match only works for literal
           ;; use a variable name would cause it to be a pattern
           ;; (and be bound to the value)
           ;; A syntax like elixir's pin would be nice
           [0x55 0x61] (try
                         (map-map {:accel     #(map (fn [x] (* x accel-coef)) %)
                                   :angle-vel #(map (fn [x] (* x angle-vel-coef)) %)
                                   :angle     #(map (fn [x] (* x degree-coef)) %)}
                                  (bin/decode gyro-codec rst))
                         (catch EOFException _ {:error "EOF"}))
           [0x55 0x71] (let [[regs rst'] (split-at-as-is 2 (bs/to-byte-array rst))
                             [start _] (bin/decode register-codec regs)
                             _ (log/info "start" (byte->hex-str start))]
                         (condp = start
                           magnetic-reg {:magnetic (bin/decode magnetic-codec rst')}
                           quaternion-reg {:quaternion (->> (bin/decode quaternion-codec rst')
                                                            (map #(* % quaternion-coef)))}
                           temperature-reg {:temperature (* temperature-coef
                                                            (first (bin/decode temperature-codec rst')))}
                           {:error "bad register"}))
           [0x55 _] {:error "bad flag"}
           :else {:error "bad header"})))
