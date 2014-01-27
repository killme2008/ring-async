(ns ring.util.async
  (:require [clojure.core.async :refer [go <! map<]]
            [clojure.core.async.impl.protocols :refer [Channel]]
            [clojure.java.io :as io]
            [ring.util.servlet :as servlet]
            [cheshire.core :as json])
  (:import (javax.servlet.http HttpServletRequest HttpServletResponse)
           (java.io PrintWriter InputStream File FileInputStream)))

(defn- set-body
  "Update a HttpServletResponse body with a String, ISeq, File or InputStream.
  It's copied from ring-servlet."
  [^HttpServletResponse response, body]
  (cond
   (string? body)
   (with-open [writer (.getWriter response)]
     (.print writer body))
   (seq? body)
   (with-open [writer (.getWriter response)]
     (doseq [chunk body]
       (.print writer (str chunk))
       (.flush writer)))
   (instance? InputStream body)
   (with-open [^InputStream b body]
     (io/copy b (.getOutputStream response)))
   (instance? File body)
   (let [^File f body]
     (with-open [stream (FileInputStream. f)]
       (set-body response stream)))
   (nil? body)
   nil
   :else
   (throw (Exception. ^String (format "Unrecognized body: %s" body)))))

(defn handle-async-body [response ^HttpServletRequest servlet-request options]
  (if (satisfies? Channel (:body response))
    (let [chan (:body response)
          timeout (:async-timeout options)
          listener (:async-listener options)
          ^org.eclipse.jetty.server.AsyncContinuation async (.startAsync servlet-request)
          ^HttpServletResponse servlet-response (.getResponse async)]
      (when timeout
        (.setTimeout async timeout))
      (when listener
        (.addContinuationListener async listener))
      (servlet/set-headers servlet-response (:headers response))
      (go
       (when-not (.isComplete async)
         (when-let [data (<! chan)]
           (set-body servlet-response data))
         (.complete async)))
      (dissoc response :body :headers))
    response))

(defn add-sse-headers [request]
  (-> request
      (assoc-in [:headers "Content-Type"] "text/event-stream; charset=utf-8")
      (assoc-in [:headers "Cache-Control"] "no-cache")))

(defn edn-events [chan]
  (add-sse-headers
   {:body (map< (fn [event]
                  (str "data: "
                       (pr-str event)
                       "\n\n"))
                chan)}))

(defn json-events [chan]
  (add-sse-headers
   {:body (map< (fn [event]
                  (str "data: "
                       (json/generate-string event)
                       "\n\n"))
                chan)}))
