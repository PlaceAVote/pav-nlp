(ns pav-nlp-playground.core
  (:gen-class)
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [clojure.tools.logging :as log]
            [ring.adapter.jetty :refer [run-jetty]]
            [pav-nlp-playground.finder :as f]
            [pav-nlp-playground.sentiment :as sentiment]))

(defn- read-bill
  "Read bill content using bill id, from predefined location."
  [^String id]
  (let [path (format "https://www.govtrack.us/data/congress/114/bills/hr/%s/text-versions/ih/document.txt" id)]
  (try 
    (log/infof "Reading bill '%s' from '%s'..." id path)
    (slurp path)
    (catch java.io.FileNotFoundException e
      (log/errorf e "Failed to read bill '%s'" id)
      nil))))

(defn- extract-something
  "Extract names/orgs from given id."
  [^String id extractor]
  (if-let [body (read-bill id)]
    (ok (extractor body))
    (not-found)))

(defn- compute-sentiment
  "Calculate sentiment."
  [^String id]
  (if-let [body (read-bill id)]
    (let [ret (sentiment/classify-string (sentiment/default-model) body)]
      (ok 
       (if (= ret 1)
         "positive"
         "negative")))
    (not-found)))


(s/defschema StringList [s/Str])

(def app
  (api
    {:swagger {:ui "/"
               :spec "/swagger.json"
               :data {:info {:version "0.1.0"
                             :title "Simple NLP API"
                             :description "Showcase for some NLP stuff we can use."
                             :termsOfService "http://www.placeavote.com"
                             :contact {:name "sz"
                                       :email "sanel@placeavote.com"
                                       :url "http://www.placeavote.com"}}
                      :tags [{:name "nlp", :description "NLP section"}]}}}
    (context "/find" []
      :tags ["finder"]
      (GET "/people/:bill_id" []
        :return StringList
        :path-params [bill_id :- (describe String "Bill ID (e.g. hr2002)")]
        :summary "Returns all person names recognized inside this bill."
        (extract-something bill_id f/find-names))
      (GET "/orgs/:bill_id" []
        :return StringList
        :path-params [bill_id :- (describe String "Bill ID (e.g. hr2003)")]
        :summary "Returns all organzations recognized inside this bill."
        (extract-something bill_id f/find-orgs))
      (GET "/sentiment/:bill_id" []
        :return String
        :path-params [bill_id :- (describe String "Bill ID (e.g. hr2003)")]
        :summary "Returns bill sentiment. For now, sentiment is generated based on Twitter messages tone as sample. Returns 'positive' or 'negative'."
        (compute-sentiment bill_id))) ))

;; ':join? false' will prevent jetty to run in foreground, which will block REPL.
(comment 
(def server 
  (run-jetty #'app {:port 8080 :join? false}))
)

(defn -main [& args]
  (run-jetty #'app {:port 8080}))
