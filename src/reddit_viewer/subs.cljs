(ns reddit-viewer.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :app/view
 (fn [db _]
   (:app/view db)))

(rf/reg-sub
 :app/navbar-items
 (fn [db _]
   (:app/navbar-items db)))

(rf/reg-sub
 :app/navbar-items-list
 (fn [db _]
   (:app/navbar-items-list db)))

(rf/reg-sub
 :app/sort-keys
 (fn [db _]
   (:app/sort-keys db)))

(rf/reg-sub
 :app/sort-keys-list
 (fn [db _]
   (:app/sort-keys-list db)))

(rf/reg-sub
 :sort-key
 (fn [db _]
   (:sort-key db)))

(rf/reg-sub
 :subreddit/active-posts
 (fn [db _]
   (let [current-subreddit-id (:subreddit/view db)]
     (-> db :subreddit/subreddits current-subreddit-id :posts))))

(rf/reg-sub
 :subreddit/subreddits
 (fn [db _]
   (-> db :subreddit/subreddits)))

(rf/reg-sub
 :subreddit/view
 (fn [db _]
   (:subreddit/view db)))

(rf/reg-sub
 :subreddit/tabs
 (fn [db _]
   (:subreddit/tabs db)))

(rf/reg-sub
 :subreddit/loading-posts?
 (fn [db _]
   (:subreddit/loading-posts? db)))
