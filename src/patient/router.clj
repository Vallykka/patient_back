(ns patient.router
  (:require [bidi.bidi :as bidi]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring.middleware.params :refer [params-request]]
            [ring.util.response :refer [response header not-found status]]
            [clojure.edn :as edn]
            [patient.logic :as logic]
            [clojure.tools.logging :as log]
            [clojure.walk :refer [keywordize-keys]]))

(def routes
  ["/" {"list" {:get :patients}
        "patient" {#{"" "/"} {:post :patient}
                   "/edit" {:post :save-patient}
                   "/delete" {:post :delete-patient}}
        "add-patient" {:post :add-patient}
        "addresses" {:post :addresses}
        "preflight" {:options :preflight}
        true :not-found
        }])

(defmulti multi-handler :handler)

(defmethod multi-handler :patients
  [request]
  (let [params (get request :query-params)
        {:keys [offset limit]} params
        resp (logic/get-patient-list (edn/read-string offset) (edn/read-string limit) (dissoc params :limit :offset))]
    (response {:patients resp :count (:full-count (first resp))})))

(defmethod multi-handler :patient
  [request]
  (let [patient-id (get-in request [:params :id])
        res (logic/get-patient-by-id patient-id)]
    (response res))
  )

(defmethod multi-handler :add-patient
  [request]
  (let [params (:params request)
        {:keys [surname name patronymic sex birth-date address oms-policy]} params
        resp (logic/add-patient surname name patronymic sex birth-date address oms-policy)]
  (response {:id resp})))

(defmethod multi-handler :save-patient
  [request]
  (let [params (:params request)
        {:keys [id surname name patronymic sex birth-date address oms-policy]} params
        resp (logic/update-patient id surname name patronymic sex birth-date address oms-policy)]
    (response {:id resp})))

(defmethod multi-handler :delete-patient
  [request]
  (let [patient-id (get-in request [:params :id])
        resp (logic/delete-patient patient-id)]
    (response (str "Successfully deleted! Id - " resp))))

(defmethod multi-handler :addresses
  [request]
  (let [query (get-in request [:params :query])
        res (logic/get-addresses query)]
    (response res)
    ))

(defmethod multi-handler :preflight
  [request]
  (status 200))

(defmethod multi-handler :not-found
  [request]
  (not-found "Not found"))

(defn app-handler
  [handler]
  (fn [request]
      (let [{:keys [uri]} request
            request* (if (= (:request-method request) :options)
                       (bidi/match-route* routes "/preflight" request)
                       (bidi/match-route* routes uri request))]
        (handler request*))))

(defn keywordize-query-params
  [handler]
  (fn [request]
    (handler (keywordize-keys (params-request request)))))

(defn add-cors-header
  [handler]
  (fn [request]
      (let [resp (handler request)]
            (merge
              resp
              {:headers {
                         "Access-Control-Allow-Origin"  "*"
                         "Access-Control-Allow-Headers" "*"
                         "Access-Control-Allow-Methods" "DELETE, POST, GET, OPTIONS"
                         }}
        ))
    ))

(defn exception-wrapper
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/errorf "Exception occured: %s" (ex-message e))
        {:status 500
         :headers {"content-type" "application/json"}
         :body (ex-message e)
         }))))

(def app (-> multi-handler
           app-handler
           keywordize-query-params
           wrap-json-params
           exception-wrapper
           add-cors-header
           wrap-json-response
           ))