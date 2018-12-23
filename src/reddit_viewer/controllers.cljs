(ns reddit-viewer.controllers
  (:require
    [ajax.core :as ajax]
    [re-frame.core :as rf]))

(rf/reg-event-db
  :initialize-db
  (fn [_ _]
    {:view      :posts
     :sort-key  :score
     :subreddit "Catloaf"}))

(defn find-posts-with-preview [posts]
  (filter #(= (:post_hint %) "image") posts))

(rf/reg-event-db
  :set-posts
  (fn [db [_ posts]]
    (assoc db :posts
              (->> (get-in posts [:data :children])
                   (map :data)
                   (find-posts-with-preview)))))

(rf/reg-fx
  :ajax-get
  (fn [[url handler]]
    (ajax/GET url
              {:handler         handler
               :error-handler   #(js/alert "No subreddit found")
               :response-format :json
               :keywords?       true})))

(rf/reg-event-fx
  :load-posts
  (fn [_ [_ url]]
    {:ajax-get [url #(rf/dispatch [:set-posts %])]}))

(rf/reg-event-db
  :sort-posts
  (fn [db [_ sort-key]]
    (update db :posts (partial sort-by sort-key >))))

(rf/reg-event-db
  :select-view
  (fn [db [_ view]]
    (assoc db :view view)))

(rf/reg-sub
  :view
  (fn [db _]
    (:view db)))

(rf/reg-sub
  :posts
  (fn [db _]
    (:posts db)))

(rf/reg-event-db
  :set-num-posts
  (fn [db [_ num]]
    (assoc db :num-posts num)))

(rf/reg-sub
  :get-num-posts
  (fn [db _]
    (get db :num-posts)))

(rf/reg-event-db
  :set-subreddit
  (fn [db [_ subreddit]]
    (assoc db :subreddit subreddit)))

(rf/reg-sub
  :get-subreddit
  (fn [db _]
    (get db :subreddit)))

