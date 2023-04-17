;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clojure.data.alpha.replicant.client.spi
  (:require [clojure.data.alpha.replicant.client.protocols :as p]
            [clojure.core.protocols :as corep])
  (:import [clojure.lang IDeref]
           [java.io Writer]))

(defn- throw-unsupported-change-ex [op target]
  (throw (UnsupportedOperationException.
          (str "Operation " op
               " not supported for RDS objects, instead "
               "obtain a copy of the referent collection using the replicant clone function."))))

(deftype Relay [rid remote]
  IDeref
  (deref [this]
    (p/remote-fetch remote this))

  p/IRelay
  (relay-seq [this]
    (p/remote-seq remote this))
  (relay-entry [this k]
    (p/remote-entry remote this k))
  (relay-apply [this args]
    (p/remote-apply remote this args))
  (relay-hasheq [this]
    (p/remote-hasheq remote this))
  (relay-hashcode [this]
    (p/remote-hashcode remote this))
  ;; Unsupported ops
  (relay-cons [this _] (throw-unsupported-change-ex 'cons this))
  (relay-assoc [this _ _] (throw-unsupported-change-ex 'assoc this))
  (relay-dissoc [this _] (throw-unsupported-change-ex 'dissoc this))
  (relay-disj [this _] (throw-unsupported-change-ex 'disjoin this))
  (relay-withmeta [this _] (throw-unsupported-change-ex 'with-meta this))

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

