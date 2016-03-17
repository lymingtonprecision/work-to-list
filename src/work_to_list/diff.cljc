(ns work-to-list.diff
  (:require [clojure.data :as data]
            [clojure.set :as set]
            [meta-merge.core :refer [meta-merge]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utility fns

(defn diff
  "Returns a tuple of `[removed added modified]` where:

  * `removed` is a collection of the keys present in `old` not in `new`
  * `added` is the subset of `new` that is not in `old`
  * `modified` is a map of keys to [meta merge]able diffs for those
    entries in both `old` and `new` whose values differ

  [meta merge]: https://github.com/weavejester/meta-merge"
  [old new]
  {:pre [(map? old) (map? new)]}
  (let [[old-keys new-keys] (map (comp set keys) [old new])
        removed (set/difference old-keys new-keys)
        added (select-keys new (set/difference new-keys old-keys))
        in-both (set/intersection old-keys new-keys)
        modified (second
                  (data/diff
                   (select-keys old in-both)
                   (select-keys new in-both)))]
    [removed added modified]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn unified-diff
  "Returns a collection of the diffs made to `new-work-to` since `old-work-to`
  where each entry is one of the following variants:

  * `[:remove group key]`
  * `[:add group key value]`
  * `[:update group key meta-merge-diff]`

  The `group`s in the above variants correspond to the root keys in the work to
  list (`:operations`, `:clockings`, `:managers`, `:terminals`, and
  `:production-lines`.) Only those groups present in both work to lists are
  diffed.

  The `meta-merge-diff`s in the `:update` entries are record diffs that can be
  applied to the existing value via [meta-merge] to update it.

  [meta-merge]: https://github.com/weavejester/meta-merge"
  [old-work-to new-work-to]
  (mapcat
   (fn [k]
     (let [[removed added modified] (diff (k old-work-to) (k new-work-to))]
       (concat
        (map (fn [id] [:remove k id]) removed)
        (map (fn [[id val]] [:add k id val]) added)
        (map (fn [[id diff]] [:update k id diff]) modified))))
   (apply
    set/intersection
    (map
     (comp set keys)
     [old-work-to
      new-work-to]))))

(defn patch
  "Returns the result of applying the `updates` (as returned by `unified-diff`)
  to `work-to-list`."
  [work-to-list updates]
  (reduce
   (fn [wtl upd]
     (case (first upd)
       :add (assoc-in wtl (subvec upd 1 3) (last upd))
       :remove (update wtl (second upd) dissoc (last upd))
       :update (update-in wtl (subvec upd 1 3) meta-merge (last upd))))
   work-to-list
   updates))
