(ns reddit-viewer.core
  (:require
    [reagent.core :as r]
    [reddit-viewer.chart :as chart]
    [reddit-viewer.controllers]
    [re-frame.core :as rf]))

(defn sort-posts [title sort-key]
  [:button.btn.btn-secondary
   {:on-click #(rf/dispatch [:sort-posts sort-key])}
   (str "sort posts by " title)])

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
   {:class-name (when (= id view) "active")}
   [:a.nav-link
    {:href     "#"
     :on-click #(rf/dispatch [:select-view id])}
    title]])

(defn navbar [view]
  [:nav.navbar.navbar-toggleable-md.navbar-light.bg-faded
   [:ul.navbar-nav.mr-auto.nav
    {:class-name "navbar-nav mr-auto"}
    [navitem "Posts" view :posts]
    [navitem "Chart" view :chart]]])

(defn loading-spinner []
  [:div.spinner
   [:div.bounce1]
   [:div.bounce2]
   [:div.bounce3]])

(defn fetch-posts []
  [:div
   [:form
    {:action "#"}
    [:input.m-2 {:type        "text"
                 :pattern     "\\d{1,2}"
                 :title       "Number between [1-99]"
                 :placeholder "Enter number of posts"
                 :on-change   #(rf/dispatch [:set-num-posts (-> % .-target .-value)])}]
    [:button.btn.btn-secondary
     {:type     "submit"
      :on-click #(let [num-posts @(rf/subscribe [:get-num-posts])]
                   (when (< 0 num-posts 100)
                     (rf/dispatch [:load-posts (str "http://www.reddit.com/r/Catloaf.json?sort=new&limit=" num-posts)])))}
     "Fetch"]]])

(defn select-subreddit []
  [:div
   [:input.m-2 {:type        "text"
                :placeholder "Enter subreddit"
                :on-change   #(rf/dispatch [:set-subreddit (-> % .-target .-value)])}]
   [:button.btn.btn-secondary
    {:type     "button"
     :on-click #(let [subreddit @(rf/subscribe [:get-subreddit])]
                  (rf/dispatch [:load-posts (str "http://www.reddit.com/r/" subreddit ".json?sort=new&limit=10")]))}
    "Get Subreddit"]])

(defn custom-search-bar []
  [:div.ml-2.mr-2 {:style {:background-color "#eee"
                           :border           "1px solid"}}
   [:h5 "Custom Search"]
   [fetch-posts]
   [select-subreddit]])

(defn no-posts []
  [:div "No posts to show! :("])

(defn home-page []
  (let [view @(rf/subscribe [:view])
        posts @(rf/subscribe [:posts])]
    (if (nil? posts)
      [loading-spinner]
      [:div
       [navbar view]
       [custom-search-bar]
       (if (= (count posts) 0)
         [no-posts]
         [:div.card>div.card-block
          [:div.btn-group
           [sort-posts "score" :score]
           [sort-posts "comments" :num_comments]]
          (case @(rf/subscribe [:view])
            :chart [chart/chart-posts-by-votes]
            :posts [display-posts posts])])])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch [:load-posts "http://www.reddit.com/r/Catloaf.json?sort=new&limit=10"])
  (mount-root))
