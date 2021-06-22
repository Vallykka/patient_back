(ns patient.core-test
  (:require [clojure.test :refer :all]
            [patient.logic :refer :all]
            [patient.test_config :refer :all]
            [cheshire.core :as json])
  (:import (clojure.lang ExceptionInfo)
           (java.text SimpleDateFormat)))

(use-fixtures :once prep-db)

(def patient-to-add {:surname "TEST"
                     :name "Test"
                     :patronymic "Testovich"
                     :sex "MALE"
                     :birth-date "2010-10-11"
                     :oms-policy "blabla"
                     :address {:value "Concatenated"
                               :country "Russia"
                               :region "Novosibirskaya obl"
                               :city "Novosibirsk"
                               :street   "Kirova"
                               :house "78"
                               :flat "25"}})
(def patient-to-upd {:surname "TEST"
                     :name "Update"
                     :patronymic "Updatovich"
                     :sex "MALE"
                     :birth-date "2021-06-02"
                     :oms-policy "blabla3"
                     :address {:value "Concatenated"
                               :country "Russia"
                               :region "Novosibirskaya obl"
                               :city "Novosibirsk"
                               :street   "Kirova"
                               :house "99"
                               :flat "25"}})

(deftest add-patient-test
  (testing "add-patient"
    (let [{:keys [surname name patronymic sex birth-date oms-policy address]} patient-to-add
          id (add-patient surname name patronymic sex birth-date address oms-policy)]
      (is (not (nil? id)) "Added patient")
      (is (thrown? ExceptionInfo (add-patient surname nil patronymic sex birth-date address oms-policy)) "nil params")
      (is (thrown? ExceptionInfo (add-patient surname "" patronymic sex birth-date address oms-policy)) "blank params")
      (is (thrown? ExceptionInfo (add-patient surname name patronymic "azaza" birth-date address oms-policy)) "invalid sex params")
      (is (thrown? ExceptionInfo (add-patient surname name patronymic sex "birth-date" address oms-policy)) "invalid birth date params")
      ))
  )

(deftest upd-patient-test
  (dorun (map
     (fn [[k v]]
       (testing (str "update-patient-" k)
         (let [{:keys [surname name patronymic sex birth-date address oms-policy]} patient-to-upd
               id (str (add-patient surname name patronymic sex birth-date address oms-policy))
               new-values (assoc patient-to-upd k (str "TEST-Updated-" k))
               {:keys [surname name patronymic sex birth-date address oms-policy]} new-values
               upd-id (update-patient id surname name patronymic sex birth-date address oms-policy)
               updated (get-patient-by-id (str upd-id))]
           (is (not (nil? upd-id)) "Updated patient")
           (is (not (nil? updated)) "Got updated patient")
           (is (= (get new-values k) (get updated k)) (format "Updated patient's $" k))))
       )
     (filter #(not (contains? #{:sex :birth-date :oms-policy :address} (key %))) patient-to-upd)
     ))
  (dorun (map
           (fn [[k v]]
             (testing (str "update-patient-address" k)
               (let [{:keys [surname name patronymic sex birth-date address oms-policy]} patient-to-upd
                     id (str (add-patient surname name patronymic sex birth-date address oms-policy))
                     new-values (assoc-in patient-to-upd [:address k] (str "Updated-" k))
                     {:keys [surname name patronymic sex birth-date address oms-policy]} new-values
                     upd-id (update-patient id surname name patronymic sex birth-date address oms-policy)
                     updated (get-patient-by-id (str upd-id))]
                 (is (not (nil? upd-id)) "Updated patient")
                 (is (not (nil? updated)) "Got updated patient")
                 (is (= (get-in new-values [:address k]) (get updated k)) (format "Updated patient's $" k))))
             )
           (filter #(not (contains? #{:value :address-id} (key %))) (:address patient-to-upd))
           ))
  (testing "update-patient-birth-date"
    (let [{:keys [surname name patronymic sex birth-date address oms-policy]} patient-to-upd
          id (str (add-patient surname name patronymic sex birth-date address oms-policy))
          new-value "2021-02-22"
          upd-id (update-patient id surname name patronymic sex new-value address oms-policy)
          updated (get-patient-by-id (str upd-id))]
      (is (not (nil? upd-id)) "Updated patient")
      (is (not (nil? updated)) "Got updated patient")
      (is (= (.format (SimpleDateFormat. "dd-MM-yyyy") (.parse (SimpleDateFormat. "yyyy-MM-dd") new-value))
            (get updated :birth-date)) (format "Updated patient's birth-date"))
      )
    )
  (testing "update-patient-huge-change-in-life"
    (let [{:keys [surname name patronymic sex birth-date address oms-policy]} patient-to-upd
          id (str (add-patient surname name patronymic sex birth-date address oms-policy))
          new-value "FEMALE"
          upd-id (update-patient id surname name patronymic new-value birth-date address oms-policy)
          updated (get-patient-by-id (str upd-id))]
      (is (not (nil? upd-id)) "Updated patient")
      (is (not (nil? updated)) "Got updated patient")
      (is (= new-value (get updated :sex)) (format "Updated patient's sex"))
      )
    )
  (testing "update-invalid"
    (let [{:keys [surname name patronymic sex birth-date address oms-policy]} patient-to-upd
          id (str (add-patient surname name patronymic sex birth-date address oms-policy))]
    (is (thrown? ExceptionInfo (update-patient id surname nil patronymic sex birth-date address oms-policy)) "nil params")
    (is (thrown? ExceptionInfo (update-patient id surname "" patronymic sex birth-date address oms-policy)) "blank params")
    (is (thrown? ExceptionInfo (update-patient id surname name patronymic "azaza" birth-date address oms-policy)) "invalid sex params")
    (is (thrown? ExceptionInfo (update-patient id surname name patronymic sex "birth-date" address oms-policy)) "invalid birth date params")))
  )

(deftest delete-patient-test
  (testing "add-patient"
    (let [{:keys [surname name patronymic sex birth-date oms-policy address]} patient-to-add
          id (add-patient surname name patronymic sex birth-date address oms-policy)
          resp (delete-patient (str id))
          deleted (get-patient-by-id (str id))]
      (is (not (nil? id)) "Added patient")
      (is (nil? deleted) "Deleted patient")
      ))
  )

