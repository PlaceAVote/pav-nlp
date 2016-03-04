(ns pav-nlp.summarizer
  "Simpla, naive summarizer based on 
https://gist.github.com/shlomibabluki/5473521 code."
  (:require [clojure.string :as s]
            [clojure.set :as st]
            [pav-nlp.finder :as f]
            [opennlp.nlp :as n]))

(defn- sentences-intersection
  "Calculate intersection between two sentences."
  [sent1 sent2]
  (let [s1 (-> sent1 (s/split #"\s+") set)
        s2 (-> sent2 (s/split #"\s+") set)
        int (st/intersection s1 s2)]
    (if (seq int)
      (double
       (/
        (count int)
        (/ (+ (count s1) (count s2)) 2)))
      0)))

(defn- split-to-sentences
  "Split body by sentences."
  [body]
  (let [get-sentences (f/build-sentence-detector-memo)]
    (get-sentences body)))

(defn- split-to-paragraphs
  "Split body to paragraphs."
  [body]
  (s/split body #"\n\n"))

(defn- format-sentence
  "Remove all non-alphbetic characters. Used as key in sentence dictionary."
  [content]
  (s/replace content #"\W+" "")) 

(defn- build-rank-map
  "Build rank map out of given sentences and their intersections."
  [sentences values]
  (let [dict  (atom {})
        score (atom 0)
        n     (count sentences)]
    (dotimes [i n]
      (reset! score 0)
      (dotimes [j n]
        (when-not (= i j)
          (reset! score (+ @score (aget values i j)))))
      (swap! dict assoc (format-sentence (nth sentences i)) @score))
    @dict))

(defn- get-ranks
  "Calculate sentences ranks."
  [content]
  (let [sentences (split-to-sentences content)
        len       (count sentences)
        ;; Build Java 2d array. Not as clojure gurus would like, but works.
        values (make-array Double/TYPE len len)]
    (dotimes [i len]
      (dotimes [j len]
        (let [intersection (sentences-intersection (nth sentences i)
                                                   (nth sentences j))]
          (printf "%s = [%s] | [%s]\n\n" intersection (nth sentences i) (nth sentences j))
          (aset values i j intersection))))
    ;(clojure.pprint/pprint (vec values))
    (build-rank-map sentences values)))

(defn- get-best-sentence [paragraph sentence-map]
  (let [sentences (split-to-sentences paragraph)]
    ;(println "---------> " sentences)
    (if (< (count sentences) 2)
      ""
      (let [best-sentence (atom "")
            max-value     (atom 0)]
        (doseq [s sentences]
          (when-let [k (format-sentence s)]
            (when (> (get sentence-map k 0) @max-value)
              (reset! max-value (get sentence-map k))
              (reset! best-sentence s))))
        @best-sentence))))
      
(defn get-summary
  "Build summary."
  ([content sentences-map]
     (let [paragraphs (split-to-paragraphs content)]
       (s/join "\n"
               (reduce (fn [coll para]
                         (let [sentence (s/trim (get-best-sentence para sentences-map))]
                           (if (seq sentence)
                             (conj coll sentence)
                             coll)))
                       []
                       paragraphs))))
  ([content] (get-summary content (get-ranks content))))

(comment

(def sample "Lior Degani, the Co-Founder and head of Marketing of Swayy, pinged me last week when I was in California to tell me about his startup and give me beta access. I hear d his pitch and was skeptical. I was also tired, cranky and missing my kids \u2013 so my frame of mind wasn\u2019t the most positive.

    I went into Swayy to check it out, and when it asked for access to my Twitter and permission to tweet from my account, all I could think was, \u201CIf this thing spams my Twitter account I am going to bitch-slap him all over the Internet.\u201D Fortunately that thought stayed in my head, and not out of my mouth.")

(def o (get-summary sample (get-ranks sample)))

;(clojure.pprint/pprint (get-ranks sample))
)
