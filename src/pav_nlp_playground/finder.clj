(ns pav-nlp-playground.finder
  (:require [opennlp.nlp :as n]
            [clojure.string :as s]
            [clojure.java.io :as io])
  (:import [java.io BufferedReader FileReader]))

(defn- remove-dupes-icase
  "Remove duplicate string values, ignoring case."
  [lst]
  (->> lst
       (group-by s/lower-case)
       (vals)
       (map first)))

(defn- filter-out-junk
  "Remove all junk-alike words."
  [lst]
  (let [junks [#"-{2,}"
               #"[A-Z]{10,}"]]
    (reduce
     (fn [coll item]
       (if (or (some #(re-find % item) junks)
               ;; skip unmatched parentheses
               (not (re-find #"^[^()]*(?:\([^()]*\)[^()]*)*$" item)))
         coll
         (conj coll item)))
     []
     lst)))

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
        name-find
        filter-out-junk
        sort)))

(defn find-orgs
  "Find all organizations in string." 
  [s]
  (let [[tokenize org-find] (build-finders-memo "models/en-ner-organization.bin")]
    (-> s
        tokenize
        org-find
        remove-dupes-icase
        filter-out-junk
        sort)))

(defn confirm-us-org
  "Check organization name against known list of US organizations
fetched from Wikipedia."
  [o]
  (-> (build-org-regex-memo "input/us-orgs.txt")
      (re-find o)))

(defn build-sentence-detector
  "Create sentence detection model."
  []
  (-> "models/en-sent.bin" io/resource n/make-sentence-detector))

(def ^{:public true
       :doc "Cached version of build-sentence-detector."}
  build-sentence-detector-memo (memoize build-sentence-detector))

(defn sentences-by-entities
  "Return all sentences containing given entities. Can be slow on large content.
If given highlighing tags, surround found words with it."
  ([body entities options]
     ;; FIXME: default opennlp sentence detector does not work on questions
     (let [get-sentences (build-sentence-detector-memo)
           sentences (get-sentences body)
           hi-start  (:hi-start options)
           hi-end    (:hi-end options)
           icase?    (:ignore-case? options)]
       (reduce
        (fn [coll ent]
          (let [;; lookup only exact words, not part of larger word, e.g. 'congress'
                ;; but not 'congressional'
                what (format "\\b%s\\b" ent)
                what (if icase?
                       (format "(?i)%s" what)
                       what)
                pattern (re-pattern what)
                found   (filter #(re-find pattern %) sentences)]
            (if (seq found)
              (concat coll
                    (if-not (or (s/blank? hi-start)
                                (s/blank? hi-end))
                      (map #(s/replace ^String % pattern (format "%s%s%s" hi-start ent hi-end)) found)
                      found))
              coll)))
        []
        entities)))
  ([body entities] (sentences-by-entities body entities nil)))
