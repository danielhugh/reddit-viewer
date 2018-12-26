(ns reddit-viewer.core
  (:require
    [cuerdas.core :as str]
    [reagent.core :as r]
    [reddit-viewer.chart :as chart]
    [reddit-viewer.controllers]
    [re-frame.core :as rf]))

(def sort-keys
  "Map of title to sort-key."
  {"score"    :score
   "comments" :num_comments})

(defn sort-posts [title sort-key]
  [:button.btn.btn-secondary
   {:on-click #(rf/dispatch [:sort-posts sort-key])}
   (str "sort posts by " title)])

(defn sort-buttons [sort-keys]
  [:div.btn-group
   (for [[title sort-key] sort-keys]
     ^{:key [title sort-key]}
     [sort-posts title sort-key])])

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

(defn custom-search-bar []
  [:div.ml-2.mr-2 {:style {:background-color "#eee"
                           :border           "1px solid"}}
   [:h5 "Custom Search"]
   [:form
    {:action "#"}
    [:input.m-2 {:type        "text"
                 :pattern     "^[1-9]{1}\\d?$"
                 :title       "Number between [1-99]"
                 :placeholder "Enter number of posts"
                 :on-change   #(rf/dispatch [:set-num-posts (-> % .-target .-value)])}]
    [:input.m-2 {:type        "text"
                 :pattern     "^(?!\\s*$).+"
                 :required    true
                 :title       "Enter subreddit"
                 :placeholder "Enter subreddit"
                 :on-change   #(rf/dispatch [:set-subreddit (-> % .-target .-value)])}]
    [:button.btn.btn-secondary
     {:type     "submit"
      :on-click #(let [subreddit @(rf/subscribe [:get-subreddit])
                       num-posts (if (str/blank? @(rf/subscribe [:get-num-posts]))
                                   10
                                   @(rf/subscribe [:get-num-posts]))]
                   (when (and
                           (not (str/blank? subreddit))
                           (< 0 num-posts 100))
                     (rf/dispatch [:load-posts subreddit num-posts])))}
     "Search"]]])

(defn no-posts []
  [:div "No posts to show! :("])

(defn subreddit-tab [subreddit view id]
  [:li.nav-item
   [:a.nav-link
    {:href     "#"
     :class    (when (= id view) "active")
     :on-click #(rf/dispatch [:load-posts subreddit 10])}
    subreddit]])

(defn subreddit-tabs [subreddits]
  (let [view @(rf/subscribe [:subreddit/view])]
    [:ul.nav.nav-tabs.flex-sm-row
     {:style {:flex-wrap "wrap"}}
     (for [[id subreddit] subreddits]
       ^{:key [id subreddit]}
       [subreddit-tab subreddit view id])]))

(defn home-page []
  (let [view @(rf/subscribe [:view])
        posts @(rf/subscribe [:posts])
        subreddits @(rf/subscribe [:subreddit/tabs])]
    (if (nil? posts)
      [loading-spinner]
      [:div
       [navbar view]
       [custom-search-bar]
       [subreddit-tabs subreddits]
       (if (= (count posts) 0)
         [no-posts]
         [:div.card>div.card-block
          [sort-buttons sort-keys]
          (case @(rf/subscribe [:view])
            :chart [chart/chart-posts-by-votes]
            :posts [display-posts posts])])])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch [:load-posts "Catloaf" 10])
  (mount-root))
