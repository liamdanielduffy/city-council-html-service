(ns city-council-html-service.core
  (:require [etaoin.api :as e]
            [etaoin.keys :as k]))

(def driver (e/chrome))

(def xpath {
             :page-buttons-container "//*[@id=\"ctl00_ContentPlaceHolder1_gridCalendar_ctl00\"]/thead/tr[1]/td/table/tbody/tr/td/div[1]"
             :page-state-text "/html/body/form/div[3]/div[6]/div/div/div[5]/div[1]/div/div/div/div/table/thead/tr[1]/td/table/tbody/tr/td/div[2]"
             })

(def url {
           :city-council-calendar "https://legistar.council.nyc.gov/Calendar.aspx"
           })

(def regex {
            :page-state-text #"Page 1 of (\d+)"
            })

(defn visit-page [url]
  (e/go driver url))

(defn query [xpath]
  (e/query driver xpath))

(defn visit-calendar []
  (visit-page (:city-council-calendar url)))

(defn get-children-of [element query]
  (e/children driver element query))

(defn get-el-id [xpath-key]
  (query (xpath-key xpath)))

(defn get-page-buttons []
  (let
    [page-buttons-container (get-el-id :page-buttons-container)
     page-buttons (get-children-of
                          page-buttons-container
                          {:tag :a})]
    page-buttons))

(defn get-text-for-element [el-id]
  (e/get-element-text-el driver el-id))

(defn get-page-button-labels []
  (map get-text-for-element (get-page-buttons)))

(defn get-total-pages []
  (let
    [el-id (get-el-id :page-state-text)
     text (get-text-for-element el-id)
     matched-text (re-find (:page-state-text regex) text)
     total-num-pages-str (nth matched-text 1)
     total-num-pages (Integer/parseInt total-num-pages-str)]
    total-num-pages))

(defn page-exists [page-num]
  (<= page-num (get-total-pages)))

;; check if page exists
;; if so, check if page is on screen
;; if so, return page element
;; if not, return nil (stub)
;; eventually if not, click "...", wait for page num to be visible, return page element