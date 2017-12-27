(ns no-more-f5.core
  (:require [feedparser-clj.core :refer :all :reload true])
  (:require [clj-time.coerce])
  (:require [clj-time.core])
  (:require [clj-time.format])
  (:require [clojure.string :as str])
  (:import (com.amazonaws.services.lambda.runtime Context))
  (:import (java.net URL HttpURLConnection))
  (:gen-class :methods [^:static [handler [Object com.amazonaws.services.lambda.runtime.Context] Object]])
  )


(defn parse-feed-with-user-agent
  "Call parse-feed with a custom User-Agent"
  [feed]
  (parse-feed
    (doto
      (cast HttpURLConnection (.openConnection (URL. feed)))
      (.setRequestProperty "User-Agent" (System/getenv "USER_AGENT"))
      )
    )
  )

(def today
  "Current date as a string"
  (clj-time.format/unparse
    (clj-time.format/formatter "yyyy-MM-dd")
    (clj-time.core/now)
    )
  )

(defn get-timestamp
  "Get an entry timestamp, take either published date or update date.
  Fallback to 'now' if neither is available."
  [entry]
  (clj-time.coerce/from-date (or (:updated-date entry) (:published-date entry) (java.util.Date.)))
  )

(def get-title
  "Get entry title"
  (comp str/trim :title)
  )

(defn get-link
  "Get entry link. Handle GitHub links separately."
  [entry]
  (if (.contains (:uri entry) "tag:github")
    (str "https://github.com" (:link entry))
    (:link entry)
    )
  )

(defn pretty-print-timestamp
  "Print a timestamp in a nice ISO-like format"
  [timestamp]
  (clj-time.format/unparse (clj-time.format/formatter "yyyy-MM-dd, HH:mm") timestamp)
  )

(defn pretty-print-entry
  "Pretty-print entry as HTML"
  [entry]
  (format "<p><a href=\"%s\"><b>%s:</b> %s</a></p>\r\n"
    (get-link entry)
    (pretty-print-timestamp (get-timestamp entry))
    (get-title entry)
    )
  )

(defn within-last-24h?
  "Check if a timestamp is within the last 24 hours"
  [timestamp]
  (clj-time.core/after? timestamp (clj-time.core/yesterday))
  )

(def fresh-entry?
  "Check if an entry is within last 24 hours"
  (comp within-last-24h? get-timestamp)
  )

(defn fresh-feed?
  "Check if a feed contains at least one fresh entry"
  [feed]
  (not (empty? (filter fresh-entry? (:entries feed))))
  )

(defn process-feed
  "Process feed into a HTML snippet"
  [feed]
  (reduce str (str "\r\n<h2>" (:title feed) "</h2>\r\n")
    (map pretty-print-entry (filter fresh-entry? (:entries feed)))
    )
  )

(defn process-feeds
  "Process all feeds into a HTML document"
  [feeds]
  (str/join "\r\n\r\n"
    (map process-feed
      (filter fresh-feed? (map parse-feed-with-user-agent feeds))
      )
    )
  )

(defn send-mail
  "Adjusted from https://nakkaya.com/2009/11/10/using-java-mail-api-from-clojure/"
  [& m]
  (let [
    mail (apply hash-map m)
    props (java.util.Properties.)
    ]

    (doto props
      (.put "mail.smtp.host" (:host mail))
      (.put "mail.smtp.port" (:port mail))
      (.put "mail.smtp.user" (:user mail))
      (.put "mail.smtp.socketFactory.port" (:port mail))
      (.put "mail.smtp.auth" "true")
      (.put "mail.smtp.starttls.enable" "true")
      (.put "mail.smtp.socketFactory.class"
            "javax.net.ssl.SSLSocketFactory")
      (.put "mail.smtp.socketFactory.fallback" "true")
      )

    (let [
      authenticator (proxy
        [javax.mail.Authenticator]
        []
        (getPasswordAuthentication
          []
          (javax.mail.PasswordAuthentication. (:user mail) (:password mail))
          )
        )
      recipients (:to mail)
      session (javax.mail.Session/getInstance props authenticator)
      msg (javax.mail.internet.MimeMessage. session)
      ]

      (.setFrom msg (javax.mail.internet.InternetAddress. (:from mail)))
      (.setRecipients
        msg
        (javax.mail.Message$RecipientType/TO)
        (javax.mail.internet.InternetAddress/parse recipients)
        )
      (.setSubject msg (:subject mail))
      (.setContent msg (:content mail) "text/html; charset=utf-8")
      (javax.mail.Transport/send msg)
      )
    )
  )

(defn -handler
  [^Object req ^Context ctx]
  (let [body (process-feeds (str/split-lines (slurp (System/getenv "FEEDS"))))]
    (send-mail
      :user (System/getenv "SMTP_USER")
      :password (System/getenv "SMTP_PASS")
      :host (System/getenv "SMTP_SERVER")
      ; FIXME: Is there a safer string to int conversion?
      :port (read-string (System/getenv "SMTP_PORT"))
      :from (System/getenv "EMAIL_FROM")
      :to (System/getenv "EMAIL_TO")
      :subject (str "Daily digest " today)
      :content body
      )
    )
  )
