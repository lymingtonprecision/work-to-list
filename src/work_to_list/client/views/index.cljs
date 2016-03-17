(ns work-to-list.client.views.index
  (:require [re-frame.core :refer [subscribe]]
            [work-to-list.filters :refer [group-id group-name]]
            [work-to-list.client.navigation :refer [href]]
            [work-to-list.client.views.utils :as u]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Sub components

(defn group-list-entry [group item]
  ^{:key (str group "/" (group-id item))}
  [:li [:a {:href (href (u/unpluralize group) :id (group-id item))} (group-name item)]])

(defn group-list
  ([group]
   (group-list group {}))
  ([group {:keys [compact] :as opts}]
   (let [entries (subscribe [group])]
     (fn [group opts]
       (let [css-class (if compact "terminals compact" "terminals")]
         (when (seq @entries)
           [:div
            [:h2 {} (u/kw->title group)]
            [:ol {:class css-class}
             (doall
              (for [v @entries]
                (group-list-entry group v)))]]))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main component

(defn index []
  [:div
   [:h1 "Drum Buffer Rope Prioritized Production"]
   [:ol.terminals
    [:li.current
     [:a {:href (href :current-bookings)} "Current Bookings"]]
    [:li.current
     [:a {:href (href :indirect-bookings)} "Indirect Bookings"]]]
   [group-list :production-lines]
   [group-list :managers]
   [group-list :terminals {:compact true}]])
