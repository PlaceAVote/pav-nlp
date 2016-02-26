(ns pav-nlp-playground.scraper.representatives
  "Scrape all government agencies from https://www.usa.gov/federal-agencies"
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as s]))

(defn- federal-agency?
  "Check if given url looks like federal agency."
  [^String url]
  (and url
       (.contains url "federal-agencies")))

(defn- url-only
  "Extract only urls from the three of agencies. Resolve url to the full
url path."
  [tree]
  (reduce (fn [coll mp]
            (let [url-part (get-in mp [:attrs :href])]
              (if (federal-agency? url-part)
                (conj coll (str "https://www.usa.gov/" url-part))
                coll)))
          #{}
          tree))

(defn- select-first-content
  "Select first item from content key, given resource tree and path."
  [tree path]
  (some-> tree (html/select path) first :content first))

(defn- scrape-agency-page
  "Scrape specific agency page."
  [url]
  (when-let [res (-> url java.net.URL. html/html-resource)]
    (let [name (select-first-content res [:div.rightnav :header :h1])
          description (select-first-content res [:div.rightnav :header :p])]
      [name description])))

(defn- gen-a-z-urls
  "Agency sub-urls are from a to z. Generate all urls with given main url as root."
  [url]
  (map #(str url "/" (char %))
       (range (int \a)
              (inc (int \z)))))

(-> "https://www.usa.gov/federal-agencies/a"
    java.net.URL.
    html/html-resource
    (html/select [:ul.one_column_bullet :li :a])
    url-only
    clojure.pprint/pprint)
