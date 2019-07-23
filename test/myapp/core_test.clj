(ns myapp.core-test
  (:require [clojure.test :refer :all]
            [myapp.core :refer :all]
            [myapp.database.mongodb :as mg]
            [cheshire.core :as cheshire]
            [ring.mock.request :as mock]))

(defn db_init
  "init mongodb"
  [test-fn]
  (mg/clear-orders)
  (mg/clear-products)
  (mg/clear-users)
  (test-fn))

(deftest testcase
  (testing "hello route"
    (let [response (app (mock/request :get "/api/hello"))
          body (cheshire/parse-string (slurp (:body response)) true)]
      (is (= (:status response) 200))
      (is (= (:message body) "hello"))))

  (testing
    "wechat-info route"
    (let [response (app (-> (mock/request :get "/api/wechat-info")
                            (mock/query-string {:code "code" :state "state"})))
          body (cheshire/parse-string (slurp (:body response)) true)]
      (is (= (:status response) 200))
      (is (= (select-keys body [:isSuccess :errcode :result]){
                                                              :isSuccess false
                                                              :errcode 40029
                                                              :result {}}))))

  (testing
    "wechat-auth-info"
    (let [response (app (mock/request :get "/api/wecha-auth-info"))
          body (cheshire/parse-string (slurp (:body response)) true)]
      (is (= (:status response) 0))
      (is (nil? body))))

  (testing
    "register route"
    (let [response (app (-> (mock/request :get "/user/register")
                            (mock/query-string {:id 132
                                                :username "edward"
                                                :pwd "123"})))
          body (cheshire/parse-string (slurp (:body response)) true)]
      (is (= (:status response) 200))
      (is (= (select-keys body [:result]) {"edward" 132}))))

  (testing
    "login route"
    (let [response (app (-> (mock/request :get "/user/login")
                            (mock/query-string {:id 132
                                                :username "edward"
                                                :pwd "123"})))
          body (cheshire/parse-string (slurp (:body response)) true)]
      (is (= (:status response) 200))
      (is (= (select-keys body [:result]) {:message "login succeed"})))))


