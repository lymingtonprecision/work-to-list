(ns work-to-list.protocols.work-to-list
  (:refer-clojure :exclude [add-watch remove-watch reset!]))

(defprotocol IWorkToList
  (tasks [this])
  (add-watch [this key fn])
  (remove-watch [this key])
  (reset! [this]))
