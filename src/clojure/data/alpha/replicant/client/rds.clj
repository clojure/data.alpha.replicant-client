(ns clojure.data.alpha.replicant.client.rds
  (:require
    [clojure.core.protocols :as corep]
    [clojure.data.alpha.replicant.client.protocols :as p])
  (:import
    [java.io Writer]
    [java.util Collection Map]
    [clojure.lang IDeref Seqable Associative ILookup Sequential Indexed Counted IFn
                  IMeta IPersistentCollection IPersistentStack IPersistentMap IPersistentSet
                  IPersistentVector ArityException]))

(set! *warn-on-reflection* true)

(defn remote-seq
  "Read '#r/seq {:head [h e a d] :rest rid} and return a seq"
  [head rest-relay]
  (concat head
    (reify Seqable
      (seq [_] (p/relay-seq rest-relay)))))

(deftype RemoteVector
  [relay count metadata]
  Associative
  (containsKey [this k] (boolean (.entryAt this k)))
  (entryAt [this k] (p/relay-entry relay k))

  Seqable
  (seq [this] (p/relay-seq relay))

  IPersistentCollection
  (count [this] count)
  (empty [this] [])

  ILookup
  (valAt [this k] (val (.entryAt this k)))

  Sequential

  IPersistentVector

  Collection

  IPersistentStack
  (peek [this]
    (when (pos? count) (.valAt this (dec count))))

  Indexed
  (nth [this n] (.valAt this n))

  Counted

  IMeta
  (meta [this] metadata)

  IFn
  (invoke [this k]
    (.valAt this k))
  (invoke [this k not-found]
    (if-let [e (.entryAt this k)]
      (val e)
      not-found))
  (applyTo [this args]
    (condp = (count args)
      1 (.invoke this (nth args 0))
      2 (.invoke this (nth args 0) (nth args 1))
      (throw (ArityException. (count args) "RemoteVector"))))

  Iterable
  (iterator [this] (clojure.lang.SeqIterator. (seq this))))

(defn remote-vector
  [relay count metadata]
  (->RemoteVector relay count metadata))

(deftype RemoteMap
  [relay count metadata]
  Associative
  (containsKey [this k] (boolean (.entryAt this k)))
  (entryAt [this k] (p/relay-entry relay k))

  Seqable
  (seq [this] (p/relay-seq relay))

  IPersistentCollection
  (count [this] count)
  (empty [this] {})

  ILookup
  (valAt [this k] (val (.entryAt this k)))

  Counted

  IPersistentMap

  Iterable
  (iterator [this] (clojure.lang.SeqIterator. (seq this)))

  Map
  (size [this] count)

  IMeta
  (meta [this] metadata)

  IFn
  (invoke [this k] (.valAt this k))
  (invoke [this k not-found]
          (if-let [e (.entryAt this k)]
            (val e)
            not-found))
  (applyTo [this args]
           (condp = (count args)
             1 (.invoke this (nth args 0))
             2 (.invoke this (nth args 0) (nth args 1))
             (throw (ArityException. (count args) "RemoteMap")))))

(defn remote-map
  [relay count metadata]
  (->RemoteMap relay count metadata))

(deftype RemoteSet
  [relay count metadata]
  clojure.lang.Seqable
  (seq [this] (p/relay-seq relay))

  IPersistentCollection
  (count [this] count)
  (empty [this] #{})

  Collection
  (size [this] count)

  Counted

  IPersistentSet
  (contains [this k] (boolean (p/relay-entry relay k)))
  (get [this k] (val (p/relay-entry relay k)))

  IMeta
  (meta [this] metadata))

  ;;IFn
  ;;Iterable
  
(defn remote-set
  [relay count metadata]
  (->RemoteSet relay count metadata))

(deftype RemoteFn
  [relay]

  IDeref
  (deref [this] this)

  IFn
  (invoke [_] (p/relay-apply relay []))
  (invoke [_ a1] (p/relay-apply relay [a1]))
  (invoke [_ a1 a2] (p/relay-apply relay [a1 a2]))
  (invoke [_ a1 a2 a3] (p/relay-apply relay [a1 a2 a3]))
  (invoke [_ a1 a2 a3 a4] (p/relay-apply relay [a1 a2 a3 a4]))
  (invoke [_ a1 a2 a3 a4 a5] (p/relay-apply relay [a1 a2 a3 a4 a5]))
  (invoke [_ a1 a2 a3 a4 a5 a6] (p/relay-apply relay [a1 a2 a3 a4 a5 a6]))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7] (p/relay-apply relay [a1 a2 a3 a4 a5 a6 a7]))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8] (p/relay-apply relay [a1 a2 a3 a4 a5 a6 a7 a8]))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9] (p/relay-apply relay [a1 a2 a3 a4 a5 a6 a7 a8 a9]))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10] (p/relay-apply relay [a1 a2 a3 a4 a5 a6 a7 a8 a9 a10]))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11] (p/relay-apply relay [a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11]))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12] (p/relay-apply relay [a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12]))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13] (p/relay-apply relay [a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13]))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14] (p/relay-apply relay [a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14]))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15] (p/relay-apply relay [a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15]))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16] (p/relay-apply relay [a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16]))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17] (p/relay-apply relay [a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17]))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18] (p/relay-apply relay [a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18]))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19] (p/relay-apply relay [a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19]))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19 a20] (p/relay-apply relay [a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19 a20]))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19 a20 a21] (p/relay-apply relay [a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19 a20 a21]))
  (applyTo [_ args] (p/relay-apply relay args))

  corep/Datafiable
  (datafy [this] this))

(defn remote-fn
  [relay]
  (->RemoteFn relay))