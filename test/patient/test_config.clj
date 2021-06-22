(ns patient.test_config
  (:require [clojure.test :refer :all]
            [mount.core :refer [start stop defstate]]
            [patient.db.migration :as migration]
            [patient.db.core :as db-core]))

(defn run-db
  []
  (mount.core/start)
  (migration/migrate)
  )

(defn remove-test-records
  []
  (let [test-records (db-core/patients {:surname "TEST" :offset 0 :limit 3000})]
    (map #(db-core/delete-patient! {:id (:id %)}))
    )
  )

(defn prep-db
  [test]
  (run-db)
  (test)
  (remove-test-records)
  )


