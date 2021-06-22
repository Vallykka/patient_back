(ns patient.core
  (:require [mount.core :refer [start stop defstate]]
            [patient.config :refer [env]]
            [patient.db.core :refer [db]]
            [patient.db.migration :as migration]
            [patient.router :as router]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defstate ^{:on-reload :noop} webserver
          :start (let [{jetty-config :server} env]
                   (run-jetty router/app jetty-config))
          :stop (.stop webserver))

(defn run []
  (start [#'env #'db #'webserver])
  (migration/migrate))

(defn shutdown []
  (stop [#'env #'db #'webserver]))

(defn restart []
  (shutdown)
  (start [#'env #'db #'webserver]))

(defn -main
  [& args]
  (run))
