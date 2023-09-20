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

(def ff-protection-message
  "If on Firefox, try turning off Enhanced Tracking Protection.")

(defn extract-http-error
  [{:keys [status status-text response]}]
  (let [res-message (if (str/blank? status-text)
                      (:message response)
                      status-text)
        base-message (str/fmt "%s: %s" status res-message)]
    (cond-> base-message
      (= status 0)
      (str " " ff-protection-message))))

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
