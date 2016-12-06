(ns app.augment.mappings
  (:require [backend-shared.service.index :refer [perform fetch]]
            [shared.protocols.actionable :as ac]
            [shared.protocols.queryable :as qa]
            [shared.protocols.loggable :as log]
            [cljs.core.async :as async]
            [clojure.string :as str])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn mappings []

  (defn to-course-query [{:keys [offcourse-id] :as bm}]
    (let [[repo curator course-id revision checkpoint-id] (str/split offcourse-id "::")]
      {:course-id (str repo "::" curator "::" course-id)
       :revision (int revision)}))

  (defmethod fetch :bookmarks [{:keys [db]} resources]
    (go
      (let [bookmarks-res   (async/<! (qa/fetch db resources))
            bookmarks       (:found bookmarks-res)
            courses-query   (map to-course-query bookmarks)
            courses-res     (async/<! (qa/fetch db courses-query))
            courses         (:found courses-res)
            errors          (mapcat :errors [courses-res bookmarks-res])]
        {:resources resources
         :courses   courses
         :bookmarks bookmarks
         :errors    (when-not (empty? errors) errors)})))


  (defmethod perform [:put :nothing] [_ _]
    (go {:error :no-payload}))

  (defmethod perform [:put :errors] [_ [_ errors]]
    (go errors))

  (defmethod perform :default [{:keys [bucket]} action]
    (ac/perform bucket action)))