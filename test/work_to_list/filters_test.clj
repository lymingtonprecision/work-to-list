(ns work-to-list.filters-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [work-to-list.schema.generators :as schema.gen]
            [work-to-list.groups.utils :refer [contains-group?]]
            [work-to-list.filters :as filters]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current-bookings

(defspec current-bookings-with-no-clockings
  (prop/for-all [ops schema.gen/operations]
    (let [wtl {:operations ops :clockings {}}]
      (is (nil? (seq (filters/current-bookings wtl)))))))

(defspec current-bookings-spec
  (prop/for-all [wtl schema.gen/ops-with-clockings]
    (let [op-ids (map (fn [[[op _ _] _]] op) (:clockings wtl))
          ops (vals (select-keys (:operations wtl) op-ids))
          sorted-ops (reverse (sort-by :order/buffer-penetration ops))]
      (is (= sorted-ops (filters/current-bookings wtl))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; operations-for-group

(defspec operations-for-unused-group
  (prop/for-all [[ops grp] (gen/let [ops schema.gen/operations
                                     g (gen/such-that
                                        #(not (contains-group? (vals ops) %))
                                        schema.gen/group)]
                             [ops g])]
    (is (nil? (seq (filters/operations-for-group {:operations ops} grp))))))

(defspec operations-for-group-spec
  (prop/for-all
    [[wtl grp]
     (gen/let [{:keys [operations
                       managers
                       terminals
                       production-lines]
                :as wtl} schema.gen/ops-with-groups
               grp (gen/elements
                    (flatten
                     (map vals [managers terminals production-lines])))]
       [wtl grp])]
    (is (seq (filters/operations-for-group wtl grp)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; operations-for-work-center

(defspec operations-for-work-center-spec
  (prop/for-all
    [[wtl wc]
     (gen/let [{:keys [operations wcs] :as wtl} schema.gen/ops-with-groups
               wc (gen/elements (vals wcs))]
       [wtl wc])]
    (is (seq (filters/operations-for-work-center wtl wc)))))
