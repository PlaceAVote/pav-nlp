(ns pav-nlp-playground.sentiment
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

(defn classify-string
  "Determine sentiment out of given string. Returns 1 if is positive or
0 if is negative."
  [^DoccatModel model ^String str]
  (let [categorizer (DocumentCategorizerME. model)]
    (if (= "1" (->> str
                    (.categorize categorizer)
                    (.getBestCategory categorizer)))
      1
      0)))
