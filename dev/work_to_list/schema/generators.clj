(ns work-to-list.schema.generators
  (:require [clojure.test.check.generators :as gen]
            [schema.experimental.complete :as schema.complete]
            [schema.experimental.generators :as schema.gen]
            [work-to-list.components.ifs-work-to-list.queries :refer [coerce-group]]
            [work-to-list.groups.utils :refer :all]
            [work-to-list.schema :as schema]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Basic element generators

(def pos-int
  (gen/such-that pos? gen/pos-int))

(def real-double
  (gen/double* {:infinite? false :NaN? false}))

(def pos-number
  (gen/such-that pos? (gen/one-of [gen/pos-int real-double])))

(def zero-or-pos-number
  (gen/such-that pos? (gen/one-of [gen/pos-int real-double])))

(def leaf-generators
  {schema/pos-int pos-int
   schema/pos-number pos-number
   schema/zero-or-pos-number zero-or-pos-number})

(def manager (schema.gen/generator schema/Manager))
(def terminal (schema.gen/generator schema/Terminal))
(def production-line (schema.gen/generator schema/ProductionLine))
(def clocking (schema.gen/generator schema/Clocking leaf-generators))
(def operation (schema.gen/generator schema/Operation leaf-generators))

(def group
  (gen/fmap
   coerce-group
   (gen/hash-map
    :type (gen/elements [:manager :terminal :production-line])
    :id (gen/not-empty gen/string-ascii)
    :name gen/string-ascii)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Collection generators

(def operations
  (gen/fmap
   #(index-by :op/id %)
   (gen/vector-distinct-by :op/id operation)))

(defn clockings-for-ops [ops]
  (gen/let [n (gen/choose 0 (count ops))
            clk (gen/vector-distinct-by
                 :op/id
                 (schema.gen/generator
                  schema/Clocking
                  (merge
                   leaf-generators
                   {schema/OpID (gen/elements (keys ops))}))
                 {:max-elements n})]
    (index-by #(schema/clocking-key %) clk)))

(defn groups-from-ops [ops]
  (hydrate-groups (group-ids (vals ops))))

(def ops-with-clockings
  (gen/let [ops (gen/not-empty operations)
            clk (clockings-for-ops ops)]
    {:operations ops
     :clockings clk}))

(def ops-with-groups
  (gen/let [ops (gen/not-empty operations)]
    (let [groups (groups-from-ops ops)]
      (assoc groups :operations ops))))

(def work-to-list
  (gen/let [ops (gen/not-empty operations)
            clk (clockings-for-ops ops)]
    (let [groups (groups-from-ops ops)]
      (merge
       {:operations ops
        :clockings clk}
       (dissoc groups :wcs)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Expose useful Schema fns

(def generator
  schema.gen/generator)

(defn complete
  [partial schema]
  (schema.complete/complete partial schema {} leaf-generators))
