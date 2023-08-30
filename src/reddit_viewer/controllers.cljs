(ns reddit-viewer.controllers
  (:require
   [ajax.core :as ajax]
   [reddit-viewer.utils :as utils]
   [re-frame.core :as rf]))

(rf/reg-event-db
 :initialize-db
 (fn [_ _]
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
    :subreddit/loading-posts? true}))

(defn find-posts-with-preview [posts]
  (filter #(= (:post_hint %) "image") posts))

(rf/reg-event-db
 :set-posts
 (fn [db [_ posts]]
   (assoc db :posts
          (->> (get-in posts [:data :children])
               (map :data)
               (find-posts-with-preview)))))

(rf/reg-event-db
 :subreddit/add-subreddit-tab
 (fn [db [_ id subreddit]]
   (update db :subreddit/tabs conj {:id    id
                                    :title subreddit})))

(rf/reg-fx
 :ajax-get
 (fn [[url handler & [error-handler]]]
   (ajax/GET url
     (merge
      {:handler         handler
       :error-handler   #(js/alert "Unexpected error!")
       :response-format :json
       :keywords?       true}
      (if error-handler
        {:error-handler error-handler})))))

(rf/reg-event-fx
 :subreddit/swap-view
 (fn [{db :db} [_ id subreddit]]
   (let [reddit-url (utils/generate-reddit-url subreddit 10)]
     {:db       (assoc db :subreddit/view id)
      :ajax-get [reddit-url #(rf/dispatch [:set-posts %])]})))

(rf/reg-event-fx
 :load-posts
 (fn [{db :db} [_ subreddit num-posts]]
   (let [id (utils/generate-uuid subreddit)
         reddit-url (utils/generate-reddit-url subreddit num-posts)]
     {:db (assoc db :subreddit/view id
                 :subreddit/loading-posts? true)
      :fx [[:ajax-get [reddit-url #(when %
                                     (rf/dispatch [:subreddit/add-subreddit-tab id subreddit])
                                     (rf/dispatch [:set-posts %])
                                     (rf/dispatch [:subreddit/set-loading-status false]))]]]})))

(rf/reg-event-db
 :subreddit/set-loading-status
 (fn [db [_ status]]
   (assoc db :subreddit/loading-posts? status)))

(rf/reg-event-db
 :sort-posts
 (fn [db [_ sort-key]]
   (-> db
       (assoc :sort-key sort-key)
       (update :posts (partial sort-by sort-key >)))))

(rf/reg-event-db
 :select-view
 (fn [db [_ view]]
   (assoc db :view view)))

(rf/reg-sub
 :view
 (fn [db _]
   (:view db)))

(rf/reg-sub
 :app/navbar-items
 (fn [db _]
   (:app/navbar-items db)))

(rf/reg-sub
 :app/sort-keys
 (fn [db _]
   (:app/sort-keys db)))

(rf/reg-sub
 :sort-key
 (fn [db _]
   (:sort-key db)))

(rf/reg-sub
 :posts
 (fn [db _]
   (:posts db)))

(rf/reg-sub
 :subreddit/view
 (fn [db _]
   (:subreddit/view db)))

(rf/reg-sub
 :subreddit/tabs
 (fn [db _]
   (:subreddit/tabs db)))

(rf/reg-sub
 :subreddit/loading-posts?
 (fn [db _]
   (:subreddit/loading-posts? db)))

(rf/reg-event-fx
 :subreddit/remove-subreddit-tab
 (fn [{db :db} [_ evict-id]]
   (let [tabs (:subreddit/tabs db)
         view (:subreddit/view db)
         evict-index (utils/get-evict-tab-index tabs evict-id)
         replacement-index (utils/get-replacement-tab-index tabs evict-index)
         {new-id :id new-subreddit :title} (get tabs replacement-index)
         reddit-url (utils/generate-reddit-url new-subreddit 10)
         evict-current? (= evict-id view)]
     (merge (if (and evict-current? (> (count tabs) 1))
              {:ajax-get [reddit-url #(rf/dispatch [:set-posts %])]})
            {:db (-> db
                     (cond->
                      evict-current? (assoc :subreddit/view new-id)
                      (= (count tabs) 1) (->
                                          (assoc :posts [])
                                          (assoc :subreddit/view nil)))
                     (update :subreddit/tabs utils/remove-by-index evict-index))}))))
