(ns work-to-list.components.immutant-server
  (:require [com.stuartsierra.component :as component]
            [immutant.web]
            [taoensso.timbre :as log])
  (:import java.lang.Integer))

(def default-options
  {:host "0.0.0.0"
   :port 0})

(defn remove-nil-entries [m]
  (reduce
    (fn [rs [k v]]
      (if (nil? v)
        rs
        (assoc rs k v)))
    {}
    m))

(defrecord ImmutantServer [options handler]
  component/Lifecycle
  (start [this]
    (if (:server this)
      this
      (let [opts (-> (merge default-options (remove-nil-entries options))
                     (update :port #(if (nil? %) 0 (Integer. %))))
            server (immutant.web/run handler opts)]
        (when (zero? (:port opts))
          (log/warn (str "no port specified, running at "
                         (:host server) ":" (:port server))))
        (assoc this :server server))))
  (stop [this]
    (when-let [s (:server this)]
      (immutant.web/stop s))
    (dissoc this :server)))

(defn immutant-server
  ([]
   (immutant-server {}))
  ([options]
   (component/using
    (map->ImmutantServer {:options options})
    [:handler]))
  ([options handler]
   (->ImmutantServer options handler)))
