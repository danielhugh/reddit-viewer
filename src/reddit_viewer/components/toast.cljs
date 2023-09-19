(ns reddit-viewer.components.toast
  (:require ["react-toastify" :refer (toast)]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn default-toast-body-ui
  [message _toast-opts]
  [:div
   [:div message]
   [:small (str "Sent at: " (.toLocaleString (js/Date.)))]])

(rf/reg-fx
 ::send-toast
 (fn [[message toast-opts & {:keys [toast-body-ui] :as _opts}]]
   (let [component (or toast-body-ui default-toast-body-ui)]
     (toast
      (r/as-element
       [component message toast-opts])
      (clj->js toast-opts)))))

(comment
  (toast (clj->js {:type :error}))
  :rcf)

