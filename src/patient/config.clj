(ns patient.config
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [mount.core :refer [args defstate]]))

(defstate env
          :start (let [config (load-config :merge [(source/from-resource)])]
                   (if-some [db-host (System/getenv "DB-HOST")]
                     (assoc-in config [:db :host] db-host)
                     config)))

