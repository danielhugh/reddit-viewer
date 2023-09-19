(ns reddit-viewer.db
  (:require
   [malli.core :as m]
   [malli.error :as me]
   [re-frame.core :as rf]
   [reddit-viewer.utils.schema :refer [non-empty-string distinct-sequence]]
   [reddit-viewer.utils :as u]))

;; Schema

(def app-db-schema
  [:map
   [:app/sort-keys [:map-of
                    :keyword [:map
                              [:id keyword?]
                              [:title non-empty-string]]]]
   [:app/sort-keys-list [:and
                         [:vector :keyword]
                         distinct-sequence]]
   [:app/navbar-items [:map-of
                       :keyword [:map
                                 [:id keyword?]
                                 [:title non-empty-string]]]]
   [:app/navbar-items-list [:and
                            [:vector :keyword]
                            distinct-sequence]]
   [:app/view [:enum :posts :chart]]
   [:subreddit/tabs [:and
                     [:vector :keyword]
                     distinct-sequence]]
   [:subreddit/loading-posts? boolean?]
   [:subreddit/view {:optional true} [:maybe keyword?]]
   [:subreddit/subreddits
    [:map-of
     :keyword [:map
               [:metadata [:map
                           [:id keyword?]
                           [:sort-key {:optional true} [:enum :score :num_comments]]
                           [:subreddit-name non-empty-string]
                           [:num-posts {:min 1 :max 99} int?]]]
               [:posts [:vector
                        [:map
                         [:score int?]
                         [:num_comments int?]
                         [:id string?]
                         [:title string?]
                         [:permalink string?]
                         [:url string?]]]]]]]])

(def initial-db
  {:app/sort-keys {:score {:id :score
                           :title "score"}
                   :num_comments {:id :num_comments
                                  :title "comments"}}
   :app/sort-keys-list [:score :num_comments]
   :app/navbar-items {:posts {:id :posts
                              :title "Posts"}
                      :chart {:id :chart
                              :title "Chart"}}
   :app/navbar-items-list [:posts :chart]
   :app/view :posts
   :subreddit/tabs []
   :subreddit/loading-posts? true
   :subreddit/subreddits {}})

(def app-db-explainer
  (-> app-db-schema m/explainer))

(defn valid-schema? [db]
  (when-let [error (-> db app-db-explainer me/humanize)]
    (throw
     (ex-info (str "Invalid schema.")
              {}
              error))))

(def validate-schema (rf/after valid-schema?))

;; Interceptors

(def standard-interceptors
  [(when u/debug? rf/debug)
   (when u/debug? validate-schema)])
