(ns work-to-list.components.sente-socket
  (:require [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.immutant :refer [sente-web-server-adapter]]
            [taoensso.timbre :as log]
            [work-to-list.diff :as diff]
            [work-to-list.duct :as duct]
            [work-to-list.routes :as routes]
            [work-to-list.socket-handler :refer [handle-event!]]
            [work-to-list.protocols.work-to-list :as work-to-list]))

(defn unsupported-method
  [req]
  {:status 405
   :headers {"Content-Type" "text/plain;charset=utf-8"}
   :body "Method not allowed"})

(defn dispatch-socket-request
  [socket req]
  (case (:request-method req)
    :get ((:ajax-get-or-ws-handshake-fn socket) req)
    :post ((:ajax-post-fn socket) req)
    (unsupported-method req)))

(defn broadcast
  ([socket]
   (partial broadcast socket))
  ([socket event]
   (let [uuids (some-> (:connected-uids socket)
                       deref
                       :any)
         send-fn (:send-fn socket)]
     (doseq [uid uuids]
       (send-fn uid event)))))

(defn stream-work-to-list-updates
  [work-to-list send-fn]
  (let [k (java.util.UUID/randomUUID)]
    (work-to-list/add-watch
     (:list work-to-list) k
     (fn [_ _ old new]
       (when-let [u (seq (diff/unified-diff old new))]
         (let [n (count u)
               ending-seq (swap! (:seq work-to-list) + n)
               applies-to (- ending-seq n)]
           (send-fn [:work-to-list/update
                     [(:id work-to-list) applies-to ending-seq u]])))))))

;; DEBUG ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn watch-for-new-clients!
  [connected-uids]
  (let [k (java.util.UUID/randomUUID)]
    (add-watch
     connected-uids
     k
     (fn [k ref {old-clients :any} {new-clients :any}]
       (log/debug "client connection/disconnection" old-clients new-clients)))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn handler
  [uri-prefix work-to-list]
  (let [routes (routes/channel-socket uri-prefix)
        socket (sente/make-channel-socket-server!
                sente-web-server-adapter
                {:user-id-fn (fn [req] (:client-id req))})
        wtl-id (hash (work-to-list/tasks work-to-list))
        wtl-seq (atom (long 0))
        wtl {:id wtl-id :seq wtl-seq :list work-to-list}
        router (sente/start-server-chsk-router!
                (:ch-recv socket)
                #(handle-event! % wtl))
        updts (stream-work-to-list-updates wtl (broadcast socket))
        ;; DEBUG
        climnt (watch-for-new-clients! (:connected-uids socket))]
    (fn [{:keys [uri path-info] :as req}]
      (when (= :chsk (:handler (routes/match-route routes (or path-info uri))))
        (dispatch-socket-request socket req)))))

(defn wrap-handler
  [handler]
  (-> handler
      wrap-keyword-params
      wrap-params))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn sente-socket
  [{{:keys [uri-prefix] :or {uri-prefix "/"}} :http-config
    work-to-list :work-to-list}]
   (wrap-handler (handler uri-prefix work-to-list)))

(defn sente-socket-endpoint []
  (duct/ring-endpoint sente-socket))
