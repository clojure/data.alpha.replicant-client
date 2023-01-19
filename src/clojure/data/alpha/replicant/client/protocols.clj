(ns clojure.data.alpha.replicant.client.protocols)

(defprotocol IRemote
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
    "Invoke remote function with args"))

(defprotocol IRelay
  (relay-seq [this] "Get seq of remote object")
  (relay-entry [this k] "Get entry of remote object")
  (relay-apply [this args] "Apply remote object with args"))
