(ns clojure.data.alpha.replicant.client.rds
  (:require
    [clojure.core.protocols :as corep]
    [clojure.data.alpha.replicant.client.protocols :as p])
  (:import
    [java.io Writer]
    [java.util Collection List Map Map$Entry Set]
    [clojure.lang IDeref Seqable Associative ILookup Sequential Indexed Counted IFn
                  IMeta IPersistentCollection IPersistentStack IPersistentMap IPersistentSet
                  IPersistentVector ArityException MapEquivalence IHashEq IObj]))

(set! *warn-on-reflection* true)

(defn- rds-eq-sequential
  [x y eq-fun]
  (boolean
   (when (sequential? y)
     (loop [xs (seq x) ys (seq y)]
       (cond (nil? xs) (nil? ys)
             (nil? ys) false
             (eq-fun (first xs) (first ys)) (recur (next xs) (next ys))
             :else false)))))

(deftype NeverEq []
  IPersistentCollection
  (equiv [_ _] false)
  Object
  (equals [_ _] false))

(def ^:private never-eq (NeverEq.))

(defn- rds-eq-map
  [x y eq-fun]
  (boolean
   (when (map? y)
     (when (== (count x) (count y))
       (every? identity
               (map (fn [xkv]
                      (eq-fun (get y (first xkv) never-eq)
                              (second xkv)))
                    x))))))

(defn- sequential-contains-all [coll objs]
  (reduce (fn [_ elem]
            (let [has (some #(= elem %) coll)]
              (if has
                (boolean has)
                (reduced false))))
          true
          objs))

(defn- rds-eq-set
  [x y]
  (every? #(contains? x %) y))

(defn remote-seq
  "Read '#r/seq {:head [h e a d] :rest rid} and return a seq"
  [head rest-relay]
  (concat head
    (reify Seqable
      (seq [_] (p/relay-seq rest-relay)))))

(deftype RemoteVector
  [relay count metadata ^:volatile-mutable _hasheq ^:volatile-mutable _hashcode]
  Associative
  (containsKey [this k] (boolean (.entryAt this k)))
  (entryAt [this k] (p/relay-entry relay k))
  (assoc [this k v] (p/relay-assoc relay k v))

  Seqable
  (seq [this] (p/relay-seq relay))

  IPersistentCollection
  (count [this] count)
  (empty [this] [])
  (equiv [this other]
    (if (instance? clojure.lang.PersistentVector other)
      (if (== count (clojure.core/count other))
        (rds-eq-sequential this other #(clojure.lang.Util/equiv %1 %2))
        false)
      (rds-eq-sequential this other #(clojure.lang.Util/equiv %1 %2))))
  (cons [this elem]
    (p/relay-cons relay elem))

  ILookup
  (valAt [this k] (val (.entryAt this k)))

  Sequential

  IPersistentVector

  Collection

  IPersistentStack
  (peek [this]
    (when (pos? count) (.valAt this (dec count))))
  (pop [this]
    (cond (zero? count) (throw (IllegalStateException. "Can't pop empty vector"))
          (== count 1)  []
          (pos? count)  (vec (butlast this))))

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
    (condp = (clojure.core/count args)
      1 (.invoke this (nth args 0))
      2 (.invoke this (nth args 0) (nth args 1))
      (throw (ArityException. (clojure.core/count args) "RemoteVector"))))

  List
  (contains [this obj]
    (boolean (some #(= obj %) this)))
  (containsAll [this objs]
    (sequential-contains-all this objs))
  (indexOf [this obj]
    (reduce-kv (fn [_ index val]
                 (if (= obj val)
                   (reduced index)
                   -1))
               -1
               (map #(clojure.lang.MapEntry. %2 %1)
                    this
                    (range))))
  (lastIndexOf [this obj]
    (reduce-kv (fn [lastio index val]
                 (if (= obj val)
                   index
                   lastio))
               -1
               (map #(clojure.lang.MapEntry. %2 %1)
                    this
                    (range))))
  (isEmpty [_]
    (== 0 count))
  (size [_] count)
  (subList [this from to]
    (subvec this from to))
  (toArray [this]
    (into-array Object this))

  Iterable
  (iterator [this] (clojure.lang.SeqIterator. (seq this)))

  IObj
  (withMeta [this metadata]
    (p/relay-withmeta relay metadata))

  IHashEq
  (hasheq [this]
    (if _hasheq
      _hasheq
      (let [he (p/relay-hasheq relay)]
        (set! _hasheq he)
        _hasheq)))

  Object
  (hashCode [this]
    (if _hashcode
      _hashcode
      (let [hc (p/relay-hashcode relay)]
        (set! _hashcode hc)
        _hashcode)))
  (equals [this other]
    (if (instance? clojure.lang.PersistentVector other)
      (if (== count (clojure.core/count other))
        (rds-eq-sequential this other #(clojure.lang.Util/equals %1 %2))
        false)
      (rds-eq-sequential this other #(clojure.lang.Util/equals %1 %2)))))

(defn remote-vector
  [relay count metadata]
  (->RemoteVector relay count metadata nil nil))

(deftype RemoteMap
  [relay count metadata ^:volatile-mutable _hasheq ^:volatile-mutable _hashcode]
  Associative
  (containsKey [this k] (boolean (.entryAt this k)))
  (entryAt [this k] (p/relay-entry relay k))
  (assoc [this k v] (p/relay-assoc relay k v))

  Seqable
  (seq [this] (p/relay-seq relay))

  IPersistentCollection
  (count [this] count)
  (empty [this] {})
  (equiv [this other]
    (rds-eq-map this other #(clojure.lang.Util/equiv %1 %2)))
  (cons [this elem]
    (p/relay-cons relay elem))

  ILookup
  (valAt [this k]
    (when-let [e (.entryAt this k)]
      (val e)))

  Counted

  IPersistentMap
  (without [this k]
    (p/relay-dissoc relay k))

  MapEquivalence

  Iterable
  (iterator [this] (clojure.lang.SeqIterator. (seq this)))

  Map
  (size [this] count)
  (get [this k]
    (let [^Map$Entry e (.entryAt this k)]
      (when e
        (.getValue e))))
  (getOrDefault [this k opt]
    (let [^Map$Entry e (.entryAt this k)]
      (if e
        (.getValue e)
        opt)))
  (isEmpty [_]
    (== 0 count))
  (entrySet [this]
    (set (seq this)))
  (keySet [this]
    (set (keys this)))
  (values [this]
    (vals this))

  IMeta
  (meta [this] metadata)

  IFn
  (invoke [this k] (.valAt this k))
  (invoke [this k not-found]
          (if-let [e (.entryAt this k)]
            (val e)
            not-found))
  (applyTo [this args]
           (condp = (clojure.core/count args)
             1 (.invoke this (nth args 0))
             2 (.invoke this (nth args 0) (nth args 1))
             (throw (ArityException. (clojure.core/count args) "RemoteMap"))))

  IObj
  (withMeta [this metadata]
    (p/relay-withmeta relay metadata))

  IHashEq
  (hasheq [this]
    (if _hasheq
      _hasheq
      (let [he (p/relay-hasheq relay)]
        (set! _hasheq he)
        _hasheq)))
  
  Object
  (hashCode [this]
    (if _hashcode
      _hashcode
      (let [hc (p/relay-hashcode relay)]
        (set! _hashcode hc)
        _hashcode)))
  (equals [this other]
    (rds-eq-map this other #(clojure.lang.Util/equals %1 %2))))

(defn remote-map
  [relay count metadata]
  (->RemoteMap relay count metadata nil nil))

(deftype RemoteSet [relay count metadata ^:volatile-mutable _hasheq ^:volatile-mutable _hashcode]
  clojure.lang.Seqable
  (seq [this] (p/relay-seq relay))

  IPersistentCollection
  (count [this] count)
  (empty [this] #{})
  (equiv [this other]
    (and (set? other)
         (== (clojure.core/count this) (clojure.core/count other))
         (rds-eq-set this other)))
  (cons [this elem]
    (p/relay-cons relay elem))

  Collection
  (size [this] count)

  Counted

  IPersistentSet
  (contains [this k] (boolean (p/relay-entry relay k)))
  (get [this k] (val (p/relay-entry relay k)))
  (disjoin [this k] (p/relay-disj relay k))

  IMeta
  (meta [this] metadata)

  Set
  (containsAll [this objs]
    (sequential-contains-all this objs))
  (isEmpty [_] (== 0 count))
  (toArray [this]
    (into-array Object this))

  Iterable
  (iterator [this] (clojure.lang.SeqIterator. (seq this)))
  
  IFn
  (invoke [this k] (.get this k))
  (applyTo [this args]
           (condp = (clojure.core/count args)
             1 (.invoke this (nth args 0))
             (throw (ArityException. (clojure.core/count args) "RemoteSet"))))

  IObj
  (withMeta [this metadata]
    (p/relay-withmeta relay metadata))

  IHashEq
  (hasheq [this]
    (if _hasheq
      _hasheq
      (let [he (p/relay-hasheq relay)]
        (set! _hasheq he)
        _hasheq)))

  Object
  (hashCode [this]
    (if _hashcode
      _hashcode
      (let [hc (p/relay-hashcode relay)]
        (set! _hashcode hc)
        _hashcode)))
  (equals [this other]
    (and (set? other)
         (== (clojure.core/count this) (clojure.core/count other))
         (rds-eq-set this other))))

(defn remote-set
  [relay count metadata]
  (->RemoteSet relay count metadata nil nil))

(deftype RemoteFn
  [relay]

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
