(ns patient.db.migration
  (:require [migratus.core :as migratus]
            [clojure.tools.logging :as log]
            [patient.config :refer [env]]))

(defn migrate []
  (let [db-conf (env :db)
        db-url {:db
                (str "jdbc:postgresql://"
                     (db-conf :host)
                     "/"
                     (db-conf :dbname)
                     "?"
                     "user=" (db-conf :user)
                     "&password=" (db-conf :password))}]
    (migratus/migrate (merge (env :migratus) db-url))))
