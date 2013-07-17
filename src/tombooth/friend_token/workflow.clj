(ns tombooth.friend-token.workflow
  (:require [cemerick.friend :as friend]
            [cemerick.friend.workflows :as workflows]
            [ring.util.request :as req]
            [clojure.tools.logging :as logging]
            [cheshire.core :as json])
  (:use [cemerick.friend.util :only (gets)]))

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
  [{:keys [credential-fn create-token-fn token-header] :as config} request]
  ; we expect a json body with :username and :password
  (if-let [body (:body request)]
    (let [{:keys [username password] :as creds} (json/parse-string (slurp body) true)]
      ;check to see if the credentials are valid
      (if-let [user-record (and username password
                               (credential-fn (with-meta creds {::friend/workflow :token})))]

        (let [session-token (create-token-fn user-record)]
          (workflows/make-auth user-record
            {::friend/workflow :token
             ::friend/redirect-on-auth? false})
          {:status 200 :headers {token-header session-token}})

        (token-deny)))

    {:status 400 :headers {"Content-Type" "text/plain"}}))

(defn- read-token
  [{:keys [token-header verify-token-fn] :as config} {:keys [headers] :as request}]
  (if-let [session-token (headers (.toLowerCase token-header))]
    (if-let [user-record (verify-token-fn session-token)]
      (workflows/make-auth user-record
        {::friend/workflow :token
         ::friend/redirect-on-auth? false}))))

(defn token
  [& {:as config}]
  (fn [request]
    (if
      (login-url? config request)
      (authenticate config request)
      (read-token config request))))



