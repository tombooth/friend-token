# tombooth/friend-token

A token workflow for apis using the Friend middleware for authentication

## Usage

```clojure
[com.tombooth/friend-token "0.1.0-SNAPSHOT"]
```

## Example compojure app

```clojure
(ns api.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [cemerick.friend :as friend]
            (cemerick.friend [credentials :as creds])
            [tombooth.friend-token.workflow :as token-workflow]
            [tombooth.friend-token.token-store :as store]
            [tombooth.friend-token.token :as token]))

(def users {"friend" {:username "friend"
                      :password (creds/hash-bcrypt "clojure")
                      :roles #{::user}}})

(defonce secret-key (token/generate-key))

(def token-header "X-Auth-Token")
(def token-store
  (store/->MemTokenStore secret-key 30 (atom {})))

(defroutes app-routes
  (GET "/" [] (friend/authenticated "Authenticated Hello!!"))
  (GET "/un" [] "Unauthenticated Hello")
  (POST "/extend-token" [:as request]
    (if-let [token-hex (token/from-request request token-header)]
      (do (store/extend-life token-store token-hex) {:status 200})
      {:status 401}))
  (POST "/destroy-token" [:as request]
    (if-let [token-hex (token/from-request request token-header)]
      (do (store/destroy token-store token-hex) {:status 200})
      {:status 401}))
  (route/resources "/")
  (route/not-found "Not Found"))

(def secured-app (friend/authenticate
                   app-routes
                   {:allow-anon? true
                    :unauthenticated-handler #(token-workflow/token-deny %)
                    :login-uri "/authenticate"
                    :workflows [(token-workflow/token
                                  :token-header token-header
                                  :credential-fn (partial creds/bcrypt-credential-fn users)
                                  :token-store token-store
                                  :get-user-fn users )]}))

(def app
  (handler/api secured-app))
```

## License

Copyright © 2013 Thomas Booth

Distributed under the Eclipse Public License, the same as Clojure.
