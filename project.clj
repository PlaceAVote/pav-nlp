(defproject pav-nlp-playground "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clojure-opennlp "0.3.3"]
                 [instaparse "1.4.1"]
                 [metosin/compojure-api "1.0.0-RC1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ring/ring-jetty-adapter "1.4.0"]]
  :global-vars {*warn-on-reflection* true}
  :uberjar-name "pav-nlp-playground.jar"
  :jvm-opts ["-server"]
  :repl-options {:port 7888}
  :main pav-nlp-playground.core)
