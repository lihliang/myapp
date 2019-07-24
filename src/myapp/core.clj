(ns myapp.core
  (:require [compojure.api.sweet :refer :all]
            [myapp.service.wechat :as wechat]
            [myapp.service.user :as user]
            [myapp.service.buddyt :as buddyt]
            [myapp.service.liberatortest :as liberatort]
            [ring.util.http-response :refer :all]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]
            [schema.core :as s]
            [liberator.core :refer [defresource]]
            [ring.util.response :refer [response]]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [buddy.auth :refer [authenticated? throw-unauthorized]])
  (:gen-class))

(s/defschema Response
             {:isSuccess s/Bool
              :errcode s/Int
              :errmsg s/Str
              :result {s/Keyword s/Any}})


;; we hold a entries in this ref
(defonce entries (ref {}))
;; create and list entries
(defresource list-resource
             :available-media-types ["application/json"]
             :allowed-methods [:get :post]
             :known-content-type? #(liberatort/check-content-type % ["application/json"])
             :malformed? #(liberatort/parse-json % ::data)
             :post! #(let [id (str (inc (rand-int 100000)))]
                       (dosync (alter entries assoc id (::data %)))
                       {::id id})
             :post-redirect? true
             :location #(liberatort/build-entry-url (get % :request) (get % ::id))
             :handle-ok #(map (fn [id] (str (liberatort/build-entry-url (get % :request) id)))
                              (keys @entries)))

(defresource entry-resource [id]
             :allowed-methods [:get :put :delete]
             :known-content-type? #(liberatort/check-content-type % ["application/json"])
             :exists? (fn [_]
                        (let [e (get @entries id)]
                          (if-not (nil? e)
                            {::entry e})))
             :existed? (fn [_] (nil? (get @entries id ::sentinel)))
             :available-media-types ["application/json"]
             :handle-ok ::entry
             :delete! (fn [_] (dosync (alter entries assoc id nil)))
             :malformed? #(liberatort/parse-json % ::data)
             :can-put-to-missing? false
             :put! #(dosync (alter entries assoc id (::data %)))
             :new? (fn [_] (nil? (get @entries id ::sentinel))))

(defroutes collection-example
  (ANY ["/collection/:id{[0-9]+}"] [id] (entry-resource id))
  (ANY "/collection" [] list-resource))

(defn buddyhandler
  [request]
  (if (:identity request)
    (response (format "Hello %s" (:identity request)))
    (response (str (:identity request)  "Hello Anonymous") )))

(def buddyt1
  (GET "/buddyt1" []
    (ok {:message (str buddyt/buddytest1)} )))

(def buddyt2
  (GET "/buddyt2" []
    (ok {:message (str buddyt/buddytest2)} )))

(def buddyt3
  (GET "/buddyt3" []
    (ok {:message (str buddyt/buddytest3)} )))

(def verifyt3
  (GET "/verifyt3" []
    (ok {:message buddyt/verify3} )))

(def app (-> buddyhandler
             (wrap-authentication buddyt/backend)))

(def app1
  (api
    {
     :swagger
     {:ui   "/"
      :spec "/swagger.json"
      :data {
             :info {
                    :title       "My APP to Cat&Fish forever"
                    :description "A real heart to fish"
                    }
             :tags [{:name "cat" :description "handsome cat"}
                    {:name "fish" :description "beautiful fish"}
                    {:name "buddy" :description "Buddy test"}]}}}
    (context "/api" []
             :tags ["cat"]
             (GET "/hello" []
                  (ok {:message "hello"}))
             (GET "/wechat-info" []
                  :return Response
                  :query-params [code :- String, state :- String]
                  :summary "The method to progress info from wechat"
                  (ok (wechat/wechat-info code state)))
             (GET "/wechat-auth-info" []
                  :summary "The way to have user's perm"
                  (permanent-redirect (wechat/wechat-auth-info))))

    (context "/user" []
             :tags ["fish"]
             (GET "/register" []
                  :return Response
                  :query-params [id :- s/Int
                                 username :- String
                                 pwd :- String]
                  :summary "Fish register"
                  (ok (user/register! id username pwd)))
             (GET "/login" []
                  :return Response
                  :query-params [id :- s/Int
                                 username :- String
                                 pwd :- String]
                  (ok (user/login! id username pwd))))

    (context "/buddy" []
      :tags ["buddy"]
      buddyt1
      buddyt2
      buddyt3
      verifyt3)
    ))