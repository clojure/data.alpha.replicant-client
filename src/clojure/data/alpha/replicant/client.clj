;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

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
