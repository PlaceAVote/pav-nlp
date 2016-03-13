(ns pav-nlp.sentiment
  "Sentiment analysis using document analyzer (maxent)."
  (:require [clojure.java.io :as io])
  (:import [opennlp.tools.doccat DoccatModel DocumentCategorizerME DocumentSampleStream]
           [opennlp.tools.util ObjectStream PlainTextByLineStream]
           java.io.BufferedOutputStream))

(defn train
  "Train model on given samples. Returns that trained model."
  [samples-file]
  (let [sample (-> samples-file
                   io/input-stream
                   (PlainTextByLineStream. "UTF-8")
                   DocumentSampleStream.)
        cutoff 2 ;; min. number of times feature has to be seen
        iterations 30]
    (DocumentCategorizerME/train "en" sample cutoff iterations)))

(defn save-model
  "Save trained model to given file."
  [^DoccatModel model ^String path]
  (->> path
       io/output-stream
       BufferedOutputStream.
       (.serialize model)))

(defn load-model
  "Load model from file."
  [^String path]
  (-> path io/input-stream DoccatModel.))

(def ^{:public true
       :doc "Memoised version of load-model."}
  load-model-memo (memoize load-model))

(defn default-model
  "Default model based on Twitter data."
  []
  (-> "input/tweets.db"
      io/resource
      load-model-memo))

(defn- make-score-map
  "Build a map with categories as keys and their score, as values."
  [^DocumentCategorizerME model results]
  (reduce
   (fn [coll item]
     (assoc coll (.getCategory model item) (nth results item)))
   {}
   (-> model .getNumberOfCategories range)))

(defn classify-string
  "Determine sentiment out of given string. Default option will return
\"1\" for what appears positive or \"0\" for negative results.

If you set 'details?' to true, it will return result as map, with detail
scores per each category."
  ([^DoccatModel model ^String str details?]
     (let [categorizer (DocumentCategorizerME. model)
           results     (.categorize categorizer str)
           best        (.getBestCategory categorizer results)]
       (if details?
         (-> categorizer
             (make-score-map results)
             (assoc :result best))
         best)))
  ([^DoccatModel model ^String str] (classify-string model str false)))
