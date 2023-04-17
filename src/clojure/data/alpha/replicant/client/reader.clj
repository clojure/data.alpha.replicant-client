;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clojure.data.alpha.replicant.client.reader
  (:require
   [clojure.data.alpha.replicant.client.spi :as spi]
   [clojure.data.alpha.replicant.client.rds :as rds])
  (:import
    [clojure.lang MapEntry]))

;; {'r/id #'rid-reader
;;  'r/seq #'seq-reader
;;  'r/kv #'kv-reader
;;  'r/vec #'vector-reader
;;  'r/map #'map-reader
;;  'r/set #'set-reader
;;  'r/object #'object-reader
;;  'r/fn #'fn-reader}

(def ^:dynamic *remote-client* nil)

(defn rid-reader
  "Read '#r/id id' and return a relay"
  [rid]
  (spi/relay rid *remote-client*))

(defn seq-reader
  "Read '#r/seq {:head [h e a d] :rest rid} and return a seq"
  [{:keys [head rest] :as m}]
  (rds/remote-seq head (rid-reader rest)))

(defn kv-reader
  "Read '#r/kv [k v] and return a map entry"
  [entry]
  (MapEntry. (nth entry 0) (nth entry 1)))

(defn vector-reader
  "Read '#r/vec {:id rid :count N}' and return a vector"
  [{:keys [id count meta] :as m}]
  (rds/remote-vector (rid-reader id) count meta))

(defn map-reader
  "Read '#r/map {:id rid :count N}' and return a vector"
  [{:keys [id count meta] :as m}]
  (rds/remote-map (rid-reader id) count meta))

(defn set-reader
  "Read '#r/set {:id rid :count N}' and return a vector"
  [{:keys [id count meta] :as m}]
  (rds/remote-set (rid-reader id) count meta))

(defn fn-reader
  "Read '#r/fn {:id id}' and return an IFn"
  [{:keys [id] :as m}]
  (rds/remote-fn (rid-reader id)))

(defn object-reader
  "Read '#r/object {:klass c, :ref rid}' and return a map describing the object"
  [{:keys [klass ref] :as m}]
  {:class klass
   :ref   ref})
