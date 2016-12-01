(ns app.retrieve.specs
  (:require [cljs.spec :as spec]
            [shared.specs.action :as action :refer [action-spec]]))

(defn actions []

  (defmethod action-spec :put [_]
    (spec/tuple :offcourse/actions (spec/or :courses        (spec/coll-of :raw/course)
                                            :raw-resources  (spec/coll-of :embedly/resource)
                                            :github-courses (spec/coll-of :github/course)
                                            :github-repos   (spec/coll-of :github/repo)
                                            :raw-users      (spec/coll-of :raw/user)
                                            :errors         (spec/coll-of :offcourse/error)
                                            :nothing        nil?))))
