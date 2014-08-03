# tombooth/friend-token

A token workflow for apis using the Friend middleware for authentication.

## Dependency

```clojure
[com.tombooth/friend-token "0.1.1-SNAPSHOT"]
```

## Example

### Clojure App

```clojure
(ns api.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [cemerick.friend :as friend]
            [cemerick.friend [credentials :as creds]]
            [tombooth.friend-token :as friend-token]
            [tombooth.friend-token.token-store :as store]))

(def users {"friend" {:username "friend"
                      :password (creds/hash-bcrypt "clojure")
                      :roles #{::user}}})

(defonce secret-key (friend-token/generate-key))


(def token-store
  (store/->MemTokenStore secret-key     ;; Key used to sign tokens
                         30             ;; TTL of tokens in seconds
                         (atom {})))    ;; Hash to store tokens -> users

(defroutes app-routes
  (GET "/" [] (friend/authenticated "Authenticated Hello!!"))
  (GET "/un" [] "Unauthenticated Hello")
  (POST "/extend-token" []
    (friend/authenticated
      (friend-token/extend-life
        {:status 200 :headers {}})))
  (POST "/destroy-token" []
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

### cURL Usage
The `:login-uri` path expects a JSON POST response containing a `username` and `password` key:

``` curl
$ curl -X POST  -H "Content-type: application/json" -d '{"username": "friend", "password": "clojure"}' http://localhost:3000/authenticate -i

HTTP/1.1 200 OK
Date: Sun, 03 Aug 2014 14:15:45 GMT
X-Auth-Token: 0000000000000006667269656e64000001479c39e403e6c7b7d7c7e95e72adae7f33b7e876fe683932b0f82a5e60f6cc18f912d697409139dc874491a89d8e58e39c8a9af160f43cd2e03ebb3269b403200d943c0d87
Content-Length: 0
Server: Jetty(7.6.13.v20130916)

```

You can then use the `X-Auth-Token` value to make subsequent requests to secured resources:

``` curl
$ curl -i 'http://localhost:3000' -H "Accept: application/json" -H "X-Auth-Token: 0000000000000006667269656e64000001479c39e403e6c7b7d7c7e95e72adae7f33b7e876fe683932b0f82a5e60f6cc18f912d697409139dc874491a89d8e58e39c8a9af160f43cd2e03ebb3269b403200d943c0d87"

HTTP/1.1 200 OK
Date: Sun, 03 Aug 2014 14:17:14 GMT
Set-Cookie: ring-session=fb8a674f-d703-4fb8-b489-917c5af961cf;Path=/
Content-Type: text/html;charset=UTF-8
Content-Length: 21
Server: Jetty(7.6.13.v20130916)

Authenticated Hello!!⏎

```

## License

Copyright © 2013-2014 Thomas Booth

Distributed under the Eclipse Public License, the same as Clojure.
