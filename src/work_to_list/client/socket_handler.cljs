(ns work-to-list.client.socket-handler
  (:require-macros [taoensso.timbre :as log])
  (:require [re-frame.core :refer [dispatch]]
            [taoensso.timbre :as log]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; App event handler

(defmulti handle-app-push-event! (fn [id _] id))

(defmethod handle-app-push-event!
  :default
  [id data]
  (log/debugf "Received from %s event from server: %s" id data))

(defmethod handle-app-push-event!
  :work-to-list/update
  [_ data]
  (dispatch [:update-state data]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Socket event handler

(defmulti handle-event! :id)

(defmethod handle-event!
  :default
  [event]
  (log/debugf "Unhandled event: %s" (:event event)))

(defmethod handle-event!
  :chsk/state
  [{{:keys [open? first-open?]} :?data :as event}]
  (cond
    first-open? (log/debug "Channel established")
    (not open?) (dispatch [:connection-lost!])
    :else (log/debugf "Channel state change: %s" (:?data event))))

(defmethod handle-event!
  :chsk/recv
  [{[id data] :?data :as event}]
  (when id
    (handle-app-push-event! id data)))

(defmethod handle-event!
  :chsk/handshake
  [{:keys [?data] :as event}]
  (log/debugf "Handshake: %s" ?data)
  (dispatch [:connection-established]))
