(ns pav-nlp.scraper.representatives
  "Scrape representatives from http://www.house.gov/representatives/"
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as s]))

(defn- fix-name
  "Take input as 'Last Name, First Name' and produce map of
'{:first First Name :last Last Name}'."
  [name]
  (let [[nlast nfirst] (-> name s/trim (s/split #"\s*,\s*"))]
    {:first nfirst :last nlast}))

(defn- get-person-details
  "Extract url and person name. Convert person name to first/last names."
  [mp]
  (let [content (map :content (remove (complement map?) 
                                      (:content mp)))]
    ;; if name is not present, we are in different tree, so skip everything
    (when-let [name (some-> content second first :content first s/trim)]
      (let [url   (some-> content (nth 1) first :attrs :href)
            party (-> content (nth 2) first)
            room  (-> content (nth 3) first)
            phone (-> content (nth 4) first)
            assignment (->> content last (remove map?))]
        {:name (fix-name name)
         :url url
         :phone phone
         :party party
         :room room
         :assignment assignment}))))

(defn scrape
  "Scrape representatives page and return map of all
available representatives."
  []
  (let [tree (-> "http://www.house.gov/representatives"
                 java.net.URL.
                 html/html-resource
                 (html/select [:table :tr])
                 ;; skip first one, since it is only a header
                 rest)]
    ;; use 'reduce' to filter out nil elements, without re-iterating
    ;; them with 'map/remove'
    (reduce (fn [coll item]
              (if item
                (conj coll item)
                item))
            []
            tree)))
