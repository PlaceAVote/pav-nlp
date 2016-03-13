(ns pav-nlp.sentiment-test
  (:require [clojure.test :refer :all]
            [pav-nlp.sentiment :as sen]))

(defn- my-classify [s details?]
  (sen/classify-string (sen/default-model) s details?))

(deftest sentiment-tests
  (testing "Positive"
    (is (= "1" (my-classify "Thank you for making this positive" false)))
    (is (= "1" (my-classify "Nice job!" false)))
    (is (= "1" (my-classify "I'm happy person" false))))

  (testing "Negative"
    (is (= "0" (my-classify "He is hating that job" false)))
    (is (= "0" (my-classify "John has lost a big game!" false)))
    (is (= "0" (my-classify "I'm sad to see that" false))))

  (testing "Detail results"
    (let [ret (my-classify "Thank you for making this positive" true)]
      (is (= "1" (:result ret)))
      (is (contains? ret "1"))
      (is (contains? ret "0")))))
