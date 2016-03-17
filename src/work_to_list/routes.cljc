(ns work-to-list.routes
  (:require [clojure.string :as string]
            [bidi.bidi :as bidi]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utility fns

(defn prepend-forward-slash [s]
  (if (string/starts-with? s "/")
    s
    (str "/" s)))

(defn append-forward-slash [s]
  (if (string/ends-with? s "/")
    s
    (str s "/")))

(defn sanitize-prefix [prefix]
  (-> prefix
      str
      prepend-forward-slash
      append-forward-slash))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn channel-socket
  ([]
   (channel-socket "/"))
  ([prefix]
   (let [prefix (sanitize-prefix prefix)]
     ["" {prefix {"chsk" :chsk}}])))

(defn frontend
  ([]
   (frontend "/"))
  ([prefix]
   (let [prefix (sanitize-prefix prefix)]
     [""
      {
       prefix
       {"" :index
        "current-bookings" :current-bookings
        "indirect-bookings" :indirect-bookings
        "manager/" {[[#"\d+" :id]] :manager}
        "production-line/" {[[#"\d+" :id]] :production-line}
        "wc/" {[[#"(?i)[A-Z]{2}\d{3}" :id]] :work-centre}
        "terminal/" {[[#"\d+" :id]] :terminal}}}])))

(def match-route bidi/match-route)
(def path-for bidi/path-for)
