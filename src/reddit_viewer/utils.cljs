(ns reddit-viewer.utils
  (:require [cuerdas.core :as str]))

(def debug? ^boolean goog.DEBUG)

(def reddit-origin "https://www.reddit.com")

(defn generate-reddit-url [subreddit num-posts]
  (str reddit-origin "/r/" subreddit ".json?sort=new&limit=" num-posts))

(defn generate-uuid []
  (str/keyword (str (random-uuid))))

(defn get-evict-tab-index [tabs evict-id]
  (first (keep-indexed
          (fn [index current-id]
            (when (= current-id evict-id)
              index))
          tabs)))

(defn get-replacement-tab-index [tabs evict-index]
  (cond
    ;; the right most tab
    (= (inc evict-index) (count tabs))
    (if (= 1 (count tabs))
      nil ;; no replacement as there is only 1 tab
      (dec evict-index))

    ;; otherwise its a tab somewhere in the middle, take the right tab
    (< (inc evict-index) (count tabs))
    (inc evict-index)

    ;; should not happen
    :else
    (do (js/alert "Something went wrong >:|") nil)))
