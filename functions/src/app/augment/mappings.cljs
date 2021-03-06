(ns app.augment.mappings
  (:require [backend-shared.service.index :refer [perform fetch]]
            [app.augment.implementation :as impl]
            [shared.protocols.actionable :as ac]
            [shared.protocols.queryable :as qa]
            [shared.protocols.loggable :as log]
            [cljs.core.async :as async]
            [clojure.string :as str]
            [shared.protocols.convertible :as cv])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn mappings []

  (defmethod fetch :resources [{:keys [db]} bookmarks]
    (go
      (let [courses-query   (->> bookmarks
                                 (map :offcourse-id)
                                 (map cv/to-query))
            courses-res     (async/<! (qa/fetch db courses-query))
            courses         (:found courses-res)
            resources-res   (async/<! (qa/fetch db bookmarks))
            resources       (:found resources-res)
            errors          (mapcat :errors [courses-res resources-res])]
        {:found {:resources resources
                 :courses   courses}
         :errors    (when-not (empty? errors) errors)})))

  (defmethod fetch :bookmarks [{:keys [db]} resources]
    (go
      (let [bookmarks-res   (async/<! (qa/fetch db resources))
            bookmarks       (:found bookmarks-res)
            courses-query   (->> bookmarks
                                 (map :offcourse-id)
                                 (map cv/to-query))
            courses-res     (async/<! (qa/fetch db courses-query))
            courses         (:found courses-res)
            errors          (mapcat :errors [courses-res bookmarks-res])]
        {:found {:resources resources
                 :courses   courses}
         :errors    (when-not (empty? errors) errors)})))

  (defmethod perform [:transform :motherload] [_ [_ payload]]
    (let [{:keys [resources courses]} payload
          tags-data   (map #(identity [(:tags %1) (:resource-url %1)]) resources)
          ;; move impl logic to course model
          courses     (map #(impl/augment-course %1 tags-data) courses)]
      (go {:courses courses})))

  (defmethod perform [:put :nothing] [_ _]
    (go {:error :no-payload}))

  (defmethod perform [:put :errors] [_ [_ errors]]
    (go errors))

  (defmethod perform :default [{:keys [stream]} action]
    (ac/perform stream action)))
