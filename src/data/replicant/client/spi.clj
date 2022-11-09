(ns data.replicant.client.spi
  (:require [data.replicant.client.protocols :as p]
            [clojure.core.protocols :as corep])
  (:import [clojure.lang IDeref]
           [java.io Writer]))

(deftype Relay [rid remote]
  #_IDeref
  #_(deref [this] this)

  p/IRelay
  (relay-seq [this]
    (p/remote-seq remote this))
  (relay-entry [this k]
    (p/remote-entry remote this k))
  (relay-apply [this args]
    (p/remote-apply remote this args))

  Object
  (toString [this]
    (p/remote-string remote this))

  corep/Datafiable
  (datafy [this]
    (p/remote-datafy remote this)))

(defmethod print-method Relay [^Relay relay ^Writer w]
  (.write w (str "#l/id "))
  (@#'print (.-rid relay)))

(defmethod print-dup Relay [^Relay relay ^Writer w]
  (.write w (str "#l/id "))
  (@#'print (.-rid relay)))

(defn relay
  [rid remote]
  (->Relay rid remote))

(comment

  (require '[clojure.pprint :as pp])

  (str
   (binding []
     (pp/with-pprint-dispatch pp/code-dispatch
       (with-out-str
         (pp/pprint
          (relay (java.util.UUID/randomUUID) nil)
          )))))

)
