(ns work-to-list.components.ifs-work-to-list
  (:require [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [work-to-list.schema :as schema]
            [work-to-list.protocols.work-to-list :refer [IWorkToList]]
            [work-to-list.components.ifs-work-to-list.queries :as queries]))

(def ^:dynamic *default-poll-interval-ms* (* 10 1000))

(defn index-by [f coll]
  (reduce
   (fn [rs v]
     (assoc rs (f v) v))
   {}
   coll))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Implementation fns

(defn blank-state
  "Returns a new, empty, work to list state atom."
  []
  (atom {}))

(s/defn work-to-list :- schema/WorkToList
  "Returns a map of the `:operations` that are currently on the work to list and
  their associated `:clockings` and groups (each under a key corresponding to
  the group name, e.g. `:terminals` for the current SFDC terminals.)

  `db-spec` should be a JDBC database connection or connection specification for
  the IFS database from which to retrieve the work to list entries.

  Returns `nil` if any of the dependent queries fails.''"
  [db-spec]
  (let [[groups
         indirects
         clockings
         operations
         :as results]
        (pvalues (queries/groups db-spec)
                 (queries/indirect-codes db-spec)
                 (queries/open-op-clockings db-spec)
                 (queries/work-to-list db-spec))]
    (when (not-any? nil? results)
      (merge
       {:operations (index-by :op/id operations)
        :clockings (index-by schema/clocking-key clockings)
        :indirects (index-by :op/id indirects)}
       groups))))

(defn poll!
  "Returns a channel onto which the current value of the work to list retrieved
  from the specified database will be put every `interval-ms` milliseconds.

  The channel is configured with a single sliding buffer slot and so will only
  every contain the most recent value of the work to list.

  Closing the returned channel will stop the polling process."
  ([db-spec]
   (poll! db-spec *default-poll-interval-ms*))
  ([db-spec interval-ms]
   (let [wtl-ch (async/chan (async/sliding-buffer 1))]
     (async/go-loop []
       (let [wtl (try
                   (work-to-list db-spec)
                   (catch Exception e
                     (log/error e "error polling database")))]
         (when (or (nil? wtl) (async/>! wtl-ch wtl))
           (async/<! (async/timeout interval-ms))
           (when-not (.closed? wtl-ch) (recur)))))
     wtl-ch)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Component

(defrecord IfsWorkToList [state db-spec]
  component/Lifecycle
  (start [this]
    (if (:update-ch this)
      this
      (let [update-ch (poll! db-spec)]
        (reset! state (async/<!! update-ch))
        (async/go-loop []
          (when-let [wtl (async/<! update-ch)]
            (reset! state wtl)
            (recur)))
        (assoc this :update-ch update-ch))))
  (stop [this]
    (when-let [ch (:update-ch this)]
      (async/close! ch))
    (dissoc this :update-ch))

  IWorkToList
  (tasks [this] @(:state this))
  (add-watch [this key fn] (add-watch (:state this) key fn))
  (remove-watch [this key] (remove-watch (:state this) key))
  (reset! [this] (reset! (:state this) (work-to-list db-spec))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn ifs-work-to-list
  ([]
   (map->IfsWorkToList {:state (blank-state)}))
  ([db-spec]
   (->IfsWorkToList (blank-state) db-spec)))
