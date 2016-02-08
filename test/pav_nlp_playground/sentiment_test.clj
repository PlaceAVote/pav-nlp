(ns pav-nlp-playground.sentiment-test
  (:require [clojure.test :refer :all]
            [pav-nlp-playground.sentiment :as sen]))

(defn- my-classify [s]
  (sen/classify-string (sen/default-model) s))

(deftest sentiment-tests
  (testing "Positive"
    (is (= 1 (my-classify "Thank you for making this positive")))
    (is (= 1 (my-classify "Nice job!")))
    (is (= 1 (my-classify "I'm happy person"))))
  
  (testing "Negative"
    (is (= 0 (my-classify "He is hating that job")))
    (is (= 0 (my-classify "John has lost a big game!")))
    (is (= 0 (my-classify "I'm sad to see that")))))

