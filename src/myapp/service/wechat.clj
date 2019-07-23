(ns myapp.service.wechat (:require
                           [clj-http.client :as client]
                           [clojure.tools.logging :as log]
                           [cheshire.core :as cheshire]
                           [ring.util.codec :as encoder]
                           [myapp.utils.response :as res]
                           ))

(def APP_ID "wx2dc64fe9107b1a39")
(def APP_SECRET "153607d2ca216762f14b839a50be7ffa")
(def SCOPE "snsapi_userinfo")
(def STATE "state")
(def TIMEOUT 200)

(defn get-wechat-accesstoken [code]
  (let [result (client/get "https://api.weixin.qq.com/sns/oauth2/access_token"
                           {:query-params {:appid      APP_ID
                                           :secret     APP_SECRET
                                           :code       code
                                           :grant_type "authorization_code"}
                            :timeout      TIMEOUT
                            :as :json})
        responsebody (:body result)]
    (log/info "access token response:" responsebody)
    (if (:errcode responsebody)
      (do
        (log/error "get access token failed:" (:errcode responsebody))
        (res/failResponse responsebody))
      (res/succResponse responsebody))))

(defn get-wechat-info
  [access_token openid]
  (let [result (client/get "https://api.weixin.qq.com/sns/userinfo"
                           {:query-params {:access_token access_token
                                           :openid openid
                                           :lang "zh_CN"}
                            :timeout TIMEOUT
                            :as :json})
        weixin-response (:body result)]
    (log/info "userinfo response: " weixin-response)
    (if (:errcode weixin-response)
      (do
        (log/error "get open user info failed: " (:errcode weixin-response))
        (res/failResponse weixin-response))
      (res/succResponse weixin-response))))

(defn wechat-info
  [code state]
  (let [responsebody (get-wechat-accesstoken code)
        success (:isSuccess responsebody)
        result (:result responsebody)]
    (if success
      (get-wechat-info (:access_token result) (:openid result))
      responsebody)))


(defn wechat-auth-info
  []
  (let [query-params {:appid APP_ID
                      :redirect_uri (encoder/url-encode "http://47.103.91.241/api/wechat-info")
                      :response_type "code"
                      :scope SCOPE
                      :state STATE}]
    (format "https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=%s&scope=%s&state=%s#wechat_redirect"
            (:appid query-params)
            (:redirect_uri query-params)
            (:response_type query-params)
            (:scope query-params)
            (:state query-params)))
  )