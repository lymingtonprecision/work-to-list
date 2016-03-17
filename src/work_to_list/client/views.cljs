(ns work-to-list.client.views
  (:require [re-frame.core :refer [subscribe]]
            [work-to-list.client.navigation :refer [href]]
            [work-to-list.client.views.index :refer [index]]
            [work-to-list.client.views.indirects :refer [indirect-bookings]]
            [work-to-list.client.views.work-to-list
             :refer [work-to-list current-bookings]]))

(defn inspector []
  (let [db (subscribe [:db])
        list-type (subscribe [:list-type])
        group-id (subscribe [:active-group-id])
        group (subscribe [:active-group])]
    (fn []
      ^{:key "inspector"}
      [:div.inspector
       {:style {:position :fixed :bottom "0px" :left "0px"
                :width "100%"
                :padding "5px 10px"
                :color "#eee"
                :background "rgba(100, 100, 100, 0.5)"}}
       [:dl
        [:dt "Path"]
        [:dd (pr-str {:path (:path @db)
                      :list-type @list-type
                      :group-id @group-id
                      :group @group})]
        [:dt "Popups"]
        [:dd (pr-str (:popup-states @db))]]])))

(defn app []
  (let [path (subscribe [:path])]
    (fn []
      [:div.wrapper
       [:a.fa.fa-home {:href (href :index)}]
       (case (first @path)
         :index [index]
         :current-bookings [current-bookings]
         :indirect-bookings [indirect-bookings]
         [work-to-list])])))
