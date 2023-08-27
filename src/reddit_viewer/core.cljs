(ns reddit-viewer.core
  (:require
   [cuerdas.core :as str]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [reddit-viewer.chart :as chart]
   [reddit-viewer.controllers]
   [re-frame.core :as rf]))

(defn sort-posts [title current-sort-key sort-key]
  [:button.btn.btn-secondary
   {:class (when (= current-sort-key sort-key) "active")
    :on-click #(rf/dispatch [:sort-posts sort-key])}
   (str "sort posts by " title)])

(defn sort-buttons []
  (let [sort-keys @(rf/subscribe [:app/sort-keys])
        current-sort-key @(rf/subscribe [:sort-key])]
    [:div.btn-group.py-3
     (for [{:keys [title sort-key]} sort-keys]
       ^{:key sort-key}
       [sort-posts title current-sort-key sort-key])]))

(defn display-post [{:keys [permalink subreddit title score url num_comments]}]
  [:div.card.m-2
   [:div.card-body
    [:h4.card-title
     [:a {:href (str "http://reddit.com" permalink)} title]]
    [:div [:span.badge.badge-pill.badge-info
           (str subreddit " score " score)]]
    [:div [:span.badge.badge-pill.badge-info
           (str subreddit " comments " num_comments)]]
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


(defn loading-spinner []
  [:div.spinner
   [:div.bounce1]
   [:div.bounce2]
   [:div.bounce3]])

(defn reset-form-fields []
  (rf/dispatch [:set-subreddit ""])
  (rf/dispatch [:set-num-posts nil]))

(defn subreddit-search-bar []
  (r/with-let [_ (reset-form-fields)]
    [:div.bg-light.px-3.py-1
     [:h5.m-2 "Search Subreddit"]
     [:form {:on-submit #(.preventDefault %)}
      [:div.row.align-items-center
       [:div.col-auto
        [:input.m-2.form-control {:type        "text"
                                  :pattern     "^(?!\\s*$).+"
                                  :required    true
                                  :title       "Enter subreddit"
                                  :placeholder "Enter subreddit"
                                  :value       @(rf/subscribe [:get-subreddit])
                                  :on-change   #(rf/dispatch [:set-subreddit (-> % .-target .-value)])}]]
       [:div.col-auto
        [:input.m-2.form-control {:type        "text"
                                  :pattern     "^[1-9]{1}\\d?$"
                                  :required    true
                                  :title       "Number between [1-99]"
                                  :placeholder "Enter number of posts"
                                  :value       @(rf/subscribe [:get-num-posts])
                                  :on-change   #(rf/dispatch [:set-num-posts (-> % .-target .-value)])}]]
       [:div.col-auto
        [:button.btn.btn-primary
         {:type     "submit"
          :on-click #(let [subreddit @(rf/subscribe [:get-subreddit])
                           num-posts (if (str/blank? @(rf/subscribe [:get-num-posts]))
                                       10
                                       @(rf/subscribe [:get-num-posts]))]
                       (when (and
                              (not (str/blank? subreddit))
                              (< 0 num-posts 100))
                         (rf/dispatch [:load-posts subreddit num-posts])
                         (reset-form-fields)))}
         "Search"]]]]]))

(defn subreddit-tab [title view id]
  [:li.nav-item
   [:a.nav-link
    {:href "#"
     :class (when (= id view) "active")
     :on-click #(rf/dispatch [:subreddit/swap-view id title])}
    title
    [:span.pl-2.close
     {:on-click #(rf/dispatch [:subreddit/remove-subreddit-tab id])}
     "\u00D7"]]])

(defn subreddit-tabs []
  (let [view @(rf/subscribe [:subreddit/view])
        subreddits @(rf/subscribe [:subreddit/tabs])]
    [:ul.nav.nav-tabs.flex-sm-row.pt-2
     {:style {:flex-wrap "wrap"}}
     (for [{:keys [id title]} subreddits]
       ^{:key id}
       [subreddit-tab title view id])]))

(defn subreddit-content []
  (let [view @(rf/subscribe [:view])
        subreddits @(rf/subscribe [:subreddit/tabs])]
    (if (empty? subreddits)
      [:div.pt-2 "Please search for a subreddit :)"]
      [:div
       [sort-buttons]
       [:div.card>div.card-block
        (let [posts @(rf/subscribe [:posts])]
          (if (empty? posts)
            [:div.pt-2 "No posts to show :("]
            (case view
              :chart [chart/chart-posts-by-votes]
              :posts [display-posts posts])))]])))

(defn navitem [title current-view-id view-id]
  [:li.nav-item
   [:a.nav-link
    {:href "#"
     :class (when (= view-id current-view-id) "active")
     :on-click #(rf/dispatch [:select-view view-id])}
    title]])

(defn navbar []
  (let [current-view-id @(rf/subscribe [:view])
        navbar-items @(rf/subscribe [:app/navbar-items])]
    [:nav.navbar.navbar-light.bg-light.sticky-top.navbar-expand-md
     [:ul.navbar-nav.mr-auto.nav
      (for [{:keys [title view-id]} navbar-items]
        ^{:key view-id}
        [navitem title current-view-id view-id])]]))

(defn home-page []
  [:div
   [navbar]
   [:div.w-100
    [subreddit-search-bar]
    [:div.container
     [subreddit-tabs]
     [subreddit-content]]]])

;; -------------------------
;; Initialize app

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [home-page] root-el)))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch [:load-posts "Catloaf" 10])
  (mount-root))
