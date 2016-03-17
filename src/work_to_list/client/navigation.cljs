(ns work-to-list.client.navigation
  (:require [pushy.core :as pushy]
            [re-frame.core :refer [dispatch]]
            [work-to-list.routes :as routes]
            [work-to-list.client.util :refer [terminal-id]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Routing fn

(defn update-path
  [{:keys [handler route-params] :as route}]
  (let [path (if (seq route-params)
               [handler route-params]
               [handler])]
    (dispatch [:update-path path])
    (js/window.scrollTo 0 0)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public references

(def app-routes
  (routes/frontend js/BASE_URL))

(def history
  (pushy/pushy update-path (partial routes/match-route app-routes)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public fns

(defn current-route []
  (routes/match-route app-routes (pushy/get-token history)))

(defn href
  [& path]
  (apply routes/path-for app-routes path))

(defn navigate-to
  [& path]
  (pushy/set-token! history (apply href path)))

(defn redirect-when-terminal []
  (when-let [id (when (= :index (:handler (current-route)))
                  (terminal-id))]
    (navigate-to :terminal :id id)))

(defn start! []
  (pushy/start! history)
  (redirect-when-terminal))
