(ns work-to-list.client.views.work-to-list
  (:require [clojure.string :as string]
            [re-frame.core :refer [subscribe dispatch]]
            [work-to-list.filters :refer [group-type group-id group-name]]
            [work-to-list.client.navigation :refer [href]]
            [work-to-list.client.views.utils :as u]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Sub components

(defn group-header
  [group]
  (when (and (some? group) (not= :terminal (group-type group)))
    [:h1 (str (u/titleize (group-name group)) " Work To List")]))

(defn bookings-popup
  [op-id]
  (let [clockings (subscribe [:clockings op-id])
        is-popped (subscribe [:popup-state :bookings op-id])]
    (fn [op-id]
      [:td
       {:class (if @is-popped "bookings popover-active" "bookings")
        :on-mouse-enter #(dispatch [:show-popup :bookings op-id])
        :on-mouse-leave #(dispatch [:hide-popup :bookings op-id])}
       (when (seq @clockings)
         [:i {:class (if (> (count @clockings) 1) "fa fa-users" "fa fa-user")}])
       (when (seq @clockings)
         [:table
          [:thead
           [:tr
            [:th.employee "Employee"]
            [:th.supervisor "Supervisor"]
            [:th.booked-at "Booked on at"]]]
          [:tbody
           (for [c @clockings]
             ^{:key (str "clocking/" op-id "/" (get-in c [:booking/employee :emp/id]))}
             [:tr
              [:td.employee (u/employee-name (:booking/employee c))]
              [:td.supervisor (u/employee-name (get-in c [:booking/employee :emp/supervisor]))]
              [:td.booked-at (u/time-str (:booking/started-at c))]])]])])))

(defn operation-entry
  [op-id]
  (let [op (subscribe [:operation op-id])]
    (fn [op-id]
      [:tr {:class (u/zone-class (:order/buffer-zone @op))}
       [:td.order (:order/id @op)]
       [:td.op (:op/number @op)]
       [:td.part (u/part-number (:order/part @op))]
       [:td.description (get-in @op [:order/part :part/description])]
       [:td.qty (:op/qty @op)]
       [:td.qty (- (:op/qty @op) (:op/qty-available @op))]
       [:td.work-center (get-in @op [:op/work-center :wc/description])]
       [:td.machine
        (if-let [id (get-in @op [:op/work-center :wc/id])]
          [:a {:href (href :work-centre :id id)} id])]
       [:td.terminal
        (if-let [id (get-in @op [:op/work-center :wc/terminal-id])]
          [:a {:href (href :terminal :id id)} id]
          "N/A")]
       [bookings-popup op-id]])))

(defn operation-list
  [operation-ids]
   [:table
    [:thead
     [:tr
      [:th.order        "Order"]
      [:th.op           "Op"]
      [:th.part         "Part"]
      [:th.description  "Description"]
      [:th.qty          "Qty"]
      [:th.qty          "OS"]
      [:th.work-center  "Work Center"]
      [:th.machine      "MCH#"]
      [:th.terminal     "T#"]
      [:td.bookings     ""]]]
    [u/css-transition u/transition-opts
     (for [op-id @operation-ids]
       ^{:key (str "operation/" op-id)}
       [operation-entry op-id])]])

(defn empty-work-queue []
  [:div.well
   [:h1 "No Orders in Queue"]
   [:p "There are no orders currently awaiting processing at machines in this area."]
   [:p "See your line manager for further instruction."]])

(defn invalid-group []
  (let [type (subscribe [:list-type])
        id (subscribe [:active-group-id])]
    (fn []
      [:div.well
       [:h1 (str "No Such " (u/kw->title @type))]
       [:p (str "There is no " (-> @type u/kw->title string/lower-case) " with ID ‘" @id "’.")]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main components

(defn current-bookings []
  (let [operations (subscribe [:current-bookings])]
    (fn []
      (if (seq @operations)
        [:div {:class "work-to-list show-terminal"}
         [:h1 "Current Bookings"]
         [operation-list operations]]
        [:div.well
         [:h1 "No Active Bookings"]
         [:p "There are no employees currently booked on to shop order operations."]]))))

(defn work-to-list []
  (let [group (subscribe [:active-group])
        operations (subscribe [:work-to-list] [group])]
    (fn []
      (if (nil? @group)
        [invalid-group]
        ^{:key (str (name (group-type @group)) "/" (group-id @group))}
        [:div {:class (if (= :terminal (group-type @group)) "work-to-list" "work-to-list show-terminal")}
         (group-header @group)
         (if (seq @operations)
           [operation-list operations]
           [empty-work-queue])]))))
