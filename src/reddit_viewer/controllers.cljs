(ns reddit-viewer.controllers
  (:require
    [ajax.core :as ajax]
    [reddit-viewer.utils :as utils]
    [re-frame.core :as rf]))

(rf/reg-event-db
  :initialize-db
  (fn [_ _]
    {:view           :posts
     :sort-key       :score
     :subreddit/tabs []}))

(defn find-posts-with-preview [posts]
  (filter #(= (:post_hint %) "image") posts))

(rf/reg-event-db
  :set-posts
  (fn [db [_ posts]]
    (assoc db :posts
              (->> (get-in posts [:data :children])
                   (map :data)
                   (find-posts-with-preview)))))

(rf/reg-event-db
  :subreddit/add-subreddit-tab
  (fn [db [_ subreddit id]]
    (update db :subreddit/tabs conj [id subreddit])))

(rf/reg-fx
  :ajax-get
  (fn [[url handler & [error-handler]]]
    (ajax/GET url
              {:handler         handler
               :error-handler   (if error-handler
                                  #(js/alert "custom error here")
                                  #(js/alert "No subreddit found"))
               :response-format :json
               :keywords?       true})))


(rf/reg-event-fx
  :load-posts
  (fn [{db :db} [_ subreddit num-posts]]
    (let [id (utils/generate-uuid subreddit)
          reddit-url (utils/generate-reddit-url subreddit num-posts)]
      {:db       (assoc db :subreddit/view id)
       :ajax-get [reddit-url
                  #(when %
                     (rf/dispatch [:subreddit/add-subreddit-tab subreddit id])
                     (rf/dispatch [:set-posts %]))]})))

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

(rf/reg-sub
  :subreddit/view
  (fn [db _]
    (:subreddit/view db)))

(rf/reg-sub
  :subreddit/tabs
  (fn [db _]
    (:subreddit/tabs db)))

(rf/reg-event-fx
  :subreddit/remove-subreddit-tab
  (fn [{db :db} [_ evict-id]]
    (let [tabs (:subreddit/tabs db)
          evict-index (utils/get-evict-tab-index tabs evict-id)
          replacement-index (utils/get-replacement-tab-index tabs evict-index)
          [new-id subreddit] (get tabs replacement-index)
          reddit-url (utils/generate-reddit-url subreddit 10)]
      {:db       (-> db
                     (update :subreddit/tabs utils/remove-by-index evict-index)
                     (assoc :subreddit/view new-id))
       :ajax-get [reddit-url #(rf/dispatch [:set-posts %])]})))
