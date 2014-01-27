# ring-async

Ring adapter for supporting asynchronous responses.

## Installation

To use ring-async, add the following to your `:dependencies`:

    [com.ninjudd/ring-async "0.2.0"]

## Usage

To return an asynchronous response that doesn't consume a thread for the duration of the request,
just use `ring.adapter.jetty-async/run-jetty-async` instead of `ring.adapter.jetty/run-jetty` and
return a response map where the `:body` is a core.async channel. Then, simply use a `go` block to
add data to the body asynchronously.

```clj
(ns ring-async-sample
  (:require [clojure.core.async :refer [go >! chan close!]]
            [ring.adapter.jetty-async :refer [run-jetty-async]]))

(defn handler [request]
  (let [body (chan)]
    (go (loop [...]
          (if ...
            (>! body data)
            (recur ...)))
        (close! body))
    {:body body}))

(defn start []
  (run-jetty-async handler {:join? false :port 8000}))
```

### AsyncContext options

You can configure the AsyncContext's timeout and AsyncListener:

```clj
(def listener
	(reify AsyncListener
		(onComplete [this e])
		(onTimeout [this e])
		(onError [this e])
		(onStartAsync [this e])))
(defn start []
  (run-jetty-async handler {:join? false :port 8000 :async-timeout 5000 :async-listener listener}))
```

## License

Copyright © 2013 Justin Balthrop

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
