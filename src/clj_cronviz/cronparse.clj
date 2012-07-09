(ns clj-cronviz.cronparse
  (:use [clojure.string :only (split)]))

;; Syntax respected:
;; N      Integer
;; N,N,N  Set
;; N-N    Range inclusive
;; *      All
;; */N    Every N

;; Syntax TODO
;; jan-dec
;; sun-sat
;; @yearly @monthly @weekly @daily @hourly

;; Syntax WONTDO
;; ? L W #

;; Syntax CANTDO
;; @reboot

;; Disregarding http://en.wikipedia.org/wiki/Cron on...
;; Use of sixth field as year value
;; Loose match on *either* field when both day of month and day of week are specified.

(def mk-int #(Integer/parseInt %))

(defn is-digits? [s] (re-find #"^\d+$"  s))
(defn has-comma? [s] (re-find #","      s))
(defn has-dash?  [s] (re-find #"-"      s))
(defn is-star?   [s] (re-find #"^\*$"   s))
(defn has-star?  [s] (re-find #"\*/\d+" s))

(defn mk-range
  ([start stop]      (mk-range start stop 1))
  ([start stop step] (range start (inc stop) step)))

(defn expand-star [type interval]
  (case type
    :mi (mk-range 0 59 interval)
    :ho (mk-range 0 23 interval)
    :da (mk-range 1 31 interval)
    :mo (mk-range 1 12 interval)
    :dw (mk-range 0 6  interval)))

(defn expand-cron-notation [type val]
  "Accepts a keyword and a string, returning all consequent values."
  ;; [:mi "*/17"] => (0 17 34 51)
  ;; [:ho "*/3"]  => (0 3 6 9 12 15 18 21)
  ;; [:dw "5,6"]  => (5 6).
  (condp apply [val]
    is-digits? (vector (mk-int val))                            ; 5
    has-comma? (map mk-int (split val #","))                    ; 5,6,7
    has-dash?  (apply mk-range (map mk-int (split val #"-")))   ; 5-7
    is-star?   (expand-star type 1)                             ; *
    has-star?  (expand-star type (mk-int (re-find #"\d+" val))) ; */17
    ))

