(ns reddit-viewer.core
  (:require
    [ajax.core :as ajax]
    [reagent.core :as r]
    [reddit-viewer.chart :as chart]))

(defonce posts (r/atom nil))

(defn find-posts-with-preview [posts]
  (filter #(= (:post_hint %) "image") posts))

(defn load-posts []
  (ajax/GET "http://www.reddit.com/r/Catloaf.json?sort=new&limit=10"
            {:handler         #(->> (get-in % [:data :children])
                                    (map :data)
                                    (find-posts-with-preview)
                                    (reset! posts))
             :response-format :json
             :keywords?       true}))

(defn reverse-sort [sort-key col]
  (reverse (sort-by sort-key col)))

(defn sort-posts [title sort-key]
  (when-not (empty? @posts)
    [:button.btn.btn-secondary
     {:on-click #(swap! posts (partial reverse-sort sort-key))}
     (str "sort posts by " title)]))

;; -------------------------
;; Views

(defn display-post [{:keys [permalink subreddit title score url num_comments]}]
  [:div.card.m-2
   [:div.card-block
    [:h4.card-title
     [:a {:href (str "http://reddit.com" permalink)} title " "]]
    [:div [:span.badge.badge-info {:color "info"} subreddit " score " score]]
    [:div [:span.badge.badge-info {:color "info"} subreddit " comments " num_comments]]
    [:img {:width "100%" :src url}]]])

(defn display-posts [posts]
  (when-not (empty? posts)
    [:div
     (for [posts-row (partition-all 3 posts)]
       ^{:key posts-row}
       [:div.row
        (for [post posts-row]
          ^{:key post}
          [:div.col-4 [display-post post]])])]))

(defn navitem [title view id]
  [:li.nav-item
   {:class-name (when (= id @view) "active")}
   [:a.nav-link
    {:href     "#"
     :on-click #(reset! view id)}
    title]])

(defn navbar [view]
  [:nav.navbar.navbar-toggleable-md.navbar-light.bg-faded
   [:ul.navbar-nav.mr-auto.nav
    {:class-name "navbar-nav mr-auto"}
    [navitem "Posts" view :posts]
    [navitem "Chart" view :chart]]])

(defn home-page []
  (r/with-let [view (r/atom :posts)]
    [:div
     [navbar view]
     [:div.card>div.card-block
      [:div.btn-group
       [sort-posts "score" :score]
       [sort-posts "comments" :num_comments]]
      (case @view
        :chart [chart/chart-posts-by-votes posts]
        :posts [display-posts @posts])]]))

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root)
  (load-posts))