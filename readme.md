# replicant-client

A Clojure a library providing client-side implementations of Clojure datastructures served by [replicant-server](https://github.com/clojure/data.alpha.replicant-server).

## Rationale

While the ability to connect to Clojure REPLs over a wire is a powerful lever for programmers, the transport of data-structures can bog down an active connection. Replicant works to avoid sending data over the wire until it's requested. "Large" data structures (via length or depth) are "remotified" - stored in a cache on the server, passed as a remote reference. When more data is needed, replicant-client interacts with replicant-server to retrieve more data.

## Docs

* [API](https://clojure.github.io/replicant-client)

# Release Information

Latest release:

[deps.edn](https://clojure.org/reference/deps_and_cli) dependency information:

As a git dep:

```clojure
io.github.clojure/data.alpha.replicant-client {:git/tag "v0.1.0" :git/sha "0a7b34b"}
``` 

# Developer Information

[![Tests](https://github.com/clojure/data.alpha.replicant-client/actions/workflows/ci.yml/badge.svg)](https://github.com/clojure/data.alpha.replicant-client/actions/workflows/ci.yml)

* [GitHub project](https://github.com/clojure/data.alpha.replicant-client)
* [How to contribute](https://clojure.org/community/contributing)
* [Bug Tracker](https://clojure.atlassian.net/browse/RCLIENT)

# Copyright and License

Copyright © 2023 Rich Hickey and contributors

All rights reserved. The use and
distribution terms for this software are covered by the
[Eclipse Public License 1.0] which can be found in the file
epl-v10.html at the root of this distribution. By using this software
in any fashion, you are agreeing to be bound by the terms of this
license. You must not remove this notice, or any other, from this
software.

[Eclipse Public License 1.0]: http://opensource.org/licenses/eclipse-1.0.php
