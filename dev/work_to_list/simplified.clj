(ns work-to-list.simplified
  (:require [clojure.core.async :as async]
            [clojure.string :as string]
            [bidi.bidi :as bidi]
            [bidi.ring :as bidi.ring]
            [immutant.web :as immutant]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [resource-response]]
            [taoensso.timbre :as log]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.immutant :refer [sente-web-server-adapter]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn http-server
  ([handler]
   (http-server handler 0))
  ([handler port]
   (let [server (immutant.web/run handler :port port)]
     {:server server
      :port (:port server)
      :stop-fn (fn [] (immutant.web/stop server))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def sente-server
  (sente/make-channel-socket-server! sente-web-server-adapter))

(defmulti sente-event-handler :id)

(defmethod sente-event-handler :default
  [event]
  (log/debug (dissoc event :ring-req :ch-recv :?reply-fn :send-fn))
  (when-let [reply (:?reply-fn event)]
    (reply {:unmatched-event event})))

(defmethod sente-event-handler :chsk/ws-ping
  [{:keys [client-id] :as event}]
  (log/debug (str "ping! from " client-id ", client is alive!")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def index
  (assoc-in
   (resource-response "public/index.html")
   [:headers "Content-Type"]
   "text/html;charset=utf-8"))

(def index-handler (constantly index))

(defn not-found [req]
  {:status 404
   :headers {"Content-Type" "text/html;charset=utf-8"}
   :body "Not Found"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def routes
  ["/"
   {"" {:get index-handler}
    "chsk" {:get (:ajax-get-or-ws-handshake-fn sente-server)
            :post (:ajax-post-fn sente-server)}
    true not-found}])

(def ring-defaults
  (assoc-in site-defaults
            [:security :anti-forgery]
            {:read-token (fn [req] (-> req :params :csrf-token))}))

(def app-handler
  (-> routes
      bidi.ring/make-handler
      (wrap-resource "/")
      (wrap-defaults ring-defaults)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn broadcast! [i]
  (log/debugf "Broadcasting server>user: %s" @(:connected-uids sente-server))
  (doseq [uid (:any @(:connected-uids sente-server))]
    ((:send-fn sente-server)
     uid
     [:some/broadcast
      {:what-is-this "An async broadcast pushed from server"
       :how-often "Every 10 seconds"
       :to-whom uid
       :i i}])))

(defn start-example-broadcaster!
  "As an example of server>user async pushes, setup a loop to broadcast an
  event to all connected users every 10 seconds"
  []
  (let [close-ch (async/chan)]
    (async/go-loop [i 0]
      (let [[_ ch] (async/alts! [close-ch (async/timeout 10000)])]
        (when-not (= close-ch ch)
          (broadcast! i)
          (recur (inc i)))))
    close-ch))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce server (atom nil))
(defn stop-server! []
  (when-let [s @server]
    ((:stop-fn s)))
  (reset! server nil))
(defn start-server! []
  (stop-server!)
  (swap! server (constantly (http-server (var app-handler)))))

(defonce sente-router (atom nil))
(defn stop-sente-router! []
  (when-let [f @sente-router] (f))
  (reset! sente-router nil))
(defn start-sente-router! []
  (stop-sente-router!)
  (reset! sente-router
          (sente/start-chsk-router!
           (:ch-recv sente-server)
           sente-event-handler)))

(defonce broadcaster (atom nil))
(defn stop-broadcaster! []
  (when-let [ch @broadcaster]
    (async/close! ch))
  (reset! broadcaster nil))
(defn start-broadcaster! []
  (stop-broadcaster!)
  (reset! broadcaster (start-example-broadcaster!)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(:connected-uids sente-server)

(comment
  (:port @server)

  (do
    (start-sente-router!)
    (start-server!)
    (start-broadcaster!))

  (do
    (stop-broadcaster!)
    (stop-sente-router!)
    (stop-server!)))
