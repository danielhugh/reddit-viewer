(ns reddit-viewer.controllers
  (:require
   [ajax.core :as ajax]
   [re-frame.core :as rf]
   [reddit-viewer.components.toast :as toast]
   [reddit-viewer.db :as db]
   [reddit-viewer.utils :as utils]))

(rf/reg-event-db
 :initialize-db
 [db/standard-interceptors]
 (fn [_ _]
   db/initial-db))

(rf/reg-event-db
 :set-posts
 [db/standard-interceptors]
 (fn [db [_ raw-posts subreddit-id search-params]]
   (let [posts (utils/extract-posts raw-posts)
         subreddit {:metadata (merge {:id subreddit-id} search-params)
                    :posts posts}]
     (assoc-in db [:subreddit/subreddits subreddit-id] subreddit))))

(rf/reg-event-db
 :subreddit/add-subreddit-tab
 [db/standard-interceptors]
 (fn [db [_ id]]
   (update db :subreddit/tabs conj id)))

(rf/reg-fx
 :ajax-get
 (fn [[url handler & [error-handler]]]
   (ajax/GET url
     (merge
      {:handler         handler
       :error-handler   #(js/alert "Unexpected error!")
       :response-format :json
       :keywords?       true}
      (when error-handler
        {:error-handler error-handler})))))

(rf/reg-event-fx
 :subreddit/swap-view
 [db/standard-interceptors]
 (fn [{db :db} [_ id]]
   {:db (assoc db :subreddit/view id)}))

(defn load-posts-success
  [subreddit-id search-params res]
  (rf/dispatch [:subreddit/add-subreddit-tab subreddit-id])
  (rf/dispatch [:set-posts res subreddit-id search-params])
  (rf/dispatch [:subreddit/swap-view subreddit-id])
  (rf/dispatch [:subreddit/set-loading-status false]))

(defn load-posts-failure
  [res]
  (rf/dispatch [:subreddit/set-loading-status false])
  (rf/dispatch [:app/emit-http-error-notification res]))

(rf/reg-event-fx
 :app/emit-http-error-notification
 [db/standard-interceptors (rf/inject-cofx :time-now) (rf/inject-cofx :uuid)]
 (fn [_ [_ res]]
   (let [error-message (utils/extract-http-error res)]
     {:fx [[::toast/send-toast [error-message {:type :error}]]]})))

(rf/reg-event-fx
 :load-posts
 [db/standard-interceptors (rf/inject-cofx :uuid)]
 (fn [{:keys [db uuid]} [_ subreddit num-posts]]
   (let [subreddit-id uuid
         reddit-url (utils/generate-reddit-url subreddit num-posts)
         search-params {:subreddit-name subreddit
                        :num-posts num-posts}
         ui-transition {:subreddit/loading-posts? true}]
     {:db (merge db ui-transition)
      :fx [[:ajax-get [reddit-url
                       (partial load-posts-success subreddit-id search-params)
                       (partial load-posts-failure)]]]})))

(rf/reg-event-db
 :subreddit/set-loading-status
 [db/standard-interceptors]
 (fn [db [_ status]]
   (assoc db :subreddit/loading-posts? status)))

(rf/reg-event-db
 :sort-posts
 [db/standard-interceptors]
 (fn [db [_ sort-key]]
   (let [current-subreddit-id (:subreddit/view db)
         sort-fn (partial sort-by sort-key >)]
     (-> db
         (assoc-in [:subreddit/subreddits current-subreddit-id :metadata :sort-key]
                   sort-key)
         (update-in [:subreddit/subreddits current-subreddit-id :posts]
                    (fn [old-posts]
                      (utils/sort-posts old-posts sort-fn)))))))

(rf/reg-event-db
 :select-view
 [db/standard-interceptors]
 (fn [db [_ view]]
   (assoc db :app/view view)))

(rf/reg-event-fx
 :subreddit/remove-subreddit-tab
 [db/standard-interceptors]
 (fn [{:keys [db]} [_ target-id]]
   (let [current-tab-list (:subreddit/tabs db)
         current-subreddit-view-id (:subreddit/view db)
         next-tab-list (utils/remove-id-from-tab-list
                        current-tab-list
                        target-id)
         next-subreddit-view (utils/get-next-subreddit-view
                              current-tab-list
                              current-subreddit-view-id
                              target-id)
         next-db (-> db
                     (assoc :subreddit/tabs next-tab-list)
                     (update :subreddit/subreddits dissoc target-id)
                     (assoc :subreddit/view next-subreddit-view))]
     {:db next-db})))

;; Co-effects

(rf/reg-cofx
 :uuid
 (fn [coeffects _]
   (assoc coeffects :uuid (utils/generate-uuid))))

(rf/reg-cofx
 :time-now
 (fn [coeffects _]
   (assoc coeffects :time-now (js/Date.))))
