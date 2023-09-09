(ns reddit-viewer.db
  (:require [malli.core :as m]
            [malli.error :as me]
            [reddit-viewer.utils :as u]
            [re-frame.core :as rf]))

;; Helpers

(def non-empty-string
  (m/schema [:string {:min 1}]))

;; Schema

(def app-db-schema
  [:map
   [:app/sort-keys [:vector
                    [:map
                     [:sort-key [:enum :score :num_comments]]
                     [:title non-empty-string]]]]
   [:app/navbar-items [:vector [:map
                                [:view-id [:enum :posts :chart]]
                                [:title non-empty-string]]]]
   [:view [:enum :posts :chart]]
   [:sort-key [:enum :score :num_comments]]
   [:subreddit/tabs [:vector [:map
                              [:id keyword?]
                              [:title non-empty-string]]]]
   [:subreddit/loading-posts? boolean?]
   [:subreddit/view {:optional true} keyword?]
   [:posts [:sequential
            [:map
             [:score int?]
             [:num_comments int?]
             [:id string?]
             [:title string?]
             [:permalink string?]
             [:url string?]]]]])

(def initial-db
  {:app/sort-keys [{:sort-key :score
                    :title "score"}
                   {:sort-key :num_comments
                    :title "comments"}]
   :app/navbar-items [{:view-id :posts
                       :title "Posts"}
                      {:view-id :chart
                       :title "Chart"}]
   :view :posts
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
