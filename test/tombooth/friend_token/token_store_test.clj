(ns tombooth.friend-token.token-store-test
  (:require [clojure.test :refer :all]
            [tombooth.friend-token.token :as token]
            [tombooth.friend-token.token-store :as store]))

(deftest create-test
  (testing "Test creating a token"
    (let [tokens (atom {})
          token-store (store/->MemTokenStore (token/generate-key) 10 tokens)]
      (store/create token-store {:username "foo"})
      (is (= 1 (count (keys @tokens)))))))

(deftest get-metadata-test
  (testing "Test we can get metadata"
    (let [tokens (atom {})
          token-store (store/->MemTokenStore (token/generate-key) 10 tokens)
          token-hex (store/create token-store {:username "foo"})]
      (is (store/get-metadata token-store token-hex)))))

(deftest valid-fail-ttl-test
  (testing "Test getting metadata fails if outside ttl"
    (let [tokens (atom {})
          token-store (store/->MemTokenStore (token/generate-key) 1 tokens)
          token-hex (store/create token-store {:username "foo"})]
      (Thread/sleep 2000)
      (is (not (store/get-metadata token-store token-hex))))))

(deftest valid-extend-test
  (testing "Test extend works"
    (let [tokens (atom {})
          token-store (store/->MemTokenStore (token/generate-key) 2 tokens)
          token-hex (store/create token-store {:username "foo"})]
      (Thread/sleep 1000)
      (store/extend-life token-store token-hex)
      (Thread/sleep 1500)
      (is (store/get-metadata token-store token-hex)))))

(deftest destroy-test
  (testing "Test destroying a token"
    (let [tokens (atom {"foo" {}})
          token-store (store/->MemTokenStore (token/generate-key) 10 tokens)]
      (store/destroy token-store "foo")
      (is (= 0 (count (keys @tokens)))))))
