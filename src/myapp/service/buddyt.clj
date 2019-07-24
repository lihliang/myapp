(ns myapp.service.buddyt
  (:require [buddy.core.hash :as hash]
            [buddy.core.codecs :refer :all]
            [buddy.core.mac :as mac]
            [clojure.java.io :as io]
            [ring.util.response :refer [response]]))

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
