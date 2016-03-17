(ns work-to-list.client.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [work-to-list.client.env]
            [work-to-list.client.handlers]
            [work-to-list.client.navigation :as navigation]
            [work-to-list.client.socket :as socket]
            [work-to-list.client.subs]
            [work-to-list.client.views]))

(defn ^:export run []
  (re-frame/dispatch-sync [:initialize])
  (navigation/start!)
  (socket/start!)
  (reagent/render [work-to-list.client.views/app]
                  (.getElementById js/document "app")))
