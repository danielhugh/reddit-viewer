(ns reddit-viewer.utils.schema
  (:require [malli.core :as m]))

(def non-empty-string
  (m/schema [:string {:min 1}]))

(def distinct-sequence
  (m/schema [:and
             [:sequential any?]
             [:fn {:error/message "all elements should be distinct"}
              (fn [xs]
                (or (empty? xs)
                    (apply distinct? xs)))]]))

(def date?
  [:fn {:error/message "should be a date"}
   (fn [d]
     (instance? js/Date d))])
