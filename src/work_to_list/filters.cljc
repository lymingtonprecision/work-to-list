(ns work-to-list.filters
  (:require [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utility fns

(defn operations
  [work-to-list-or-ops]
  (let [ops (:operations work-to-list-or-ops work-to-list-or-ops)]
    (if (map? ops)
      (vals ops)
      ops)))

(def group-types
  #{:manager :terminal :production-line :wc})

(defn group-id-key
  [group]
  (some (fn [k] (when (= (name k) "id") k)) (keys group)))

(defn group-type
  [group]
  (some->> group group-id-key namespace keyword))

(defn op-group-id-path
  [group-type]
  (case group-type
    :manager [:op/work-center :wc/manager-id]
    :terminal [:op/work-center :wc/terminal-id]
    :production-line [:op/work-center :wc/production-line-id]
    :work-centre [:op/work-center :wc/id]
    :wc [:op/work-center :wc/id]))

(def group-id-fields
  (set (map #(-> (name %) (str "/id") keyword) group-types)))

(def group-name-fields
  (conj (set (map #(-> (name %) (str "/name") keyword) group-types))
        :wc/description))

(defn group-id
  [group]
  (some group group-id-fields))

(defn group-name
  [group]
  (some group group-name-fields))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Quasi-Public, should probably be in a different namespace

(defn order-by-penetration
  [work-to-list]
  (->> (operations work-to-list)
       (sort-by :order/buffer-penetration)
       reverse))

(defn sort-groups-by-name
  [group-ids]
  (let [group-ids (if (map? group-ids) (vals group-ids) group-ids)]
    (sort-by group-name group-ids)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn current-bookings
  [{:keys [operations clockings] :as work-to-list}]
  (->> (keys clockings)
       (map first)
       distinct
       (select-keys operations)
       order-by-penetration))

(defn operations-for-group
  [work-to-list group]
  (let [group-id (group-id group)
        id-path (op-group-id-path (group-type group))]
    (->> (operations work-to-list)
         (filter #(= (get-in % id-path) group-id))
         order-by-penetration)))

(defn operations-for-work-center
  [work-to-list wc-id]
  (let [wc (if (map? wc-id) wc-id {:wc/id wc-id})]
    (operations-for-group work-to-list wc)))
