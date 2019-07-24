(ns myapp.service.user
  (:require
    [clojure.tools.logging :as log]
    [myapp.database.mongodb :as mg]
    [myapp.utils.response :as res]
    ))

(defn login
  [openid]
  (let [user (mg/find-user-by-openid openid)]
    (if (nil? user)
      (do (log/warn "unregister user, openid: " openid)
          (res/failResponse 40200 "unregister user"))
      (do (log/info "user login, openid: " openid)
          (res/succResponse (.toJson user))))))

(defn register
  [userinfo]
  (let [{:keys [openid]} userinfo
        exist (mg/find-user-by-openid openid)]
    (cond (nil? openid) (do (log/error "openid is empty")
                            (res/failResponse 40000 "empty openid"))
          (some? exist) (do (log/error "openid has already been registered")
                            (res/failResponse 40100 "already registered"))
          :else (do (mg/add-user userinfo)
                    (log/info "user registered, openid: " openid)
                    (res/succResponse "register succeed")))))

(defn order
  [openid pid]
  (let [user (mg/find-user-by-openid openid)
        product (mg/find-product-by-id pid)
        order_able (and user product)]
    (cond (not order_able)
          (do (log/error "invalid openId or productId, openid: " openid ", pid: " pid)
              (res/failResponse 40200 "invalid order"))

          (nil? (mg/add-order openid pid))
          (do (log/error "error creating order")
              (res/failResponse 40300 "order creation failed"))

          :else
          (do (log/info "new order created, openid: " openid ", pid: " pid)
              (res/succResponse {:status "succeed"})))))

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

(defn login! [id name pwd]
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