(ns clj-cronviz.core
  (:require [clj-time.core :as ct]
            [clj-cronviz.cronparse :as cron]
            [clj-time.format :as cf]
            [clojure.string :only (split replace) :as str]))


(def weekday-formatter (cf/formatters :weekyear-week-day))

;; day-of-week is a filter on top of the day field, so we filter by
;; it but do not iterate over it.
(defn date-matches-dw? [date dw]
  "Ensure dates generated respect any dw rule that was passed in."
  (let [date-weekday (cron/mk-int (str (last (cf/unparse weekday-formatter date))))]
    (some #{date-weekday} dw)))

(defn date-in-bounds? [x earliest latest]
  ;; Widen the supplied earliest/latest dates by one millisecond in
  ;; both directions since within isn't "within-or-on".
  (let [earliest (ct/minus earliest (ct/millis 1))
        latest   (ct/plus  latest   (ct/millis 1))]
    (ct/within? (ct/interval earliest latest)
                     x)))

(defn apply-filters [dates element-map earliest latest]
  (let [dw (element-map :dw)]
    (->> (filter #(date-in-bounds? % earliest latest) dates)
         (filter #(date-matches-dw? % dw)))))

(defn -generate-dates [ye mo da ho mi]
  (try
    (ct/date-time ye mo da ho mi)
    (catch org.joda.time.IllegalFieldValueException e nil)))

(defn generate-dates [elements earliest latest]
  (for [ye (apply cron/mk-range (map ct/year [earliest latest]))
        mo (elements :mo)
        da (elements :da)
        ho (elements :ho)
        mi (elements :mi)]
    (-generate-dates ye mo da ho mi)))

(defn expand-element [[type val]]
  {type (cron/expand-cron-notation type val)})

(defn repair [elements]
  "Cron syntax decrees Sunday can be 0 or 7. cljtime seems to prefer 7. If we find a 0 for dw, fix."
  (let [dw (str/replace (elements :dw) #"0" "7")]
    (merge elements {:dw dw})))

;; The main event.
(defn line-to-job [[mi ho da mo dw co] earliest latest]
  (let [pairs (repair {:mi mi :ho ho :da da :mo mo :dw dw})
        element-map (into {} (merge (map expand-element pairs)))
        dates (generate-dates element-map earliest latest)
        valid-dates (apply-filters dates element-map earliest latest)]
    {:command co :dates valid-dates}))

(defn nix-whitespace [input]
  (map #(str/split %1 #"\s" 6) (str/split input #"\n")))

(defn drop-non-jobs
  "Does the provided seq containing a string start with a star or digit?"
  [[val]]
  (not (nil? (re-find #"^[*0-9]" val))))

(defn massage-input [input]
  (->> (nix-whitespace input)
       (filter drop-non-jobs)))

(defn mk-date [string]
  (apply ct/date-time (map cron/mk-int (str/split string #"\s"))))

(defn main
  "Accept crontab contents and two start/stop clj-time-parseable date strings.
   Return a seq of maps containing corresponding cron execution times, e.g.
   ({:command 'do_some_stuff', :dates (#<DateTime 2012-07-07T00:17:00.000Z> #<DateTime 2012-07-07T03:17:00.000Z>)})"
  [input earliest latest]
  (let [earliest (mk-date earliest)
        latest   (mk-date latest)]
    (for [line (nix-whitespace input)]
      (apply line-to-job [line earliest latest]))))

