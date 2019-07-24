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
            [buddy.core.mac :as mac]
            [clojure.data.json :as json]
            [liberator.core :refer [defresource]])
  (:gen-class)
  (:import (java.net URL)))

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

;; convert the body to a reader. Useful for testing in the repl
;; where setting the body to a string is much simpler.
(defn body-as-string [ctx]
  (if-let [body (get-in ctx [:request :body])]
    (condp instance? body
      java.lang.String body
      (slurp (io/reader body)))))

;; For PUT and POST parse the body as json and store in the context
;; under the given key.
(defn parse-json [ctx key]
  (when (#{:put :post} (get-in ctx [:request :request-method]))
    (try
      (if-let [body (body-as-string ctx)]
        (let [data (json/read-str body)]
          [false {key data}])
        {:message "No body"})
      (catch Exception e
        (.printStackTrace e)
        {:message (format "IOException: %s" (.getMessage e))}))))

;; For PUT and POST check if the content type is json.
(defn check-content-type [ctx content-types]
  (if (#{:put :post} (get-in ctx [:request :request-method]))
    (or
      (some #{(get-in ctx [:request :headers "content-type"])}
            content-types)
      [false {:message "Unsupported Content-Type"}])
    true))

;; we hold a entries in this ref
(defonce entries (ref {}))

;; a helper to create a absolute url for the entry with the given id
(defn build-entry-url [request id]
  (URL. (format "%s://%s:%s%s/%s"
                (name (:scheme request))
                (:server-name request)
                (:server-port request)
                (:uri request)
                (str id))))


;; create and list entries
(defresource list-resource
             :available-media-types ["application/json"]
             :allowed-methods [:get :post]
             :known-content-type? #(check-content-type % ["application/json"])
             :malformed? #(parse-json % ::data)
             :post! #(let [id (str (inc (rand-int 100000)))]
                       (dosync (alter entries assoc id (::data %)))
                       {::id id})
             :post-redirect? true
             :location #(build-entry-url (get % :request) (get % ::id))
             :handle-ok #(map (fn [id] (str (build-entry-url (get % :request) id)))
                              (keys @entries)))

(defresource entry-resource [id]
             :allowed-methods [:get :put :delete]
             :known-content-type? #(check-content-type % ["application/json"])
             :exists? (fn [_]
                        (let [e (get @entries id)]
                          (if-not (nil? e)
                            {::entry e})))
             :existed? (fn [_] (nil? (get @entries id ::sentinel)))
             :available-media-types ["application/json"]
             :handle-ok ::entry
             :delete! (fn [_] (dosync (alter entries assoc id nil)))
             :malformed? #(parse-json % ::data)
             :can-put-to-missing? false
             :put! #(dosync (alter entries assoc id (::data %)))
             :new? (fn [_] (nil? (get @entries id ::sentinel))))


(defroutes collection-example
  (ANY ["/collection/:id{[0-9]+}"] [id] (entry-resource id))
  (ANY "/collection" [] list-resource))

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