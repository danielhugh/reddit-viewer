(ns reddit-viewer.controllers
  (:require
   [ajax.core :as ajax]
   [reddit-viewer.db :as db]
   [reddit-viewer.utils :as utils]
   [re-frame.core :as rf]))

(rf/reg-event-db
 :initialize-db
 [db/standard-interceptors]
 (fn [_ _]
   db/initial-db))

(defn find-posts-with-preview [posts]
  (filter #(= (:post_hint %) "image") posts))

(defn select-interesting-post-keys [posts]
  (mapv (fn [post]
          (select-keys post [:score :num_comments :id :title :permalink :url]))
        posts))

(defn extract-posts [raw-posts]
  (->> (get-in raw-posts [:data :children])
       (map :data)
       (find-posts-with-preview)
       (select-interesting-post-keys)))

(rf/reg-event-db
 :set-posts
 [db/standard-interceptors]
 (fn [db [_ raw-posts subreddit-id search-params]]
   (let [posts (extract-posts raw-posts)
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

(rf/reg-event-fx
 :load-posts
 [db/standard-interceptors (rf/inject-cofx :uuid)]
 (fn [{:keys [db uuid]} [_ subreddit num-posts]]
   (let [subreddit-id uuid
         reddit-url (utils/generate-reddit-url subreddit num-posts)
         search-params {:subreddit-name subreddit
                        :num-posts num-posts}
         ui-transition {:subreddit/view subreddit-id
                        :subreddit/loading-posts? true}]
     {:db (merge db ui-transition)
      :fx [[:ajax-get [reddit-url #(when %
                                     (rf/dispatch [:subreddit/add-subreddit-tab subreddit-id])
                                     (rf/dispatch [:set-posts % subreddit-id search-params])
                                     (rf/dispatch [:subreddit/set-loading-status false]))]]]})))

(rf/reg-event-db
 :subreddit/set-loading-status
 [db/standard-interceptors]
 (fn [db [_ status]]
   (assoc db :subreddit/loading-posts? status)))

(defn sort-posts [posts sort-fn]
  (vec (sort-fn posts)))

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
                      (sort-posts old-posts sort-fn)))))))

(rf/reg-event-db
 :select-view
 [db/standard-interceptors]
 (fn [db [_ view]]
   (assoc db :app/view view)))

(defn delete-from-tab-list-by-id
  [current-tab-list target-id]
  (into [] (remove #{target-id} current-tab-list)))

(rf/reg-event-fx
 :subreddit/remove-subreddit-tab
 [db/standard-interceptors]
 (fn [{:keys [db]} [_ target-id]]
   (let [current-tab-list (:subreddit/tabs db)
         current-subreddit-view-id (:subreddit/view db)
         target-index (utils/get-evict-tab-index current-tab-list target-id)
         new-tab-list (delete-from-tab-list-by-id current-tab-list target-id)
         new-subreddit-view-index (utils/get-replacement-tab-index current-tab-list target-index)
         removing-current-subreddit? (= current-subreddit-view-id target-id)
         new-subreddit-view (if removing-current-subreddit?
                              (get current-tab-list new-subreddit-view-index)
                              current-subreddit-view-id)]
     {:db (-> db
              (assoc :subreddit/tabs new-tab-list)
              (update :subreddit/subreddits dissoc target-id)
              (assoc :subreddit/view new-subreddit-view))})))

;; Co-effects

(rf/reg-cofx
 :uuid
 (fn [coeffects _]
   (assoc coeffects :uuid (utils/generate-uuid))))
