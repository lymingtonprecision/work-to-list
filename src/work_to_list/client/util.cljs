(ns work-to-list.client.util
  (:require [clojure.string :as string]))

(defn lpad [s l c]
  (let [s (str s)
        x (- l (count s))]
    (if (pos? x)
      (str (string/join (repeat x c)) s)
      s)))

(defn running-on-terminal? []
  (some? js/TERMINAL_ID))

(defn terminal-id []
  (some-> js/TERMINAL_ID js/parseInt (lpad 3 "0")))
