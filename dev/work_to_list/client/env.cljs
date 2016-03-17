(ns work-to-list.client.env
  (:require [taoensso.timbre :as log]))

(enable-console-print!)
(log/set-level! :debug)

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! re-frame.db/app-db update-in [:__figwheel_counter] inc)
  )

(comment
  (navigation/navigate-to :index)
  (navigation/navigate-to :current-bookings)
  (navigation/navigate-to :terminal :id js/TERMINAL_ID)
  (navigation/navigate-to :terminal :id "007")
  (navigation/navigate-to :production-line :id "1")
  (navigation/navigate-to :work-centre :id "MC170")
  (re-frame/dispatch [:update-path [:index]])
  (re-frame/dispatch [:update-path [:terminal :id "001"]])
  (re-frame/dispatch [:update-path [:work-centre :id "PR016"]])
  (:path @re-frame.db/app-db)
  (:db-seq @re-frame.db/app-db)
  (:db-id @re-frame.db/app-db)
  )
