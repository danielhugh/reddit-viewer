(ns reddit-viewer.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :view
 (fn [db _]
   (:view db)))

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
 :posts
 (fn [db _]
   (:posts db)))

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
