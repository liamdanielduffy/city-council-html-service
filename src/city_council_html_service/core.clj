(ns city-council-html-service.core
  (:require [etaoin.api :as e]
            [cheshire.core :as json]
            [hiccup2.core :as h]
            [clj-http.client :as client]
            [hickory.core :as hick]
            [hickory.select :as s])
  (:import [java.time LocalDate]
           [java.time.format DateTimeFormatter]
           [java.time ZoneId]
           [java.time ZonedDateTime]
           [java.time LocalTime]))

(defonce driver (e/chrome))

(def queries {:page-buttons-container "//*[@id=\"ctl00_ContentPlaceHolder1_gridCalendar_ctl00\"]/thead/tr[1]/td/table/tbody/tr/td/div[1]"
              :page-state-text "/html/body/form/div[3]/div[6]/div/div/div[5]/div[1]/div/div/div/div/table/thead/tr[1]/td/table/tbody/tr/td/div[2]"
              :more-pages-button {:title "Next Pages"}
              :years-dropdown "//*[@id=\"ctl00_ContentPlaceHolder1_tdYears\"]"
              :all-years-dropdown-item "//*[@id=\"ctl00_ContentPlaceHolder1_lstYears_DropDown\"]/div/ul/li[1]"})

(def url {:city-council-calendar "https://legistar.council.nyc.gov/Calendar.aspx"})

(def regex {:page-state-text #"Page \d+ of (\d+)"})

(defn go-to [url]
  (e/go driver url))

(defn query [q]
  (e/query driver q))

(defn go-to-calendar []
  (go-to (:city-council-calendar url)))

(defn click-el [el]
  (e/click-el driver el))

(defn get-children-of [element query]
  (e/children driver element query))

(defn query-el [q]
  (query (q queries)))

(defn click [query-key]
  (click-el (query-el query-key)))

(defn page-btns []
  (let
   [btns-container (query-el :page-buttons-container)
    btns (get-children-of btns-container {:tag :a})]
    btns))

(defn get-text-for-element [el-id]
  (e/get-element-text-el driver el-id))

(defn page-btn-labels []
  (map get-text-for-element (page-btns)))

(defn num-total-pages []
  (let
   [el-id (query-el :page-state-text)
    text (get-text-for-element el-id)
    matched-text (re-find (:page-state-text regex) text)
    total-num-pages-str (nth matched-text 1)
    total-num-pages (Integer/parseInt total-num-pages-str)]
    total-num-pages))

(defn page-exists [page-num]
  (<= page-num (num-total-pages)))

(defn is-int-string [s]
  (try
    (do (Integer/parseInt s) true)
    (catch NumberFormatException _ false)))

(defn page-btn-numbers []
  (let [btn-labels (page-btn-labels)
        numbered-btn-labels (filter #(is-int-string %1) btn-labels)
        numbers (map #(Integer/parseInt %1) numbered-btn-labels)]
    numbers))

(defn page-btn-is-visible [page-num]
  (let [visible-numbers (page-btn-numbers)
        is-visible (contains? (set visible-numbers) page-num)]
    is-visible))

(defn page-btns-by-label []
  (zipmap (page-btn-labels) (page-btns)))

(defn more-pages-btn []
  (query-el :more-pages-button))

(defn wait-for-page-load []
  (e/wait-visible driver (:page-buttons-container queries) {:timeout 30}))

(defn visit-page [page-num]
  (let [is-visible (page-btn-is-visible page-num)
        exists (page-exists page-num)
        label (str page-num)
        btn ((page-btns-by-label) label)]
    (if is-visible
      (do
        (click-el btn)
        (wait-for-page-load))
      (when exists
        (click :more-pages-button)
        (wait-for-page-load)
        (recur page-num)))))


(defn store-page-html [page-num page-html]
  (let [url "http://localhost:3000/api/store-calendar-page-snapshot"
        body (json/generate-string {"pageNumber" page-num, "html" page-html})
        headers {"Content-Type" "application/json"}]
    (client/post url {:body body :headers headers})))

(defn take-calendar-snapshot []
  (go-to-calendar)
  (wait-for-page-load)
  (loop [num-remaining (num-total-pages)
         page-num 1]
    (when (pos? num-remaining)
      (visit-page page-num)
      (def html (e/get-source driver))
      (store-page-html page-num html)
      (recur
       (dec num-remaining)
       (inc page-num)))))

(defn save-as-json
  [data filename]
  (with-open [w (java.io.BufferedWriter. (java.io.FileWriter. filename))]
    (.write w (json/generate-string data))))

(defn fetch-html [url]
  (try
    (let [response (client/get url)]
      (:body response))
    (catch Exception e
      (str "Failed to fetch HTML from URL: " (.getMessage e)))))


(defn get-page-html [page-num]
  (let [url (str "http://localhost:3000?page=" page-num)]
    (fetch-html url)))

(defn page [page-num]
  (hick/as-hickory (hick/parse (get-page-html page-num))))

(defn remove-strings [v]
  (filter (complement string?) v))

(defn rows-with-class-name [class-name parsed-page]
  (s/select (s/class class-name) parsed-page))

(defn rows [page]
  (let [odd-rows (rows-with-class-name "rgRow" page)
        even-rows (s/select (s/class "rgAltRow") page)
        all-rows (vec (interleave odd-rows even-rows))]
    all-rows))

(defn row [row-num page]
  (let [row-idx (dec row-num)
        row (nth (rows page) row-idx)]
    row))

(defn columns [row]
  (remove-strings (:content row)))

(def column-idx
  {
   :meeting-name  0
   :meeting-date 1
   :ics 2
   :meeting-time 3
   :meeting-location 4
   :meeting-topic 5
   :meeting-details 6
   :meeting-agenda 7
   :meeting-minutes 8
   :meeting-video 9
   })

(defn column [column-key row]
  (let [cols (columns row)
       idx (column-idx column-key)]
    (nth cols idx)))

(defn get-meeting-name [row]
  (let [col (column :meeting-name row)
        col-content (first (remove-strings (:content col)))
        col-text (first (:content col-content))]
    col-text))

(defn get-iso-datetime [date-string time-string]
  (let [date-formatter (DateTimeFormatter/ofPattern "MM/dd/yyyy")
        time-formatter (DateTimeFormatter/ofPattern "h:mm a")
        local-date (LocalDate/parse date-string date-formatter)
        local-time (java.time.LocalTime/parse time-string time-formatter)
        zone-id (ZoneId/of "America/New_York")
        zoned-date-time (ZonedDateTime/of local-date local-time zone-id)]
    (.format zoned-date-time DateTimeFormatter/ISO_ZONED_DATE_TIME)))

(defn get-meeting-date-string [row]
 (let [col (column :meeting-date row)
        col-content (:content col)
        col-text (first col-content)]
    col-text))

(defn get-meeting-time-string [row]
  (let [col (column :meeting-time row)
        col-content (:content (first (remove-strings (:content col))))
        col-text (first col-content)]
      col-text))

(defn get-meeting-datetime [row]
  (let [date-string (get-meeting-date-string row)
        time-string (get-meeting-time-string row)
        iso-datetime (get-iso-datetime date-string time-string)]
    iso-datetime))

(defn get-ics-url [row]
  (let [col (column :ics row)
        link-el (first (s/select (s/child (s/tag :a)) col))
        link-path (:href (:attrs link-el))]
    (str "https://legistar.council.nyc.gov/" link-path)))

(defn get-meeting-location [row]
  (let [col (column :meeting-location row)
       col-content (:content col)
       col-text (first col-content)]
  col-text))