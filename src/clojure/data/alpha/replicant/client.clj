(ns clojure.data.alpha.replicant.client
  (:require [clojure.data.alpha.replicant.client.protocols :as p]))

(defn clone
  "Given a Replicant client, a remote data structure, and a limit returns a clone
  of the collection stored on the server as long as it's length and depth fall below
  the limit. If no limit is provided then the value 100000 is used by default."
  ([client obj]
   (clone client obj 100000))
  ([client obj limit]
   (p/remote-fetch client obj {:rds/length [limit], :rds/level limit})))
