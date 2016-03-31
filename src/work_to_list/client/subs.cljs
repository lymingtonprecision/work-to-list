(ns work-to-list.client.subs
  (:require-macros [reagent.ratom :refer [reaction]]
                   [taoensso.timbre :as log])
  (:require [re-frame.core :refer [register-sub subscribe]]
            [taoensso.timbre :as log]
            [work-to-list.filters
             :refer [group-type
                     group-id
                     op-group-id-path
                     order-by-penetration]]
            [work-to-list.client.util
             :refer [running-on-terminal?]]))

(def work-to-list-row-height 36.0)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utility fns

(defn pluralize [k]
  (-> (name k) (str "s") keyword))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Generic app state

(register-sub :db (fn [db _] db))

(register-sub
 :popup-state
 (fn [db [_ & path]]
   (let [popups (reaction (:popup-states @db))]
     (reaction (get-in @popups path)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Window sizing

(register-sub :window-size (fn [db _] (reaction (:window @db))))

(register-sub
 :max-ops-for-window
 (fn [db _]
   (if (running-on-terminal?)
     (let [win-size (subscribe [:window-size])]
       (reaction (js/Math.ceil (/ (:height @win-size)
                                  work-to-list-row-height))))
     (reaction nil))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Path

(register-sub :path            (fn [db _] (reaction (:path @db))))
(register-sub :list-type       (fn [db _] (reaction (get-in @db [:path 0]))))
(register-sub :active-group-id (fn [db _] (reaction (get-in @db [:path 1 :id]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Groups

(register-sub
 :active-group
 (fn [db _]
   (let [t (subscribe [:list-type])
         id (subscribe [:active-group-id])
         g (reaction (get-in @db [:db (pluralize @t) @id]))]
     (reaction
      (cond
        @g @g
        (= :work-centre @t) {:wc/id @id :wc/description @id}
        :else nil)))))

(register-sub
 :production-lines
 (fn [db _]
   (let [coll (reaction (get-in @db [:db :production-lines]))
         vs (reaction (vals @coll))]
     (reaction (sort-by :production-line/name @vs)))))

(register-sub
 :managers
 (fn [db _]
   (let [coll (reaction (get-in @db [:db :managers]))
         vs (reaction (vals @coll))]
     (reaction (sort-by :manager/name @vs)))))

(register-sub
 :terminals
 (fn [db _]
   (let [coll (reaction (get-in @db [:db :terminals]))
         vs (reaction (vals @coll))]
     (reaction (sort-by :terminal/id @vs)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Operations & Clockings

(register-sub
 :operations
 (fn [db _]
   (reaction (get-in @db [:db :operations]))))

(register-sub
 :operation
 (fn [db [_ id]]
   (reaction (get-in @db [:db :operations id]))))

(register-sub
 :clockings
 (fn [db [_ op-id]]
   (let [clockings (reaction (get-in @db [:db :clockings]))
         op-clockings (reaction
                       (reduce
                        (fn [rs [k v]]
                          (if (= op-id (first k))
                            (conj rs v)
                            rs))
                        []
                        @clockings))]
     (reaction (sort-by :booking/started-at @op-clockings)))))

(register-sub
 :current-booked-ops
 (fn [db _]
   (let [clockings (reaction (get-in @db [:db :clockings]))
         operation-ids (reaction (map first (keys @clockings)))
         unique-ops (reaction (distinct @operation-ids))]
     (reaction (set @unique-ops)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Filtered operation lists

(register-sub
 :current-bookings
 (fn [db _]
   (let [ops (subscribe [:operations])
         clocked-op-ids (subscribe [:current-booked-ops])
         f (reaction (let [op-ids @clocked-op-ids]
                       (fn [op] (contains? op-ids (:op/id op)))))
         filtered-ops (reaction (filter @f (vals @ops)))
         sorted-ops (reaction (order-by-penetration @filtered-ops))]
     (reaction (map :op/id @sorted-ops)))))

(register-sub
 :current-emp-bookings
 (fn [db [_ emp-id]]
   (let [clockings (reaction (get-in @db [:db :clockings]))
         indirects (reaction (get-in @db [:db :indirects]))
         emp-clockings
         (reaction
          (filter #(= emp-id (get-in % [:booking/employee :emp/id]))
                  (vals @clockings)))
         emp-op-clockings
         (reaction
          (remove #(contains? @indirects (:op/id %))
                  @emp-clockings))
         sorted-ops
         (reaction (sort-by :booking/started-at @emp-op-clockings))]
     (reaction (map :op/id @sorted-ops)))))

(register-sub
 :indirect-bookings
 (fn [db _]
   (let [indirects (reaction (get-in @db [:db :indirects]))
         clockings (reaction (get-in @db [:db :clockings]))
         indirect-clockings
         (reaction
          (reduce
           (fn [rs c]
             (if-let [i (get @indirects (:op/id c))]
               (conj rs (merge c i))
               rs))
           []
           (vals @clockings)))]
     (reaction (sort-by :booking/started-at @indirect-clockings)))))

(register-sub
 :work-to-list
 (fn [db _ [group limit]]
   (if group
     (let [id-path (op-group-id-path (group-type group))
           f (fn [op] (= (get-in op id-path) (group-id group)))
           ops (subscribe [:operations])
           filtered-ops (reaction (filter f (vals @ops)))
           sorted-ops (reaction (order-by-penetration @filtered-ops))
           ops-up-to-limit (if limit
                             (reaction (take limit @sorted-ops))
                             sorted-ops)]
       (reaction (map :op/id @ops-up-to-limit)))
     (reaction nil))))
