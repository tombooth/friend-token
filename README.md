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
            [tombooth.friend-token.token :as token]))

(def users {"friend" {:username "friend"
                      :password (creds/hash-bcrypt "clojure")
                      :roles #{::user}}})

(def tokens (atom {}))

(defonce secret-key (token/generate-key))

(defroutes app-routes
  (GET "/" [] (friend/authenticated "Authenticated Hello!!"))
  (GET "/un" [] "Unauthenticated Hello")
  (route/resources "/")
  (route/not-found "Not Found"))

(defn create-token [user-record]
  (let [id (:username user-record)
        token (token/create-token secret-key id)]
    (swap! tokens assoc token id)
    token))

(defn verify-token [token]
  (if-let [id (@tokens token)]
    (if (token/verify-token secret-key token id)
      (users id))))

(def secured-app (friend/authenticate
                   app-routes
                   {:allow-anon? true
                    :unauthenticated-handler #(token-workflow/token-deny %)
                    :login-uri "/authenticate"
                    :workflows [(token-workflow/token
                                  :token-header "X-Auth-Token"
                                  :credential-fn (partial creds/bcrypt-credential-fn users)
                                  :create-token-fn create-token
                                  :verify-token-fn verify-token )]}))

(def app
  (handler/api secured-app))
```

## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
