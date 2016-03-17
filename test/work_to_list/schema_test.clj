(ns work-to-list.schema-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [schema.test]
            [work-to-list.schema :as schema]))

(use-fixtures :once schema.test/validate-schemas)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clocking-key

(deftest clocking-key-for-clocking
  (let [ts (java.util.Date.)
        c {:op/id 123456
           :booking/started-at ts
           :booking/employee
           {:emp/id "10037"
            :emp/given-name "Andy"
            :emp/family-name "Worker"
            :emp/supervisor
            {:emp/id "10007"
             :emp/given-name "Bob"
             :emp/family-name "Manager"}}}
        exp [123456 "10037" ts]]
    (is (= exp (schema/clocking-key c)))))
