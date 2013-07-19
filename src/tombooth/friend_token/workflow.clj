(ns tombooth.friend-token.workflow
  (:require [cemerick.friend :as friend]
            [cemerick.friend.workflows :as workflows]
            [tombooth.friend-token.token-store :as store]
            [tombooth.friend-token.token :as token]
            [ring.util.request :as req]
            [clojure.tools.logging :as logging]
            [cheshire.core :as json])
  (:use [cemerick.friend.util :only (gets)]))

(defn generate-key
  ([] (generate-key 128))
  ([size] (token/random-bytes size)))

(defn token-deny
  [& foo]
  {:status 401
   :headers {"Content-Type" "text/plain"}})

(defn- login-url?
  [config request]
  (and (= (gets :login-uri config (::friend/auth-config request))
          (req/path-info request))
       (= :post (:request-method request))))

(defn- authenticate
  [{:keys [credential-fn token-store token-header] :as config} request]
  ; we expect a json body with :username and :password
  (if-let [body (:body request)]
    (let [{:keys [username password] :as creds} (json/parse-string (slurp body) true)]
      ;check to see if the credentials are valid
      (if-let [user-record (and username password
                               (credential-fn (with-meta creds {::friend/workflow ::token})))]

        (let [session-token (store/create token-store user-record)]
          (workflows/make-auth user-record
            {::friend/workflow ::token
             ::friend/redirect-on-auth? false
             ::token-hex session-token
             ::token-store token-store})
          {:status 200 :headers {token-header session-token}})

        (token-deny)))

    {:status 400 :headers {"Content-Type" "text/plain"}}))

(defn- read-token
  [{:keys [token-header token-store get-user-fn] :as config} request]
  (if-let [session-token (token/from-request request token-header)]
    (if-let [user-record (get-user-fn (store/verify token-store session-token))]
      (workflows/make-auth user-record
        {::friend/workflow ::token
         ::friend/redirect-on-auth? false
         ::token-hex session-token
         ::token-store token-store}))))

(defn token
  [& {:as config}]
  (fn [request]
    (if
      (login-url? config request)
      (authenticate config request)
      (read-token config request))))


(defmacro extend-life
  [& body]
  `(if-let [auth-map# (friend/current-authentication)]
     (let [auth-meta# (meta auth-map#)
           token-hex# (::token-hex auth-meta#)
           token-store# (::token-store auth-meta#)]
       (store/extend-life token-store# token-hex#))))

(defmacro destroy
  [& body]
  `(if-let [auth-map# (friend/current-authentication)]
     (let [auth-meta# (meta auth-map#)
           token-hex# (::token-hex auth-meta#)
           token-store# (::token-store auth-meta#)]
       (store/destroy token-store# token-hex#))))

