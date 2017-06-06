(ns work-to-list.components.ifs-work-to-list.queries
  (:require [clojure.string :as str]
            [schema.core :as s]
            [yesql.core :as yesql :refer [defquery]]
            [work-to-list.schema :as schema]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Raw queries

(defquery -groups "work_to_list/sql/groups.sql")
(defquery -work-to-list "work_to_list/sql/work_to_list.sql")
(defquery -indirect-codes "work_to_list/sql/indirect_codes.sql")
(defquery -open-op-clockings "work_to_list/sql/open_op_clockings.sql")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Row/result set wrangling

(defn coerce-group
  [{type :type :as r}]
  {(keyword (str (name type) "/id")) (:id r)
   (keyword (str (name type) "/name")) (:name r)})

(defn coerce-indirect-code
  [r]
  {:op/id (int (:id r))
   :indirect/code (:code r)
   :indirect/description (:description r)
   :indirect/category (:category r)})

(defn pluralize [t]
  (-> t name (str "s") keyword))

(defn group-by-type [rs]
  (reduce
   (fn [rs r]
     (assoc-in rs [(pluralize (:type r)) (:id r)] (coerce-group r)))
   {}
   rs))

(s/defn coerce-op-clocking-row :- schema/Clocking
  [r]
  (let [supervisor
        {:emp/id (:supervisor_id r)
         :emp/given-name (:supervisor_given_name r)
         :emp/family-name (:supervisor_family_name r)}
        employee
        {:emp/id (:employee_id r)
         :emp/given-name (:employee_given_name r)
         :emp/family-name (:employee_family_name r)
         :emp/supervisor supervisor}]
    {:op/id (int (:op_id r))
     :booking/started-at (:started_at r)
     :booking/employee employee}))

(s/defn coerce-work-to-entry :- schema/Operation
  [r]
  (let [part
        {:part/id (:part_id r)
         :part/drawing-number (:part_drawing_number r)
         :part/drawing-issue (:part_issue r)
         :part/description (:part_description r)
         :part/planner-id (:planner r)}
        work-center
        {:wc/id (:work_center_id r)
         :wc/description (:work_center_description r)
         :wc/production-line-id (:production_line_id r)
         :wc/manager-id (:manager_id r)
         :wc/terminal-id (:terminal_id r)}]
    {:order/id (:order_id r)
     :order/number (:order_no r)
     :order/release (:order_release r)
     :order/sequence (:order_sequence r)
     :order/part part
     :order/buffer-zone (some-> (:buffer_zone r) str/lower-case keyword)
     :order/buffer-penetration (double (:buffer_penetration r))
     :op/id (int (:op_id r))
     :op/number (int (:op_no r))
     :op/description (:op_description r)
     :op/qty (int (:qty r))
     :op/qty-available (int (:qty_avail r))
     :op/work-center work-center}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn groups
  [db-spec]
  (-groups
   {}
   {:connection db-spec
    :row-fn (fn [r] (update r :type keyword))
    :result-set-fn (fn [rs] (doall (group-by-type rs)))}))

(defn open-op-clockings
  [db-spec]
  (-open-op-clockings
   {}
   {:connection db-spec
    :row-fn coerce-op-clocking-row}))

(defn indirect-codes
  [db-spec]
  (-indirect-codes
   {}
   {:connection db-spec
    :row-fn coerce-indirect-code}))

(defn work-to-list
  [db-spec]
  (-work-to-list
   {}
   {:connection db-spec
    :row-fn coerce-work-to-entry}))
