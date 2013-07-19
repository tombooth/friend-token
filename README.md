# tombooth/friend-token

A token workflow for apis using the Friend middleware for authentication.

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
            [tombooth.friend-token :as friend-token]
            [tombooth.friend-token.token-store :as store]))

(def users {"friend" {:username "friend"
                      :password (creds/hash-bcrypt "clojure")
                      :roles #{::user}}})

(defonce secret-key (friend-token/generate-key))

(def token-store
  (store/->MemTokenStore secret-key 30 (atom {})))

(defroutes app-routes
  (GET "/" [] (friend/authenticated "Authenticated Hello!!"))
  (GET "/un" [] "Unauthenticated Hello")
  (POST "/extend-token" [:as request]
    (friend/authenticated
      (friend-token/extend-life
        {:status 200 :headers {}})))
  (POST "/destroy-token" [:as request]
    (friend/authenticated
      (friend-token/destroy
        {:status 200 :headers {}})))
  (route/resources "/")
  (route/not-found "Not Found"))

(def secured-app (friend/authenticate
                   app-routes
                   {:allow-anon? true
                    :unauthenticated-handler #(friend-token/workflow-deny %)
                    :login-uri "/authenticate"
                    :workflows [(friend-token/workflow
                                  :token-header "X-Auth-Token"
                                  :credential-fn (partial creds/bcrypt-credential-fn users)
                                  :token-store token-store
                                  :get-user-fn users )]}))

(def app
  (handler/api secured-app))
```

## License

Copyright © 2013 Thomas Booth

Distributed under the Eclipse Public License, the same as Clojure.
