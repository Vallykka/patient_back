(ns patient.validator
  (:require
    [clojure.spec.alpha :as s]
    [clj-postgresql.core :as pg]
    [clojure.string :as str]
    [cheshire.core :as json])
  (:import (java.sql SQLException)
           (java.text SimpleDateFormat ParseException))
  )

(s/def ::non-empty-str (every-pred string? not-empty))
(s/def ::is-sex-type #(contains? #{"MALE" "FEMALE"} %))
(s/def ::is-date (fn [date]
                   (try
                    (not (nil? (.parse (SimpleDateFormat. "yyyy-MM-dd") date)))
                    (catch ParseException e false)                    )
                   ))

(s/def ::->date-pg-obj
  (s/conformer (fn [date]
                 (try (pg/object "date" date)
                      (catch SQLException e ::s/invalid)))))

(s/def ::->sex_type-pg-obj
  (s/conformer (fn [sex-type]
                 (try (pg/object "sex_type" (str/upper-case sex-type))
                      (catch SQLException e ::s/invalid)))))

(s/def ::->char-pg-uuid
  (s/and (every-pred not-empty)
    (s/conformer (fn [id]
                   (try (pg/object "uuid" id)
                        (catch SQLException e ::s/invalid))))))

(s/def ::->pg-obj-to-value
  (s/conformer (fn [coll]
                 (try
                   (let [[field obj] (first coll)]
                     {field (.getValue obj)})
                   (catch SQLException e ::s/invalid)))))

(defn check-request-param
  "Throws exception on first failed validation"
  [param & specs]
  (reduce
    #(if-let [problem (s/explain-data %2 %1)]
       (throw
         (ex-info (str "Bad request: Invalid parameter " (:clojure.spec.alpha/value problem)) problem))
       (s/conform %2 %1)
       ) param specs))

(defn ->pg-json [coll]
  (when-some [col (first coll)]
    (when-some [[field obj] col]
      {field (json/decode (.getValue obj))})))
