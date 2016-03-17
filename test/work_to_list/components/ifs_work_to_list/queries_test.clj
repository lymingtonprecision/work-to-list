(ns work-to-list.components.ifs-work-to-list.queries-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [schema.test]
            [work-to-list.schema :as schema]
            [work-to-list.components.ifs-work-to-list.queries :as queries]))

(use-fixtures :once schema.test/validate-schemas)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; coerce-group

(deftest coerce-group-manager
  (let [r {:type "manager" :id "4" :name "Production Manager"}
        exp {:manager/id "4" :manager/name "Production Manager"}]
    (is (= exp (schema/validate schema/Manager (queries/coerce-group r))))))

(deftest coerce-group-terminal
  (let [r {:type "terminal" :id "4" :name "Bay 3"}
        exp {:terminal/id "4" :terminal/name "Bay 3"}]
    (is (= exp (schema/validate schema/Terminal (queries/coerce-group r))))))

(deftest coerce-group-production-line
  (let [r {:type "production-line" :id "4" :name "Bay 3"}
        exp {:production-line/id "4" :production-line/name "Bay 3"}]
    (is (= exp (schema/validate schema/ProductionLine (queries/coerce-group r))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; group-by-type

(deftest group-by-type-empty-set
  (is (empty? (queries/group-by-type nil))))

(deftest group-by-type-single-type
  (let [rs [{:type "terminal" :id "1" :name "Terminal 1"}
            {:type "terminal" :id "2" :name "Terminal 2"}]
        exp {:terminals
             {"1" {:terminal/id "1" :terminal/name "Terminal 1"}
              "2" {:terminal/id "2" :terminal/name "Terminal 2"}}}]
    (is (= exp (queries/group-by-type rs)))))

(deftest group-by-type-multiple-types
  (let [rs [{:type "manager" :id "1" :name "Manager 1"}
            {:type "terminal" :id "1" :name "Terminal 1"}
            {:type "terminal" :id "2" :name "Terminal 2"}
            {:type "production-line" :id "2" :name "PL 2"}
            {:type "manager" :id "2" :name "Manager 2"}
            {:type "production-line" :id "4" :name "PL 4"}]
        exp {:managers
             {"1" {:manager/id "1" :manager/name "Manager 1"}
              "2" {:manager/id "2" :manager/name "Manager 2"}}
             :terminals
             {"1" {:terminal/id "1" :terminal/name "Terminal 1"}
              "2" {:terminal/id "2" :terminal/name "Terminal 2"}}
             :production-lines
             {"2" {:production-line/id "2" :production-line/name "PL 2"}
              "4" {:production-line/id "4" :production-line/name "PL 4"}}}]
    (is (= exp (queries/group-by-type rs)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; coerce-op-clocking-row

(deftest coerce-op-clocking-row
  (let [ts (java.util.Date.)
        r {:op_id 123456M
           :started_at ts
           :employee_id "10037"
           :employee_given_name "Andy"
           :employee_family_name "Worker"
           :supervisor_id "10007"
           :supervisor_given_name "Bob"
           :supervisor_family_name "Manager"}
        exp {:op/id 123456
             :booking/started-at ts
             :booking/employee
             {:emp/id "10037"
              :emp/given-name "Andy"
              :emp/family-name "Worker"
              :emp/supervisor
              {:emp/id "10007"
               :emp/given-name "Bob"
               :emp/family-name "Manager"}}}]
    (is (= exp (queries/coerce-op-clocking-row r)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; coerce-work-to-entry

(deftest coerce-work-to-entry
  (let [r {:op_id 123456M
           :op_no 10M
           :op_description "Machining"
           :order_id "204859-*-*"
           :order_no "204859"
           :order_release "*"
           :order_sequence "*"
           :buffer_zone "blue"
           :buffer_penetration -3859.0M
           :part_id "100123456R01"
           :part_drawing_number "D7894"
           :part_issue "AB"
           :part_description "Machined Part to Customer Drawing"
           :planner "ASCHULTZ"
           :qty 5.0M
           :qty_avail 4.0M
           :work_center_id "MC019"
           :work_center_description "Milling"
           :production_line_id "4"
           :manager_id "5-Y"
           :terminal_id "1"}
        exp {:order/id "204859-*-*"
             :order/number "204859"
             :order/release "*"
             :order/sequence "*"
             :order/part
             {:part/id "100123456R01"
              :part/drawing-number "D7894"
              :part/drawing-issue "AB"
              :part/description "Machined Part to Customer Drawing"
              :part/planner-id "ASCHULTZ"}
             :order/buffer-zone :blue
             :order/buffer-penetration -3859.0
             :op/id 123456
             :op/number 10
             :op/description "Machining"
             :op/qty 5
             :op/qty-available 4
             :op/work-center
             {:wc/id "MC019"
              :wc/description "Milling"
              :wc/production-line-id "4"
              :wc/manager-id "5-Y"
              :wc/terminal-id "1"}}]
    (is (= exp (queries/coerce-work-to-entry r)))))
