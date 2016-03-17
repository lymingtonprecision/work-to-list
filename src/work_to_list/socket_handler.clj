(ns work-to-list.socket-handler
  (:require [taoensso.timbre :as log]
            [work-to-list.protocols.work-to-list :as work-to-list]))

(defmulti handle-event! :id)

(defmethod handle-event! :default
  [event _]
  (log/debug (str "received event " (:id event) " from " (:client-id event))))

(defmethod handle-event! :work-to-list/fetch-state
  [{:keys [?reply-fn] :as event} wtl]
  (when ?reply-fn
    (?reply-fn {:id (:id wtl)
                :seq @(:seq wtl)
                :data (work-to-list/tasks (:list wtl))})))
