(ns work-to-list.system
  (:require [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [work-to-list.middleware.not-found :refer [wrap-not-found]]
            [work-to-list.components.hikari-pool :refer [hikari-pool]]
            [work-to-list.components.ifs-work-to-list :refer [ifs-work-to-list]]
            [work-to-list.components.immutant-server :refer [immutant-server]]
            [work-to-list.components.sente-socket :refer [sente-socket-endpoint]]
            [work-to-list.components.frontend :refer [frontend-endpoint]]
            [work-to-list.duct :as duct]
            [work-to-list.routes :as routes]))

(defn system []
  (component/system-using
   (component/system-map
    :db-pool (hikari-pool
              (:db-server env)
              (:db-name env)
              (:db-user env)
              (:db-password env))
    :work-to-list (ifs-work-to-list)
    :http-config {:uri-prefix (routes/sanitize-prefix (:wtl-prefix env))}
    :sente-socket (sente-socket-endpoint)
    :frontend (frontend-endpoint)
    :http-router (duct/ring-router
                  {:middleware [[wrap-defaults :defaults]
                                [wrap-not-found :not-found]]
                   :defaults (assoc-in site-defaults [:static :resources] false)
                   :not-found "Not Found"})
    :http-server (immutant-server {:port (:wtl-port env)}))
   {:work-to-list {:db-spec :db-pool}
    :sente-socket [:http-config :work-to-list]
    :frontend     [:http-config :work-to-list]
    :http-router  [:frontend :sente-socket]
    :http-server  {:handler :http-router}}))
