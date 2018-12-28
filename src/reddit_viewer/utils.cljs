(ns reddit-viewer.utils
  (:require [cuerdas.core :as str]))

(defn generate-reddit-url [subreddit num-posts]
  (str "https://www.reddit.com/r/" subreddit ".json?sort=new&limit=" num-posts))

(defn generate-uuid [id]
  (str/keyword
    (str (str/lower id) "-" (random-uuid))))

(defn get-evict-tab-index [tabs evict-id]
  (first (keep-indexed
           (fn [index {id :id}]
             (if (= id evict-id)
               index))
           tabs)))

(defn remove-by-index [vector index]
  (into [] (concat (subvec vector 0 index)
                   (subvec vector (inc index)))))

(defn get-replacement-tab-index [tabs evict-index]
  (cond
    ;; the right most tab
    (= (inc evict-index) (count tabs))
    (if (= 1 (count tabs))
      (do (println "only one") 0)
      (do (println "left") (dec evict-index)))

    ;; otherwise its a tab somewhere in the middle, take the right tab
    (< (inc evict-index) (count tabs))
    (inc evict-index)

    ; should not happen
    :else
    (do (js/alert "moo") nil)))
