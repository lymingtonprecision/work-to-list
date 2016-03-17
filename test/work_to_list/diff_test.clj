(ns work-to-list.diff-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [work-to-list.schema :as schema]
            [work-to-list.schema.generators :as schema.gen]
            [work-to-list.diff :as diff]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Alteration fns

(defn safe-keys-to-update [group]
  (case group
    :operations [:op/description :op/qty :op/qty-available
                 :order/buffer-zone :order/buffer-penetration]
    :managers [:manager/name]
    :terminals [:terminal/name]
    :production-lines [:production-line/name]))

(defn update-random-value [r group]
  (let [k (rand-nth (safe-keys-to-update group))
        v (if (string? (get r k))
            (gen/generate gen/string-ascii 10)
            (gen/generate gen/pos-int 10))]
    (assoc r k v)))

(defn new-value [group]
  (gen/generate
   (case group
     :operations schema.gen/operation
     :clockings schema.gen/clocking
     :managers schema.gen/manager
     :terminals schema.gen/terminal
     :production-lines schema.gen/production-line)
   10))

(defn index-key [group r]
  (case group
    :operations (:op/id r)
    :clockings (schema/clocking-key r)
    :managers (:manager/id r)
    :terminals (:terminal/id r)
    :production-lines (:production-line/id r)))

(defn add-to-group [wtl group]
  (let [v (new-value group)
        k (index-key group v)]
    (assoc-in wtl [group k] v)))

(defn apply-alteration [wtl group k op]
  (case op
    :add (add-to-group wtl group)
    :remove (update wtl group dissoc k)
    :update (update-in wtl [group k] update-random-value group)))

(defn randomly-alter [wtl]
  (let [iterations (rand-nth (range 1 21))]
    (loop [wtl wtl
           i iterations]
      (let [group (rand-nth (keys wtl))
            element (rand-nth (keys (get wtl group)))
            op (rand-nth (if (= :clockings group)
                           [:add :remove]
                           [:add :remove :update]))
            wtl (apply-alteration wtl group element op)]
        (if (zero? i)
          wtl
          (recur wtl (dec i)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tests

(defspec unified-diff-and-patch
  (prop/for-all [wtl schema.gen/work-to-list]
    (let [changed (randomly-alter wtl)
          diff (diff/unified-diff wtl changed)
          patched (diff/patch wtl diff)]
      (is (and (not= wtl changed)
               (= changed patched))))))
