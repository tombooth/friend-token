(ns tombooth.friend-token.token
  (:import java.security.SecureRandom
           java.nio.ByteBuffer
           javax.crypto.Mac
           javax.crypto.spec.SecretKeySpec))

(defn- hexify [bs]
  (let [hex [\0 \1 \2 \3 \4 \5 \6 \7 \8 \9 \a \b \c \d \e \f]]
    (letfn [(hexify-byte [b]
              (let [v (bit-and b 0xFF)]
                [(hex (bit-shift-right v 4)) (hex (bit-and v 0x0F))]))]
      (apply str (mapcat hexify-byte bs)))))

(defn- unhexify [s]
  (letfn [(unhexify-2 [c1 c2]
            (unchecked-byte
              (+ (bit-shift-left (Character/digit c1 16) 4)
                (Character/digit c2 16))))]
    (map #(apply unhexify-2 %) (partition 2 s))))

(defn- random-bytes
  [length]
  (let [gen (new SecureRandom) key (byte-array length)]
    (.nextBytes gen key)
    key))

(defn- long-to-bytes
  [l]
  (.array (.putLong (ByteBuffer/allocate 8) l)))

(defn- bytes-to-long
  [byte-seq]
  (let [bs (byte-array (take 8 byte-seq))]
    (.getLong (ByteBuffer/wrap bs))))

(defn- current-time-bytes [] (long-to-bytes (System/currentTimeMillis)))

(defn- concat-bytes [& arrs] (byte-array (apply concat arrs)))

(defn- encode
  [key data]
  (let [hmac (Mac/getInstance "HmacSHA256")
        secret-key (SecretKeySpec. key "HmacSHA256")]
    (.init hmac secret-key)
    (.doFinal hmac data)))

(defn generate-key
  ([] (generate-key 128))
  ([size] (random-bytes size)))

(defn create-token
  [key id]
  (let [id-bytes (.getBytes id)
        id-length (long-to-bytes (alength id-bytes))
        current-time-bytes (current-time-bytes)
        random-bytes (random-bytes 32)
        message (concat-bytes id-length id-bytes current-time-bytes random-bytes)
        signature (encode key message)]
    (hexify (concat-bytes message signature))))

(defn verify-token
  [key token id]
  (let [token-bytes (unhexify token)
        id-length (bytes-to-long token-bytes)
        token-id (apply str (map char (byte-array (take id-length (drop 8 token-bytes)))))]
    (if (= token-id id)
      (let [message-length (+ 8 id-length 8 32)
            message (byte-array (take message-length token-bytes))
            token-signature (drop message-length token-bytes)
            signature (encode key message)]
        (= token-signature (seq signature))))))





