(ns user
  (:require [clojure.pprint :refer [pprint]]
            [com.stuartsierra.component :as component]
            [environ.core]
            [figwheel-sidecar.repl-api :as fw.repl]
            [reloaded.repl
             :refer [system init start stop go reset reset-all]]
            [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal report]]

            ;; project
            [work-to-list.logging]
            [work-to-list.system]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Logging

(timbre/set-level! :debug)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ENV reloading

(in-ns 'environ.core)

(defn refresh-env
  "Hack to enable in-repl refresh of the environment vars"
  []
  (def env
    (merge (read-env-file ".lein-env")
           (read-system-env)
           (read-system-props))))

(in-ns 'user)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Figwheel

(defrecord Figwheel []
  component/Lifecycle
  (start [this]
    (fw.repl/start-figwheel!)
    this)
  (stop [this]
    (fw.repl/stop-figwheel!)
    this))

(defn cljs-repl []
  (fw.repl/cljs-repl "dev"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dev system

(reloaded.repl/set-init!
 (fn []
   (environ.core/refresh-env)
   (merge
    (work-to-list.system/system)
    {:figwheel (->Figwheel)})))
