(ns work-to-list.client.handlers
  (:require-macros [taoensso.timbre :as log])
  (:require [re-frame.core :as re-frame
             :refer [register-handler trim-v dispatch]]
            [taoensso.timbre :as log]
            [work-to-list.diff :as diff]
            [work-to-list.client.state :refer [initial-app-state]]
            [work-to-list.client.socket :as socket]
            [work-to-list.client.util :refer [window-dimensions]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utility fns

(defn first-arg
  "Middleware which calls the handler with the second element of v so that you
  can write single parameter handlers as:

      (register-handler :event first-arg (fn [db arg] ...))

  Instead of:

      (register-handler :event (fn [db [_ arg]] ...))
      ;; or using the trim-v middleware:
      (register-handler :event trim-v (fn [db [arg]] ...))

  No more unnecessary vectors destructuring!"
  [handler]
  (fn [db [_ v]]
    (handler db v)))

(defn request-state []
  (socket/send
   [:work-to-list/fetch-state]
   5000
   (fn [reply]
     (dispatch [:reset-state reply]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Top level state

(defn initialize [db]
  (merge db (initial-app-state)))

(defn replace-state [db {:keys [id seq data] :as new-state}]
  (log/debug "replacing state with " (dissoc new-state :data))
  (merge db {:db data :db-id id :db-seq seq :sync-lost? false}))

(defn update-state [db [id start-seq end-seq updates]]
  (if (= [(:db-id db) (:db-seq db)] [id start-seq])
    (do
      (log/debug "processing updates from " start-seq " till " end-seq)
      (log/trace updates)
      (assoc db
             :db-seq end-seq
             :db (diff/patch (:db db) updates)))
    (do
      (log/debug "received out-of-sequence update"
                 [(select-keys db [:db-id :db-seq])]
                 [id start-seq])
      (dispatch [:sync-lost!])
      db)))

(register-handler :initialize initialize)
(register-handler :reset-state first-arg replace-state)
(register-handler :update-state first-arg update-state)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Window resizing

(defn update-window-size [db]
  (update db :window merge (window-dimensions)))

(register-handler :window-resize update-window-size)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Navigation/component display

(defn update-path [db value]
  (log/debug "changing path to " value)
  (assoc db :path value))

(defn show-popup [db popup-path]
  (assoc-in db (cons :popup-states popup-path) true))

(defn hide-popup [db popup-path]
  (let [popup-group (butlast popup-path)
        popup-id (last popup-path)]
    (update-in db (cons :popup-states popup-group) dissoc popup-id)))

(register-handler :update-path first-arg update-path)
(register-handler :show-popup trim-v show-popup)
(register-handler :hide-popup trim-v hide-popup)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Socket state changes

(defn connection-established! [db]
  (log/debug "socket connected")
  (if (or (:reset-required? db) (some? (:db-id db)))
    (js/location.reload)
    (request-state))
  db)

(defn sync-lost! [db]
  (log/debug "lost synchronization with server")
  (when-not (:sync-lost? db)
    (request-state))
  (assoc db :sync-lost? true))

(defn connection-lost! [db]
  (log/debug "socket connection lost!")
  (assoc db :reset-required? true))

(register-handler :connection-established connection-established!)
(register-handler :sync-lost! sync-lost!)
(register-handler :connection-lost! connection-lost!)
