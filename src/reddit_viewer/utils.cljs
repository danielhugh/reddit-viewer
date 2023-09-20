(ns reddit-viewer.utils
  (:require [cuerdas.core :as str]))

(def debug? ^boolean goog.DEBUG)

(def reddit-origin "https://old.reddit.com")

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
    (do (js/alert "Something went wrong 🤔") nil)))

(defn find-posts-with-preview [posts]
  (filter #(= (:post_hint %) "image") posts))

(defn select-interesting-post-keys [posts]
  (mapv (fn [post]
          (select-keys post [:score :num_comments :id :title :permalink :url]))
        posts))

(defn extract-posts [raw-posts]
  (->> (get-in raw-posts [:data :children])
       (map :data)
       (find-posts-with-preview)
       (select-interesting-post-keys)))

(defn extract-http-error
  [{:keys [status status-text]}]
  (case status
    0 (str/fmt "Status: %s | %s | (If on Firefox, turn off Enhanced Tracking Protection)" status status-text)
    (str/fmt "Status: %s | %s" status status-text)))

(defn sort-posts [posts sort-fn]
  (vec (sort-fn posts)))

(defn remove-id-from-tab-list
  [current-tab-list target-id]
  (into [] (remove #{target-id} current-tab-list)))

(defn get-next-subreddit-view [current-tab-list current-subreddit-view-id target-id]
  (let [target-index (get-evict-tab-index current-tab-list target-id)
        new-subreddit-view-index (get-replacement-tab-index current-tab-list target-index)
        removing-current-subreddit? (= current-subreddit-view-id target-id)]
    (if removing-current-subreddit?
      (get current-tab-list new-subreddit-view-index)
      current-subreddit-view-id)))
