(defproject no-more-f5 "0.1.0"
  :description "Generate a digest of RSS feeds on AWS Lambda."
  :dependencies [
    [org.clojure/clojure "1.9.0"]
    [clj-time "0.14.2"]
    [org.clojars.gnzh/feedparser-clj "0.6.1"]
    [com.amazonaws/aws-lambda-java-core "1.2.0"]
    [com.sun.mail/javax.mail "1.6.0"]
    [environ "1.1.0"]
    ]
  :plugins [[lein-environ "1.1.0"]]
  :aot :all
  )
