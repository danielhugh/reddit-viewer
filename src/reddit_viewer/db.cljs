(ns reddit-viewer.db)

(def initial-db
  {:app/sort-keys [{:sort-key :score
                    :title "score"}
                   {:sort-key :num_comments
                    :title "comments"}]
   :app/navbar-items [{:view-id :posts
                       :title "Posts"}
                      {:view-id :chart
                       :title "Chart"}]
   :view           :posts
   :sort-key       :score
   :subreddit/tabs []
   :subreddit/loading-posts? true})
