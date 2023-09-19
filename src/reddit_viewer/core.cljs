(ns reddit-viewer.core
  (:require ["react-toastify" :refer (ToastContainer toast)]
            [cuerdas.core :as str]
            [malli.core :as m]
            [malli.error :as me]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [reddit-viewer.chart :as chart]
            [reddit-viewer.controllers]
            [reddit-viewer.subs]
            [reddit-viewer.utils :refer [reddit-origin]]
            [reddit-viewer.utils.schema :refer [non-empty-string]]))

(defn sort-posts [{:keys [title id] :as _sort-posts-info} current-id]
  [:button.btn.btn-light
   {:class (when (= id current-id) "active")
    :on-click #(rf/dispatch [:sort-posts id])}
   (str "sort posts by " title)])

(defn sort-buttons []
  (let [sort-keys @(rf/subscribe [:app/sort-keys])
        sort-keys-list @(rf/subscribe [:app/sort-keys-list])
        current-sort-key @(rf/subscribe [:subreddit/sort-key])]
    [:div.btn-group.py-3
     (for [sort-key-id sort-keys-list]
       (let [sort-key-info (get sort-keys sort-key-id)]
         ^{:key sort-key-id}
         [sort-posts sort-key-info current-sort-key]))]))

(defn display-post [{:keys [permalink subreddit title score url num_comments]}]
  [:div.card.m-2
   [:div.card-body
    [:h4.card-title
     [:a {:href (str reddit-origin permalink)} title]]
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

(def subreddit-search-form-schema
  [:map
   [:subreddit non-empty-string]
   [:num-posts [:and
                pos-int?
                [:fn {:error/message "should be between [1-99]"}
                 #(< 0 % 100)]]]])

(defn error-message
  [form-state error-state k]
  (let [error-msg (str (first (get error-state k)))]
    [:small.text-danger
     (when (and (contains? form-state k) (contains? error-state k))
       error-msg)]))

(defn subreddit-search-bar []
  (r/with-let [draft (r/atom {})
               form-errors (r/atom {})
               reset-form (fn []
                            (reset! draft {})
                            (reset! form-errors nil))
               validate-form (fn []
                               (if-let [errors (-> (m/explain subreddit-search-form-schema @draft)
                                                   me/humanize)]
                                 (reset! form-errors errors)
                                 (reset! form-errors nil)))
               valid-form? (fn []
                             (nil? @form-errors))]
    [:div.bg-light.px-3.py-1
     [:h5.m-2 "Search Subreddit"]
     [:form
      [:div.form-row
       [:div.col-auto.mb-3
        [:input.form-control {:type        "text"
                              :required    true
                              :title       "Enter subreddit"
                              :placeholder "Enter subreddit"
                              :auto-focus  true
                              :value       (or (:subreddit @draft) "")
                              :on-change   (fn [e]
                                             (swap! draft assoc :subreddit (-> e .-target .-value))
                                             (validate-form))}]
        [error-message @draft @form-errors :subreddit]]
       [:div.col-auto.mb-3
        [:input.form-control {:type        "number"
                              :required    true
                              :min 1
                              :max 99
                              :title       "Number between [1-99]"
                              :placeholder "Enter number of posts"
                              :value       (or (:num-posts @draft) "")
                              :on-input    (fn [e]
                                             (let [num-posts (or (parse-long (-> e .-target .-value)) "")]
                                               (swap! draft assoc :num-posts num-posts)
                                               (validate-form)))}]
        [error-message @draft @form-errors :num-posts]]
       [:div.col-auto.mb-3
        [:button.btn
         {:type "button"
          :on-click reset-form}
         "Reset"]]
       [:div.col-auto.mb-3
        [:button.btn.btn-primary
         {:type     "submit"
          :disabled (not (valid-form?))
          :on-click #(do
                       (.preventDefault %)
                       (when (valid-form?)
                         (rf/dispatch [:load-posts (:subreddit @draft) (:num-posts @draft)])
                         (reset-form)))}
         "Search"]]]]]))

(defn subreddit-tab [title view id]
  [:li.nav-item
   [:a.nav-link
    {:href "#"
     :class (when (= id view) "active")
     :on-click #(rf/dispatch [:subreddit/swap-view id])}
    title
    [:button.pl-2.close
     {:on-click (fn [e]
                  (.stopPropagation e)
                  (rf/dispatch [:subreddit/remove-subreddit-tab id]))}
     "\u00D7"]]])

(defn subreddit-tabs []
  (let [view @(rf/subscribe [:subreddit/view])
        subreddit-tabs @(rf/subscribe [:subreddit/tabs])
        subreddits @(rf/subscribe [:subreddit/subreddits])]
    [:ul.nav.nav-tabs.flex-sm-row.pt-2.flex-wrap
     (for [subreddit-id subreddit-tabs]
       ;; TODO component knows too much structural information?
       (let [{:keys [subreddit-name]} (get-in subreddits [subreddit-id :metadata])]
         ^{:key subreddit-id}
         [subreddit-tab subreddit-name view subreddit-id]))]))

(defn subreddit-content []
  (let [view @(rf/subscribe [:app/view])
        subreddits @(rf/subscribe [:subreddit/tabs])]
    (if (empty? subreddits)
      [:div.pt-2 "Please search for a subreddit :)"]
      [:div
       [sort-buttons]
       [:div.card>div.card-block
        (let [posts @(rf/subscribe [:subreddit/active-posts])]
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
  (let [current-view-id @(rf/subscribe [:app/view])
        navbar-items @(rf/subscribe [:app/navbar-items])
        navbar-items-list @(rf/subscribe [:app/navbar-items-list])]
    [:nav.navbar.navbar-light.bg-light.sticky-top.navbar-expand-md
     [:ul.navbar-nav.mr-auto.nav
      (for [navbar-item-id navbar-items-list]
        (let [navitem-info (get navbar-items navbar-item-id)]
          ^{:key navbar-item-id}
          [navitem navitem-info current-view-id]))]]))

(defn site-error [{:keys [id status status-text created-on]}]
  [:div.alert.alert-danger.alert-dismissible
   (str/fmt "Status: %s | %s" status status-text)
   [:div.small (.toLocaleString created-on)]
   [:button.btn.close
    {:type "button"
     :on-click #(rf/dispatch [:app/remove-site-error-by-id id])}
    [:span "\u00D7"]]])

(defn site-errors []
  (let [errors-list @(rf/subscribe [:app/site-errors-list])]
    [:div
     (doall
      (for [error-id errors-list]
        (let [error-info @(rf/subscribe [:app/site-error-by-id error-id])]
          ^{:key error-id}
          [site-error error-info])))]))

(defn home-page []
  [:<>
   [:header
    [navbar]
    [:> ToastContainer {:theme "light"
                        :autoClose false
                        :position "bottom-right"}]]
   [:main.w-100
    [subreddit-search-bar]
    [:button.btn
     {:on-click (fn []
                  (toast (str "Time is: " (.toLocaleString (js/Date.)))))}
     "Send toast"]
    [site-errors]
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
