(ns wit-parser.utils
  (:import (org.slf4j.helpers MessageFormatter)))

(defn format
  "A simple wrapper around slf4j MessageFormatter.
  Format string like what you would do with format from C++20/Rust fmt/Python str.format.
  See https://www.slf4j.org/api/org/slf4j/helpers/MessageFormatter.html"
  [fmt & args]
  (let [array (into-array args)]
    (MessageFormatter/basicArrayFormat fmt array)))

(defn keyword-dash-underscore
  "convert keyword from dash divided to underscore"
  [kw]
  (let [s (name kw)
        s' (clojure.string/replace s "-" "_")
        s'' (keyword s')]
    s''))

(defn keys-dash-underscore
  "convert keys of map from dash divided to underscore
  `(keys-dash-underscore {:a-b-c 1 :d-e-f 2 :e-f-g 3})`"
  [m]
  (into {} (map (fn [[k v]] [(keyword-dash-underscore k) v]) m)))
