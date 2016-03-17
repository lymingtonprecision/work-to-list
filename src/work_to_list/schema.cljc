(ns work-to-list.schema
  (:require [schema.core :as s]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utility fns

(defn optional->mandatory
  [schema k]
  (let [k (s/optional-key k)
        v (get schema k)]
    (-> schema (dissoc k) (assoc (:k k) v))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Predicates

(def pos-number
  (s/pred #(and (number? %) (pos? %))
          'pos-number))

(def pos-int
  (s/pred #(and (integer? %) (pos? %))
          'pos-int))

(def zero-or-pos-number
  (s/pred #(and (number? %) (>= % 0))
          'zero-or-pos-number))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Elements

(def OpID pos-int)
(def OrderID s/Str)
(def PartID s/Str)
(def WorkCenterID s/Str)
(def EmployeeID s/Str)
(def ManagerID s/Str)
(def TerminalID s/Str)
(def ProductionLineID s/Str)

(def ClockingKey
  [(s/one OpID "Op ID")
   (s/one EmployeeID "Employee ID")
   (s/one s/Inst "Timestamp")])

(def BufferZone
  (s/enum
   :blue
   :green
   :yellow
   :red
   :black))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Records

(def Employee
  {:emp/id EmployeeID
   :emp/given-name s/Str
   :emp/family-name s/Str
   (s/optional-key :emp/supervisor) (s/recursive #'Employee)})

(def EmployeeWithSupervisor
  (optional->mandatory Employee :emp/supervisor))

(def Clocking
  {:op/id OpID
   :booking/started-at s/Inst
   :booking/employee EmployeeWithSupervisor})

(def Manager
  {:manager/id ManagerID
   :manager/name s/Str})

(def Terminal
  {:terminal/id TerminalID
   :terminal/name s/Str})

(def ProductionLine
  {:production-line/id ProductionLineID
   :production-line/name s/Str})

(def Part
  {:part/id PartID
   :part/drawing-number s/Str
   :part/drawing-issue s/Str
   :part/description s/Str
   :part/planner-id s/Str})

(def WorkCenter
  {:wc/id WorkCenterID
   :wc/description s/Str
   :wc/production-line-id ProductionLineID
   :wc/manager-id ManagerID
   :wc/terminal-id TerminalID})

(def Indirect
  {:op/id OpID
   :indirect/code s/Str
   :indirect/description s/Str
   :indirect/category s/Str})

(def Operation
  {:op/id OpID
   :op/number pos-int
   :op/description s/Str
   :op/qty pos-number
   :op/qty-available zero-or-pos-number
   :op/work-center WorkCenter
   :order/id OrderID
   :order/number s/Str
   :order/release s/Str
   :order/sequence s/Str
   :order/part Part
   :order/buffer-zone BufferZone
   :order/buffer-penetration s/Num})

(def WorkToList
  {:operations {OpID Operation}
   :clockings {ClockingKey Clocking}
   :indirects {OpID Indirect}
   :managers {ManagerID Manager}
   :terminals {TerminalID Terminal}
   :production-lines {ProductionLineID ProductionLine}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Coercion/derivation

(s/defn clocking-key :- ClockingKey
  "Returns the key that should be used for the given clocking to uniquely
  identify it within an associative collection."
  [c :- Clocking]
  [(:op/id c)
   (get-in c [:booking/employee :emp/id])
   (:booking/started-at c)])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Expose Schema fns

(def check s/check)
(def validate s/validate)
