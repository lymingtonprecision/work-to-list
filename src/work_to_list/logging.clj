(ns work-to-list.logging
  (:require [taoensso.timbre :as timbre]
            [clojure.string :as string])
  (:import [org.slf4j Logger LoggerFactory]))

(def ^:dynamic *default-log-level* :info)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; SLF4J Interop

(defn ns-to-logger [^String ns-str]
  (LoggerFactory/getLogger (or ns-str "timbre.slf4j")))

(defmacro log-to-slf4j [logger data level]
  (let [tf (symbol (str ".is" (clojure.string/capitalize (name level)) "Enabled"))
        lf (symbol (str "." (name level)))]
    `(when (~tf ~logger)
       (~lf ~logger ((:output-fn ~data) ~data) (force (:?err_ ~data))))))

(def timbre-slf4j-appender
  {:enabled? true
   :async? false
   :min-level nil
   :rate-limit nil
   :output-fn (fn [{msg_ :msg_}] (str (force msg_)))
   :fn (fn [{:keys [level ?err_ ?ns-str output-fn] :as data}]
         (let [logger ^Logger (ns-to-logger ?ns-str)]
           (case level
             :report (log-to-slf4j logger data :error)
             :fatal  (log-to-slf4j logger data :error)
             :error  (log-to-slf4j logger data :error)
             :warn   (log-to-slf4j logger data :warn)
             :info   (log-to-slf4j logger data :info)
             :debug  (log-to-slf4j logger data :debug)
             :trace  (log-to-slf4j logger data :trace))))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Timbre configuration

(defn timbre-config []
  (merge
   timbre/example-config
   {:level *default-log-level*
    :appenders {:slf4j timbre-slf4j-appender}}))

(timbre/handle-uncaught-jvm-exceptions!)
(timbre/set-config! (timbre-config))
