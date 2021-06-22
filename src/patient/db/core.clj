(ns patient.db.core
  (:require
    [mount.core :refer [defstate]]
    [patient.config :refer [env]]
    [clj-postgresql.core :as pg]
    [conman.core :as conman]
    [hugsql.core :as h]))

(defstate ^{:dynamic true
            :on-reload :noop} db
  :start (let [db (env :db)]
           (conman/connect! (pg/pool :dbname (db :dbname)
                              :host (db :host)
                              :user (db :user)
                              :password (db :password))))
          :stop (conman/disconnect! db))

(conman/bind-connection db "sql/query.sql")

(h/def-sqlvec-fns "sql/query.sql")

(defmacro wrap-with-transaction
  [func]
  `(conman/with-transaction [db {:isolation     :read-uncommitted
                                    :read-only     false
                                    :rollback-only false}]
     (~@func)
    )
  )
