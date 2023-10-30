(ns wit-parser.repl
  (:require [taoensso.timbre :as log]
            [wit-parser.parser :refer [decode-wit-data]]
            [wit-parser.core :refer [-main]])
  (:import (java.io EOFException)))

(decode-wit-data (byte-array [0x55 0x61
                              ;; ax     ay        az
                              0x00 0x01 0x02 0x03 0x04 0x05
                              ;; gx     gy        gz
                              0x06 0x07 0x08 0x09 0x0a 0x0b
                              ;; roll   pitch     yaw
                              0x0c 0x0d 0x0e 0x0f 0x10 0x11]))


(decode-wit-data (byte-array [0x55 0x71
                              ;; reg-temp
                              0x40 0x00
                              0x34 0x56
                              ;; just padding...
                              0x00 0x00 0x00 0x00]))


(decode-wit-data (byte-array [0x55 0x71
                              ;; reg-mag
                              0x3A 0x00
                              0x34 0x56 0x78 0x9a 0xbc 0xde]))

(decode-wit-data (byte-array [0x55 0x71
                              ;; reg-quat
                              0x51 0x00
                              ;; w      x         y         z
                              0x34 0x56 0x78 0x9a 0xbc 0xde 0x10 0x20]))

(-main)
