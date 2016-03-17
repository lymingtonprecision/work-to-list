(ns work-to-list.client.state)

(defn initial-app-state []
  {:db (cljs.reader/read-string js/INIT_STATE)
   :db-id nil
   :db-seq nil
   :path [:index]
   :popup-states {}
   :sync-lost? false
   :reset-required? false})
