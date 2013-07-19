(ns tombooth.friend-token.token-store
  (:require [clj-time.core :as time]
            [tombooth.friend-token.token :as token]))

(defprotocol TokenStore
  (create [instance user])
  (valid-token? [instance token-hex])
  (verify [instance token-hex])
  (extend-life [instance token-hex])
  (destroy [instance token-hex]))

(defn- within-ttl?
  [token-data ttl]
  (let [seconds-passed (time/in-secs (time/interval (:created token-data) (time/now)))]
    (< seconds-passed ttl)))

(defrecord MemTokenStore
  [key ttl tokens]

  TokenStore

  (create [this user]
    (let [id (:username user)
          token-hex (token/create-token key id)]
      (swap! tokens assoc token-hex {
        :id id
        :created (time/now)
      })
      token-hex))

  (valid-token? [this token-hex]
    (if-let [token-data (@tokens token-hex)]
      (let [id (:id token-data)]
        (and (token/verify-token key token-hex id)
             (within-ttl? token-data ttl)))))

  (verify [this token-hex]
    (if (valid-token? this token-hex)
      (:id (@tokens token-hex))
      (destroy this token-hex)))

  (extend-life [this token-hex]
    (if (valid-token? this token-hex)
      (let [token-data (@tokens token-hex)
            new-token-data (assoc token-data :created (time/now))]
        (swap! tokens assoc token-hex new-token-data))))

  (destroy [this token-hex]
    (swap! tokens dissoc token-hex)))