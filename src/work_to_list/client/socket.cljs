(ns work-to-list.client.socket
  (:require-macros [taoensso.timbre :as log])
  (:require [taoensso.sente :as sente]
            [work-to-list.routes :as routes]
            [work-to-list.client.socket-handler :refer [handle-event!]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utility fns

(defn socket-url []
  (routes/path-for (routes/channel-socket js/BASE_URL) :chsk))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Refs

(defonce sente-client
  (sente/make-channel-socket! (socket-url) {:type :ws}))

(defonce sente-router (atom nil))

(defn new-router []
  (sente/start-client-chsk-router! (:ch-recv sente-client) handle-event!))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn stop! []
  (when-let [f @sente-router] (f)))

(defn start! []
  (stop!)
  (reset! sente-router (new-router)))

(defn send
  ([event]
   ((:send-fn sente-client) event))
  ([event timeout callback-fn]
   ((:send-fn sente-client) event timeout callback-fn)))
