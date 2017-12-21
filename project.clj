(defproject org.clojars.mp406/no-more-f5 "0.1.0"
  :description "Generate a digest of RSS feeds on AWS Lambda."
  :dependencies [[org.clojars.gnzh/feedparser-clj "0.6.1"]]
  :aot  [no-more-f5.core]
  :main no-more-f5.core)
