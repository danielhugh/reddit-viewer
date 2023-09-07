(ns reddit-viewer.controllers
  (:require
   [ajax.core :as ajax]
   [reddit-viewer.db :as db]
   [reddit-viewer.utils :as utils]
   [re-frame.core :as rf]))

(rf/reg-event-db
 :initialize-db
 (fn [_ _]
   db/initial-db))

(defn find-posts-with-preview [posts]
  (filter #(= (:post_hint %) "image") posts))

(defn select-interesting-post-keys [posts]
  (map (fn [post]
         (select-keys post [:score :num_comments :id :title :permalink :url]))
       posts))

(rf/reg-event-db
 :set-posts
 (fn [db [_ posts]]
   (assoc db :posts
          (->> (get-in posts [:data :children])
               (map :data)
               (find-posts-with-preview)
               (select-interesting-post-keys)))))

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
      (when error-handler
        {:error-handler error-handler})))))

(rf/reg-event-fx
 :subreddit/swap-view
 (fn [{db :db} [_ id subreddit]]
   (let [reddit-url (utils/generate-reddit-url subreddit 10)]
     {:db       (assoc db :subreddit/view id)
      :ajax-get [reddit-url #(rf/dispatch [:set-posts %])]})))

(rf/reg-event-fx
 :load-posts
 [(rf/inject-cofx :uuid)]
 (fn [{:keys [db uuid]} [_ subreddit num-posts]]
   (let [subreddit-id uuid
         reddit-url (utils/generate-reddit-url subreddit num-posts)]
     {:db (assoc db :subreddit/view subreddit-id
                 :subreddit/loading-posts? true)
      :fx [[:ajax-get [reddit-url #(when %
                                     (rf/dispatch [:subreddit/add-subreddit-tab subreddit-id subreddit])
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
     (merge (when (and evict-current? (> (count tabs) 1))
              {:ajax-get [reddit-url #(rf/dispatch [:set-posts %])]})
            {:db (-> db
                     (cond->
                      evict-current? (assoc :subreddit/view new-id)
                      (= (count tabs) 1) (->
                                          (assoc :posts [])
                                          (assoc :subreddit/view nil)))
                     (update :subreddit/tabs utils/remove-by-index evict-index))}))))

;; Co-effects

(rf/reg-cofx
 :uuid
 (fn [coeffects _]
   (assoc coeffects :uuid (utils/generate-uuid))))
