(ns work-to-list.middleware.not-found
  (:require [ring.util.response :as response]))

(defn wrap-not-found
  [handler response]
  (fn [req]
    (or (handler req)
        (-> (cond (fn? response) (response req)
                  (map? response) response
                  :else {:body response})
            (response/content-type "text/html")
            (response/status 404)))))
