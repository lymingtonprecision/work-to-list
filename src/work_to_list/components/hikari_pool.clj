(ns work-to-list.components.hikari-pool
  (:require [com.stuartsierra.component :as component]
            [hikari-cp.core :as hk]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Defaults

(def default-options
  (merge
   hk/default-datasource-options
   {:auto-commit true
    :read-only false
    :minimum-idle 2
    :maximum-pool-size 20
    :adapter "oracle"
    :driver-type "thin"
    :port-number 1521
    :implicit-caching-enabled true
    :max-statements 200}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Component

(defrecord HikariPool [options]
  component/Lifecycle
  (start [this]
    (if (:datasource this)
      this
      (let [opt (merge default-options options)
            ds (hk/make-datasource opt)]
        (assoc this :datasource ds))))
  (stop [this]
    (when-let [ds (:datasource this)]
      (hk/close-datasource ds))
    (dissoc this :datasource)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn hikari-pool [server database username password]
  (->HikariPool {:server-name server
                 :database-name database
                 :username username
                 :password password}))
