# clojure.data.alpha.replicant.client 





## `clone`
``` clojure

(clone client obj)
(clone client obj limit)
```


Given a Replicant client, a remote data structure, and a limit returns a clone
  of the collection stored on the server as long as it's length and depth fall below
  the limit. If no limit is provided then the value 100000 is used by default.
<br><sub>[source](src/clojure/data/alpha/replicant/client.clj#L12-L19)</sub>
