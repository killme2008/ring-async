(ns ring.async.utils-test
  (:use clojure.test)
  (:require [clojure.core.async :refer [go >! <! timeout chan close! <!!]]
            [ring.util.async :refer [edn-events json-events]]
            [clojure.string :refer [split]]))

(defn slurp-events [events]
  (let [body (:body events)]
    (split (<!! (clojure.core.async/reduce str "" body))
           #"\n\n")))

(deftest test-edn-events
  (let [body (chan)
        events (edn-events body)]
    (go
     (>! body {:foo 0})
     (close! body))
    (is (= ["data: {:foo 0}"]
           (slurp-events events)))))

(deftest test-json-events
  (let [body (chan)
        events (json-events body)]
    (go
     (>! body {:foo 0})
     (close! body))
    (is (= ["data: {\"foo\":0}"]
           (slurp-events events)))))
