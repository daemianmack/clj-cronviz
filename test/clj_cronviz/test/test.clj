(ns clj-cronviz.test.test
  (:use [expectations :only (expect given)]
        [clj-cronviz.cronparse :only (expand-cron-notation)]
        [clj-cronviz.core :only (main mk-date repair massage-input)])
  (:require [clj-time.core :as ct]))

(def _job "17 */3 * * * do_some_stuff")
(def _earliest "2012 7 7 0 0")
(def _latest    "2012 7 8 0 0")

(defn get-dates [fn]
  ((first fn) :dates))

;; Test cronparse.
(given [x y] (expect (apply expand-cron-notation x) y)
  [:mi "17"] [17] ; Should expand a minute to a single iterable value.
  [:ho "3"]  [3] ; Should expand an hour to a single iterable value.
  [:mi "16,46"] [16 46] ; Should expand a set of minutes to two values.
  [:ho "3,6,9"] [3 6 9] ; Should expand a set of hours to three values.
  [:mi "1-5"] [1 2 3 4 5] ; Should expand a range of minutes to a range of values.
  [:ho "12-23"] [12 13 14 15 16 17 18 19 20 21 22 23] ; Should expand a range of hours to a range of values.
  [:mi "*"] (range 0 60) ; Should expand all minutes to 60 minutes.
  [:ho "*"] (range 0 24) ; Should expand all hours to 24 hours.
  [:da "*"] (range 1 32) ; Should expand all days to 31 days, 1-indexed.
  [:mo "*"] (range 1 13) ; Should expand all months to 12 months, 1-indexed.
  [:mi "*/13"] [0 13 26 39 52] ; Should expand every X minutes to the proper values.
  [:ho "*/11"] [0, 11, 22] ; Should expand every X hours to the proper values.
  [:ho "*/13"] [0, 13] ; Should expand every 13 hours to 0 and 13.
  [:da "*/5"] [1, 6, 11, 16, 21, 26, 31] ; Should expand every 5 days to be one-indexed.
  [:mo "*/2"] [1, 3, 5, 7, 9, 11]) ; Should expand every 2 months to be one-indexed.


;; Test core.
;; Should generate dates no earlier than _earliest time.
(expect true
        (ct/after?
         (first (get-dates (main _job _earliest _latest)))
         (mk-date _earliest)))

;; Should generate dates no later than _latest time.
(expect true
        (ct/before?
         (last (get-dates (main _job _earliest _latest)))
         (mk-date _latest)))

;; Should generate dates respecting day-of-week field.
(expect 4 (count (get-dates (main "0 17 * * 4,5 launch_happy_hour" "2011 10 6 0 0" "2011 10 17 23 59"))))

;; Should handle single strings properly: results in one job.
(expect 1 (count (main "*/5 5,6,7 11 10 2 do_some_stuff" "2011 10 11 0 0" "2011 10 12 0 0")))

;; Should handle single strings properly: results in X dates.
(expect 36 (count (get-dates (main "*/5 5,6,7 11 10 2 do_some_stuff" "2011 10 11 0 0" "2011 10 12 0 0"))))

;; Should handle multiple strings properly: results in 2 jobs.
(expect 2 (count (main "17-21 */3 11 10 * do_some_stuff\n* * 11 10 * do_other_stuff" _earliest _latest)))

;; Should handle multiple strings properly: first job results in X dates.
(expect 40 (count ((first (main "17-21 */3 11 10 * do_some_stuff\n* * 11 10 * do_other_stuff" "2011 10 11 0 0" "2011 10 12 0 0")) :dates)))

;; Should handle multiple strings properly: second job results in X dates.
(expect 1440 (count ((last (main "17-21 */3 11 10 * do_some_stuff\n* * 11 10 * do_other_stuff" "2011 10 11 0 0" "2011 10 12 0 0")) :dates)))

;; Should repair a weekday of 0 (Sunday) to be a weekday of 7 (Sunday) since cron allows either but clj-time uses 7.
(expect {:mi "1" :ho "*/4" :da "*" :mo "2,4,6" :dw "6,7"}
        (repair {:mi "1" :ho "*/4" :da "*" :mo "2,4,6" :dw "6,0"}))

;; Should drop all non-cron-job-looking lines from input.
(expect 2 (count (massage-input "1 1 * * * one\n#1 1 * * * defunct job\n1 1 * * * two\n\n\n#1 1 * * * dead_job")))
        