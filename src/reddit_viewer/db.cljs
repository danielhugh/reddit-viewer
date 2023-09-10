(ns reddit-viewer.db
  (:require [malli.core :as m]
            [malli.error :as me]
            [reddit-viewer.utils :as u]
            [re-frame.core :as rf]))

;; Helpers

(def non-empty-string
  (m/schema [:string {:min 1}]))

(def distinct-sequence
  (m/schema [:and
             [:sequential any?]
             [:fn {:error/message "all elements should be distinct"}
              (fn [xs]
                (or (empty? xs)
                    (apply distinct? xs)))]]))

(comment

  :rcf)

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
   [:sort-key [:enum :score :num_comments]]
   [:subreddit/tabs [:vector [:map
                              [:id keyword?]
                              [:title non-empty-string]]]]
   [:subreddit/loading-posts? boolean?]
   [:subreddit/view {:optional true} keyword?]
   [:posts [:vector
            [:map
             [:score int?]
             [:num_comments int?]
             [:id string?]
             [:title string?]
             [:permalink string?]
             [:url string?]]]]])

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
   :sort-key :score
   :subreddit/tabs []
   :subreddit/loading-posts? true
   :posts []})

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
