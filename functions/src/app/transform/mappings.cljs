(ns app.transform.mappings
  (:require [backend-shared.service.index :refer [perform]]
            [shared.protocols.actionable :as ac]
            [shared.protocols.convertible :as cv]
            [app.transform.implementation :as impl]
            [shared.protocols.loggable :as log]
            [cljs.nodejs :as node]
            [clojure.walk :as walk]
            [shared.models.course.index :as co]
            [clojure.string :as clj-str]
            [shared.protocols.specced :as sp]))

(def atob (node/require "atob"))
(def yaml (node/require "js-yaml"))

(defn to-js [obj]
  (.parse js/JSON obj))

(defn yaml-file? [{:keys [path] :as ref}]
  (re-find #"\.yaml$" path))

(defn to-courses [{:keys [tree user-name] :as res}]
    (->> tree
         (filterv yaml-file?)
         (map #(assoc %1 :user-name user-name))))

(defn handle-content [{:keys [content user-name] :as res}]
  (let [new-course (->> content
                     atob
                     (.safeLoad yaml)
                     js->clj
                     walk/keywordize-keys)]
    (co/initialize (assoc new-course :repository "offcourse"
                          :curator (clj-str/lower-case user-name)))))

(defn mappings []

  (defmethod perform [:transform :raw-users] [_ [_ payload]]
    {:profiles (map impl/to-profile payload)
     :portraits (map impl/to-portrait payload)
     :identities (mapcat impl/to-identities payload)})

  (defmethod perform [:transform :github-repos] [_ [_ payload]]
    {:repos (mapcat to-courses payload)})

  (defmethod perform [:transform :github-courses] [_ [_ payload]]
    (let [courses (map handle-content payload)
          errors  (remove true? (map sp/valid? courses))]
      (if (empty? errors)
        {:courses (map handle-content payload)}
        {:errors [{:error (sp/errors courses)}]})))

  (defmethod perform [:transform :embedly] [_ [_ raw-resources]]
    (let [converted (keep impl/to-resource raw-resources)]
      (if-not (empty? converted)
        {:resources converted}
        {:error raw-resources})))

  (defmethod perform [:transform :courses] [_ [_ courses]]
    {:bookmarks (mapcat impl/to-bookmark courses)})

  (defmethod perform :default [{:keys [stream]} action]
    (ac/perform stream action)))
