(ns pav-nlp-playground.finder
  (:require [opennlp.nlp :as n]
            [clojure.string :as s]
            [clojure.java.io :as io])
  (:import [java.io BufferedReader FileReader]))

(defn- build-org-regex
  "Create regex from organization sample, where each entry
is OR-ed. Space normalization is not done on input and that can
cause some false detections."
  [^String path]
  (with-open [fd (-> path io/resource io/file FileReader. BufferedReader.)]
    (let [buf (StringBuilder.)
          _   (loop [line (.readLine fd)]
                (when line
                  ;; skip comments and empty lines
                  (when-not (or (re-find #"^\s*#" line)
                                (s/blank? line))
                    (.append buf line)
                    (.append buf "|"))
                  (recur (.readLine fd))))
          ;; skip last '|'
          buf (.substring buf 0 (- (.length buf) 1))]
      (re-pattern (format "(?i)(%s)" buf)))))

(def ^{:private true
       :doc "Cached version of build-org-regex."}
  build-org-regex-memo (memoize build-org-regex))

(defn- build-finders
  "Build NER loaders. Expects english tokens."
  [ner-models]
  (vector
   (-> "models/en-token.bin" io/resource n/make-tokenizer)
   (-> ner-models io/resource n/make-name-finder)))

(def ^{:private true
       :doc "Cached version of NER loaders."}
  build-finders-memo (memoize build-finders))

(defn find-names
  "Find all english names in string." 
  [s]
  (let [[tokenize name-find] (build-finders-memo "models/en-ner-person.bin")]
    (-> s
        tokenize
        name-find)))

(defn find-orgs
  "Find all organizations in string." 
  [s]
  (let [[tokenize org-find] (build-finders-memo "models/en-ner-organization.bin")]
    (-> s
        tokenize
        org-find)))

(defn confirm-us-org
  "Check organization name against known list of US organizations
fetched from Wikipedia."
  [o]
  (-> (build-org-regex-memo "input/us-orgs.txt")
      (re-find o)))
