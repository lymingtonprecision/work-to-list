(ns work-to-list.client.state
  (:require [work-to-list.client.util :refer [window-dimensions]]))

(defn initial-app-state []
  {:db (cljs.reader/read-string js/INIT_STATE)
   :db-id nil
   :db-seq nil
   :path [:index]
   :popup-states {}
   :window (window-dimensions)
   :sync-lost? false
   :reset-required? false})
