(ns myapp.core
  (:require [compojure.api.sweet :refer :all]
            [myapp.service.wechat :as wechat]
            [myapp.service.user :as user]
            [ring.util.http-response :refer :all]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]
            [schema.core :as s]
            [myapp.utils.response :as res]
            [clojure.tools.logging :as log]
            [buddy.core.hash :as hash]
            [buddy.core.codecs :refer :all]
            [clojure.java.io :as io]
            [buddy.core.mac :as mac])
  (:gen-class))

(s/defschema Response
             {:isSuccess s/Bool
              :errcode s/Int
              :errmsg s/Str
              :result {s/Keyword s/Any}})

;(s/defschema USER {
;                   :name s/String
;                   :pwd s/String
;                   })

(def users (atom {}))
(let [ids (atom 0)]
  (defn register! [id username pwd]
    (let [idreal (or id (swap! ids inc))]
      (if (nil? username)
        (do
          (log/error "Illegal username")
          (res/failResponse {:errmsg "wrong update"}))
        (do
          (swap! users assoc idreal (assoc {} name pwd))
          (log/info "Register succeed")
          (res/succResponse {:name username :id idreal}))))))

(defn login [id name pwd]
  (let [ishave (get @users id)]
    (if (nil? ishave)
      (do
        (log/error "User is not exist")
        (res/failResponse {:errmsg "User is not exist"}))
      (do
        (let [ispwd (map val ishave)]
          (if (= (list pwd) ispwd)
            (do
              (log/info "login succeed")
              (prn "Welcome!")
              (res/succResponse {:message "login succeed"})
              )
            (do
              (log/error "Wrong password")
              (res/failResponse {:errmsg "password is not right"
                                 :errcode  52100}))))))))

(def buddytest1
  (-> (hash/sha256 "foo bar")
      (bytes->hex)))

(def buddytest2
  (-> (hash/sha256 (io/input-stream "README.md"))
      (bytes->hex)))

(def buddytest3
  (-> (mac/hash (io/input-stream "README.md") {:key "fish & cat" :alg :hmac+sha256})
      (bytes->hex)))

(def verify3
  (mac/verify (io/input-stream "README.md") (hex->bytes "36d9b5d05649aa65d1a68933c50d9152e335a9ed89fe21bb0f0494f5974b4b7d")
     {:key "fish & cat" :alg :hmac+sha256}))

(def buddyt1
  (GET "/buddyt1" []
    (ok {:message (str buddytest1)} )))

(def buddyt2
  (GET "/buddyt2" []
    (ok {:message (str buddytest2)} )))

(def buddyt3
  (GET "/buddyt3" []
    (ok {:message (str buddytest3)} )))

(def verifyt3
  (GET "/verifyt3" []
    (ok {:message verify3} )))

(def app
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
                  (ok (register! id username pwd)))
             (GET "/login" []
                  :return Response
                  :query-params [id :- s/Int
                                 username :- String
                                 pwd :- String]
                  (ok (login id username pwd))))
    (context "/buddy" []
      :tags ["buddy"]
      buddyt1
      buddyt2
      buddyt3
      verifyt3)
    ))