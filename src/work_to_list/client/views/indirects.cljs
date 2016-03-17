(ns work-to-list.client.views.indirects
  (:require [re-frame.core :refer [subscribe dispatch]]
            [work-to-list.client.views.utils :as u]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Sub components

(defn ops-popup-entry
  [op-id]
  (let [op (subscribe [:operation op-id])]
    (fn [op-id]
      [:tr
       [:td.order (:order/id @op)]
       [:td.op (:op/number @op)]
       [:td.description (get-in @op [:order/part :part/description])]
       [:td.machine (get-in @op [:op/work-center :wc/id])]
       [:td.buffer
        [:span
         {:class (u/zone-class (:order/buffer-zone @op))}
         (int (:order/buffer-penetration @op))]]])))

(defn ops-popup
  [emp-id]
  (let [op-ids (subscribe [:current-emp-bookings emp-id])
        is-popped (subscribe [:popup-state :emp-ops emp-id])]
    (fn [emp-id]
      [:td
       {:class (if @is-popped "operations popover-active" "operations")
        :on-mouse-enter #(dispatch [:show-popup :emp-ops emp-id])
        :on-mouse-leave #(dispatch [:hide-popup :emp-ops emp-id])}
       (when (seq @op-ids)
         [:span (u/int-str (count @op-ids))])
       (when (seq @op-ids)
         [:table
          [:thead
           [:tr
            [:th.order "Order"]
            [:th.op "Op"]
            [:th.description "Description"]
            [:th.machine "MCH#"]
            [:th.buffer "Buffer"]]]
          [:tbody
           (for [op-id @op-ids]
             ^{:key (str emp-id "/" op-id)}
             [ops-popup-entry op-id])]])])))

(defn indirect-bookings-entry
  [booking]
  [:tr
   [:td.employee (u/employee-name (:booking/employee booking))]
   [:td.description (:indirect/description booking)]
   [:td.category (:indirect/category booking)]
   [:td.booked-at (u/time-str (:booking/started-at booking))]
   [ops-popup (:emp/id (:booking/employee booking))]])

(defn indirect-bookings-table
  [supervisor bookings]
  [:div.indirects
   [:h2 (u/employee-name supervisor)]
   [:table
    [:thead
     [:tr
      [:th.employee "Employee"]
      [:th.description "Indirect Code"]
      [:th.category "Category"]
      [:th.booked-at "Booked on at"]
      [:th.operations "On ops?"]]]
    [u/css-transition u/transition-opts
     (for [b bookings]
       ^{:key (str (-> b :booking/employee :emp/id) "/" (:op/id b))}
       [indirect-bookings-entry b])]]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main component

(defn indirect-bookings []
  (let [bookings (subscribe [:indirect-bookings])]
    (fn []
      (if (seq @bookings)
        (let [bbm (->> @bookings
                       (group-by #(get-in % [:booking/employee :emp/supervisor]))
                       (sort-by (fn [[s _]] (u/employee-name s))))]
          [:div
           [:h1 "Indirect Bookings by Supervisor"]
           (for [[s bs] bbm]
             ^{:key (:emp/id s)}
             [indirect-bookings-table s bs])])
        [:div.well
         [:h1 "No Active Indirects"]
         [:p "There are no employees currently booked on to indirect codes."]]))))
