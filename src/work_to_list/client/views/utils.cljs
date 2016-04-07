(ns work-to-list.client.views.utils
  (:require [clojure.string :as string]
            [reagent.core :as reagent]))

(def css-transition (reagent/adapt-react-class js/React.addons.CSSTransitionGroup))

(def transition-opts
  {:component "tbody"
   :transitionName "work-to-list"
   :transitionEnterTimeout 200
   :transitionLeaveTimeout 200})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utility fns

(defn unpluralize [k]
  (let [n (name k)
        s (if (string/ends-with? n "s")
            (subs n 0 (dec (count n)))
            n)]
    (keyword s)))

(defn titleize [s]
  (when s
    (string/replace s #"\b[a-z]" string/upper-case)))

(defn kw->title [k]
  (titleize (string/replace (name k) #"-" " ")))

(defn part-number
  [part]
  (if (:part/drawing-number part)
    (string/join
     "/"
     (remove nil? [(:part/drawing-number part) (:part/drawing-issue part)]))
    (:part/id part)))

(defn employee-name [emp]
  (string/join " " [(:emp/given-name emp) (:emp/family-name emp)]))

(defn zeropad [s l]
  (if (< (count s) l)
    (str (string/join (repeat (- l (count s)) "0")) s)
    s))

(defn time-str [t]
  (let [hm [(.getUTCHours t) (.getUTCMinutes t)]]
    (string/join ":" (map #(zeropad (str %) 2) hm))))

(defn int-str [n]
  (if (or (nil? n) (zero? n))
    "-"
    (string/join
     (reverse
      (interpose
       ","
       (map (comp string/join reverse)
            (partition 3 3 nil (reverse (str n)))))))))

(defn zone-class [zone]
  (when zone
    (str "zone-" (name zone))))
