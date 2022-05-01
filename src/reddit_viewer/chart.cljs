(ns reddit-viewer.chart
  (:require
   ["chart.js" :as chart]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [re-frame.core :as rf]))

(defn render-data [node data]
  (chart.
   node
   (clj->js
    {:type    "bar"
     :data    {:labels   (map :title data)
               :datasets [{:label "votes"
                           :data  (map :score data)}
                          #_{:label "comments"
                             :data  (map :num_comments data)}]}
     :options {:scales {:xAxes [{:display false}]}}})))

(defn destroy-chart [chart]
  (when @chart
    (.destroy @chart)
    (reset! chart nil)))

(defn render-chart [chart]
  (fn [component]
    (when-let [posts @(rf/subscribe [:posts])]
      (destroy-chart chart)
      (reset! chart (render-data (rdom/dom-node component) posts)))))

(defn render-canvas []
  (when @(rf/subscribe [:posts]) [:canvas]))

(defn chart-posts-by-votes []
  (let [chart (atom nil)]
    (r/create-class
      {:component-did-mount    (render-chart chart)
       :component-did-update   (render-chart chart)
       :component-will-unmount (fn [_] (destroy-chart chart))
       :render                 render-canvas})))