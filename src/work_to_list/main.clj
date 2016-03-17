(ns work-to-list.main
  (:require [com.stuartsierra.component :as component]
            [work-to-list.logging]
            [work-to-list.system :refer [system]])
  (:gen-class))

(defn -main [& args]
  (let [sys (component/start (system))]
    (.addShutdownHook (Runtime/getRuntime) (Thread. #(component/stop sys)))))
