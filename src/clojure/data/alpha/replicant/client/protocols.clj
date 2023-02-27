(ns clojure.data.alpha.replicant.client.protocols)

(defprotocol IRemoteRead
  (remote-fetch
    [this rid]
    [this rid depth-opts]
    "Returns remote object for rid")
  (remote-seq
    [this rid]
    [this rid depth-opts]
    "Returns seq of remote object for rid")
  (remote-entry
    [this rid k] ;; not-found?
    [this rid k depth-opts]
    "Returns entry of remote object for rid")
  (remote-string
    [this rid]
    "Returns toString of remote object for rid")
  (remote-datafy
    [this rid]
    "Returns datafy of remote object for rid")
  (remote-apply
    [this rid args]
    "Invoke remote function with args")
  (remote-hasheq [this rid]
    "Fetch the hasheq of the object refered to by rid")
  (remote-hashcode [this rid]
    "Fetch the hashCode of the object refered to by rid"))

(defprotocol IRemoteWrite
  (remote-cons
    [this rid elem]
    "Cons the object elem to the collection for rid")
  (remote-assoc
    [this rid k v]
    "Assoc the value v to the collection for rid at key k.")
  (remote-dissoc
    [this rid k]
    "Dissoc the key k from the collection for rid."))

(defprotocol IRelay
  ;; Read side
  (relay-seq [this] "Get seq of remote object")
  (relay-entry [this k] "Get entry of remote object")
  (relay-apply [this args] "Apply remote object with args")
  (relay-hasheq [this] "Get hasheq of the object refered to by rid")
  (relay-hashcode [this] "Get hashCode of the object refered to by rid")
  ;; Write side
  (relay-cons [this elem] "Cons the object elem to the collection for rid")
  (relay-assoc [this k v] "Assoc the value v to the collection for rid at key k.")
  (relay-dissoc [this k] "Dissoc the key k from the collection for rid."))
