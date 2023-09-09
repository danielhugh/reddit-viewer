(ns reddit-viewer.core
  (:require
   [cuerdas.core :as str]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [reddit-viewer.chart :as chart]
   [reddit-viewer.controllers]
   [reddit-viewer.subs]))

(defn sort-posts [{:keys [title id] :as _sort-posts-info} current-id]
  [:button.btn.btn-secondary
   {:class (when (= id current-id) "active")
    :on-click #(rf/dispatch [:sort-posts id])}
   (str "sort posts by " title)])

(defn sort-buttons []
  (let [sort-keys @(rf/subscribe [:app/sort-keys])
        sort-keys-list @(rf/subscribe [:app/sort-keys-list])
        current-sort-key @(rf/subscribe [:sort-key])]
    [:div.btn-group.py-3
     (for [sort-key-id sort-keys-list]
       (let [sort-key-info (get sort-keys sort-key-id)]
         ^{:key sort-key-id}
         [sort-posts sort-key-info current-sort-key]))]))

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

(defn subreddit-search-bar []
  (r/with-let [draft (r/atom {})
               valid-form? (fn [s]
                             (and (not (str/blank? (:subreddit s)))
                                  (< 0 (:num-posts s) 100)))]
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
                                  :value       (or (:subreddit @draft) "")
                                  :on-change   (fn [e]
                                                 (swap! draft assoc :subreddit (-> e .-target .-value)))}]]
       [:div.col-auto
        [:input.m-2.form-control {:type        "number"
                                  :pattern     "^[1-9]{1}\\d?$"
                                  :required    true
                                  :title       "Number between [1-99]"
                                  :placeholder "Enter number of posts"
                                  :value       (or (:num-posts @draft) "")
                                  :on-change   (fn [e]
                                                 (swap! draft assoc :num-posts (-> e .-target .-value)))}]]
       [:div.col-auto
        [:button.btn.btn-primary
         {:type     "submit"
          :disabled (not (valid-form? @draft))
          :on-click #(do
                       (rf/dispatch [:load-posts (:subreddit @draft) (:num-posts @draft)])
                       (reset! draft {}))}
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

(defn navitem [{:keys [title id] :as _navitem-info} current-id]
  [:li.nav-item
   [:a.nav-link
    {:href "#"
     :class (when (= id current-id) "active")
     :on-click #(rf/dispatch [:select-view id])}
    title]])

(defn navbar []
  (let [current-view-id @(rf/subscribe [:view])
        navbar-items @(rf/subscribe [:app/navbar-items])
        navbar-items-list @(rf/subscribe [:app/navbar-items-list])]
    [:nav.navbar.navbar-light.bg-light.sticky-top.navbar-expand-md
     [:ul.navbar-nav.mr-auto.nav
      (for [navbar-item-id navbar-items-list]
        (let [navitem-info (get navbar-items navbar-item-id)]
          ^{:key navbar-item-id}
          [navitem navitem-info current-view-id]))]]))

(defn home-page []
  [:div
   [navbar]
   [:div.w-100
    [subreddit-search-bar]
    [:div.container
     [subreddit-tabs]
     (if @(rf/subscribe [:subreddit/loading-posts?])
       [loading-spinner]
       [subreddit-content])]]])

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
