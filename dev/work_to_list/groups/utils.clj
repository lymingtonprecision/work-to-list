(ns work-to-list.groups.utils
  "Various utility fns useful when testing group related functionality like
  filtering."
  (:require [clojure.test.check.generators :as gen]))

(defn index-by
  "Returns a map of values in `coll` indexed by applying `f` to the value."
  [f coll]
  (reduce
   (fn [rs v]
     (assoc rs (f v) v))
   {}
   coll))

(defn op-group-ids
  "Returns the group IDs to which the given operation currently belongs."
  [op]
  (clojure.set/rename-keys
   (dissoc (:op/work-center op) :wc/description)
   {:wc/manager-id :manager/id
    :wc/terminal-id :terminal/id
    :wc/production-line-id :production-line/id}))

(defn group-ids
  "Returns a collection of the group ids present in the given collection of
  operations.

  The returned collection will be a map from group ID keys to a list of values
  present under that key, e.g.:

      {:manager/id [...manager ids...]
       :terminal/id [...terminal ids...]
       :production-line/id [...production line ids...]}"
  [operations]
  (reduce
   (fn [rs op]
     (reduce
      (fn [rs [k v]]
        (update rs k conj v))
      rs
      (op-group-ids op)))
   {}
   operations))

(defn rand-string
  "Returns a random string."
  []
  (first (gen/sample (gen/resize (rand-int 20) gen/string-ascii))))

(defn hydrate-groups
  "Given a collection of group IDs, as returned by `group-ids`, returns a map of
  fully populated group records using those IDs suitable for merging into a work
  to list.

      (hydrate-groups {:manager/id [\"5-Y\" \"4-X\"]
                       :terminal/id [\"1\" \"7\"]})
      ;=> {:managers {\"5-Y\" {:manager/id \"5-Y\" :manager/name \"+4V\"}
      ;;;             \"4-X\" {:manager/id \"4-X\" :manager/name \"&8_BK'pI]\"}}
      ;;;  :terminals {\"1\" {:terminal/id \"1\" :terminal/name \"mc?+-P|\"}
      ;;;              \"7\" {:terminal/id \"7\" :terminal/name \"Hd)ss4qhU4eIf\"}}}"
  [group-ids]
  (reduce
   (fn [rs [k ids]]
     (assoc
      rs
      (-> k namespace (str "s") keyword)
      (reduce
       (fn [rs id]
         (assoc
          rs id
          {k id
           (-> k namespace (str "/name") keyword) (rand-string)}))
       {}
       ids)))
   {}
   group-ids))

(defn contains-group?
  "Returns truthy if the given `group` is referenced by any of the `operations`
  in the given collection.

  (Returns a `fn` that can be called with a group against a cached list of the
  groups from `operations` when only given a list of operations.)"
  ([operations]
   (let [ids (group-ids operations)]
     (fn [g]
       (let [[k v] (some (fn [[k v]] (when (= "id" (name k)) [k v])) g)]
         (some #(= v %) (get ids k))))))
  ([operations group]
   ((contains-group? operations) group)))
