(ns patient.logic
  (:require [patient.db.core :as db-core]
            [patient.validator :as v]
            [clj-http.client :as http]
            [cheshire.core :refer [parse-string]]
            [clojure.string :as str]
            [clojure.set :as set]
            [clojure.tools.logging :as log])
  (:import (java.sql SQLException)
           (java.text SimpleDateFormat)))

(defn generated-id
  [resp]
  (:id (first resp)))

(defn add-address
  [address]
  (dorun (map #(v/check-request-param (val %) ::v/non-empty-str) (filter #(not= :flat (key %)) address)))
  (try
     (let [{:keys [country city region street house flat]} address
           country-id (generated-id (db-core/insert-country! {:name country}))
           region-id (generated-id (db-core/insert-region! {:name region}))
           city-id (generated-id (db-core/insert-city! {:name city}))
           street-id (generated-id (db-core/insert-street! {:name street}))
           address-id (generated-id (db-core/insert-address! {:country-id country-id
                                                              :region-id  region-id
                                                              :city-id    city-id
                                                              :street-id  street-id
                                                              :house house
                                                              :flat       flat}))]
       (log/info (format "Inserted: %s, res: %s" address address-id))
       address-id)
    (catch SQLException e
      (throw (ex-info (format "Couldn't add address %s. Reason: %s: " address (.getMessage e)) {} e))))
  )

(defn add-patient
  [surname name patronymic sex birth-date address oms-policy]
  (dorun (map #(v/check-request-param % ::v/non-empty-str) [surname name patronymic sex oms-policy]))
  (try
    (db-core/wrap-with-transaction
     (let [birth-date (v/check-request-param birth-date ::v/is-date ::v/->date-pg-obj)
           sex (v/check-request-param sex ::v/is-sex-type ::v/->sex_type-pg-obj)
           address-id (add-address address)
           patient-id (generated-id (db-core/insert-patient! {:surname    surname
                                                              :name       name
                                                              :patronymic patronymic
                                                              :sex        sex
                                                              :birth-date birth-date
                                                              :oms-policy oms-policy}))
           row-count (db-core/insert-patient-address! {:patient-id patient-id :address-id address-id})]
       (log/info (format "Inserted: %s %s" surname name))
       patient-id))
    (catch SQLException e
      (throw (ex-info (format "Couldn't add patient %s %s. Reason: %s: " surname name (.getMessage e)) {} e)))))

(defn update-patient
  [id surname name patronymic sex birth-date address oms-policy]
  (dorun (map #(v/check-request-param % ::v/non-empty-str) [id surname name patronymic sex oms-policy]))
  (try
    (db-core/wrap-with-transaction
      (let [id (v/check-request-param id ::v/->char-pg-uuid)
            birth-date (v/check-request-param birth-date ::v/is-date ::v/->date-pg-obj)
            sex (v/check-request-param sex  ::v/is-sex-type ::v/->sex_type-pg-obj)
            patient-id (generated-id (db-core/update-patient! {:id         id
                                                               :surname    surname
                                                               :name       name
                                                               :patronymic patronymic
                                                               :sex        sex
                                                               :birth-date birth-date
                                                               :oms-policy oms-policy}))
            address-id (if-let [id (:address-id address)]
                         (v/check-request-param id ::v/->char-pg-uuid)
                         (add-address address))
            ]
        (db-core/delete-patient-address-by-patient! {:patient-id patient-id})
        (db-core/insert-patient-address! {:patient-id patient-id
                                          :address-id address-id})
       (log/info (format "Updated: %s %s" surname name))
        patient-id
        ))
    (catch SQLException e
      (throw (ex-info (format "Couldn't update patient %s %s. Reason: %s: " surname name (.getMessage e)) {} e)))))

(defn delete-patient
  [id]
  (v/check-request-param id ::v/non-empty-str)
  (try
    (db-core/wrap-with-transaction
      (let [id (v/check-request-param id ::v/->char-pg-uuid)]
        (db-core/delete-patient-address-by-patient! {:patient-id id})
        (db-core/delete-patient! {:id id})
        ))
       (catch SQLException e
         (throw (ex-info (format "Couldn't delete patient %s. Reason: %s: " id (.getMessage e)) {} e)))))

(defn merge-address-strings
  [patient]
  (merge patient
    {:address
     (str/join ", " (vals (select-keys patient [:country :region :city :street :house :flat])))}))

(defn replace-underscores
  [patient]
  (let [keys-to-replace (map (fn [entry]
                               {(key entry) (keyword (str/replace (str/replace (key entry) "_" "-") ":" ""))})
                          patient)]
    (set/rename-keys patient (apply merge keys-to-replace)))
  )

(defn format-date
  [patient]
  (when-let [date (:birth-date patient)]
    (assoc-in patient [:birth-date] (.format (SimpleDateFormat. "dd-MM-yyyy") date))))

(defn get-patient-by-id
  [id]
  (try
    (let [param (-> id
                    (v/check-request-param ::v/non-empty-str ::v/->char-pg-uuid))]
      (when-let [response (db-core/patient-by-id {:id param})]
       ((comp format-date merge-address-strings replace-underscores) response)))
    (catch SQLException e
        (throw (ex-info (format "Couldn't find patient %s. Reason: %s: " id (.getMessage e)) {} e)))))

(defn get-patient-list
  [offset limit params]
  (let [offset (if (some? offset) offset 0)
        limit (if (some? limit) limit 10)
        of-lim {:limit limit :offset offset}
        valid-params (if #(not (nil? params)) (merge {} params of-lim) of-lim)]
    (try (let [res (db-core/patients valid-params)]
           (map (comp format-date merge-address-strings replace-underscores) res))
         (catch SQLException e
           (throw (ex-info (format "Couldn't find patient %s. Reason: %s: " name (.getMessage e)) {} e))))))

(defn get-addresses
  [query]
  (v/check-request-param query ::v/non-empty-str)
  (let [resp (http/post "https://suggestions.dadata.ru/suggestions/api/4_1/rs/suggest/address"
     {:form-params {:query query}
      :content-type :json
      :headers {"Content-Type"  "application/json"
                "Accept"        "application/json"
                "Authorization" "Token 3f4cbf77d55f1c9ebbeede5a8cdc80e528524e6c"}})]
    (parse-string (:body resp) true)
    ))
