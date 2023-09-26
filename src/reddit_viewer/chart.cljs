(ns reddit-viewer.chart
  (:require
   ["chart.js" :refer [Chart Colors BarController CategoryScale LinearScale BarElement Legend Tooltip]]
   ["react" :as react]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defn- register-chart []
  ;; See: https://www.chartjs.org/docs/latest/getting-started/integration.html#bundle-optimization
  (.register Chart
             Colors
             BarController
             BarElement
             CategoryScale
             LinearScale
             Legend
             Tooltip))

(defn render-data [node data]
  (register-chart)
  (Chart.
   node
   (clj->js
    {:type "bar"
     :data {:labels (map :title data)
            :datasets [{:label "votes"
                        :data  (map :score data)}
                       {:label "comments"
                        :data  (map :num_comments data)}]}
     :options {:plugins {:tooltip {:enabled true}}
               :scales {:x {:display false}}}})))

(defn destroy-chart [chart]
  (when @chart
    (.destroy @chart)
    (reset! chart nil)))

(defn render-chart [chart ref]
  (fn [component]
    (let [[_ posts] (r/argv component)]
      (when posts
        (destroy-chart chart)
        (reset! chart (render-data (.-current ref) posts))))))

(defn chart-posts-by-votes-inner [_posts]
  (let [chart (atom nil)
        ref (react/createRef)]
    (r/create-class
     {:display-name "chart-posts-by-votes-inner"

      :reagent-render
      (fn [_posts]
        [:canvas {:ref ref}])

      :component-did-mount
      (render-chart chart ref)

      :component-did-update
      (render-chart chart ref)

      :component-will-unmount
      (fn [_component]
        (destroy-chart chart))})))

(defn chart-posts-by-votes []
  (let [posts @(rf/subscribe [:subreddit/active-posts])]
    [chart-posts-by-votes-inner posts]))
