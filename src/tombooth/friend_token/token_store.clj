(ns tombooth.friend-token.token-store
  (:require [clj-time.core :as time]
            [tombooth.friend-token.token :as token]))

(defprotocol TokenStore
  (create [instance user])
  (get-metadata [instance token-hex])
  (verify [instance token-hex])
  (extend-life [instance token-hex])
  (destroy [instance token-hex]))

(defn- within-ttl?
  [token-data ttl]
  (let [seconds-passed (time/in-seconds (time/interval (:created token-data) (time/now)))]
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

  (get-metadata [this token-hex]
    (if-let [token-data (@tokens token-hex)]
      (let [id (:id token-data)]
        (if (and (token/verify-token key token-hex id)
             (within-ttl? token-data ttl))
          token-data))))

  (verify [this token-hex]
    (if-let [token-data (get-metadata this token-hex)]
      (:id token-data)
      (destroy this token-hex)))

  (extend-life [this token-hex]
    (if-let [token-data (get-metadata this token-hex)]
      (let [new-token-data (assoc token-data :created (time/now))]
        (swap! tokens assoc token-hex new-token-data))
      (destroy this token-hex)))

  (destroy [this token-hex]
    (swap! tokens dissoc token-hex)))