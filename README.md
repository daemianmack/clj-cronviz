This is a Clojure port of [cronviz](https://github.com/federatedmedia/cronviz), a Ruby tool I wrote for [Federated Media](http://www.federatedmedia.net/). The Ruby version works great but I wasn't happy with its speed -- 20 seconds to run 30 rSpec tests -- and suspected Java's speedy date-handling and Clojure's laziness would help. 

Also, I kinda just wanted to.

This port splits the original version into multiple concerns: a pure library suitable for composition, and, elsewhere, a companion web interface for visualization of the output.

The original achieved its goals using separate modules but layered some visualization routines on top. This pattern served its goal but I'd like it to be a bit more generalized: to wit...

- A library to parse crontab syntax
- A library using the cron-parsing logic to transmute arbitrary crontab jobs into a collection of valid dates

And alongside this, a second codebase: a webservice using the aforementioned to provide visualization of crontabs, ala...

![](https://github.com/federatedmedia/cronviz/raw/master/assets/screenshot.png)

***

# cronviz

It's 3 AM. Do you know where your cron jobs are?

## Use case

You have a problem: something's causing performance issues on the application server between 1 and 4 AM, and the cron jobs seem a likely culprit.

Naturally, you eyeball your crontab to find out what's running during those hours.

Now you have two problems.

Over time, cron jobs accrete into an impenetrable, opaque mass of text. Trying to get a comprehensive sense of all the various run times, and finding patterns therein, can be exceedingly difficult. Crontabs are written for computers to interpret -- not humans.

cronviz can help, by parsing stuff like this...

````
({:command "do_some_stuff", :dates (#<DateTime 2012-07-07T00:17:00.000Z> #<DateTime 2012-07-07T03:17:00.000Z>)})
````

out of stuff like this...

````
* * * * * /usr/bin/foo
*/10 * * * * /usr/bin/bar
*/15 * * * * /usr/bin/baz
*/30 * * * * /usr/bin/qux
8 */8 * * * /usr/bin/quux
* * * * * /usr/bin/corge
*/30 23,0,1 * * * /usr/bin/grault
*/5 * * * * /usr/bin/garply
0 * * * * /usr/bin/waldo
0 0 4,22 * * /usr/bin/fred
0 1 * * * /usr/bin/plugh
0 13 * * * /usr/bin/xyzzy
0 2 * * * /usr/bin/thud
30 6 * * 1,2,3,4,5 /usr/bin/wibble
30 7 * * * /usr/bin/wobble
30 8 * * * /usr/bin/wubble
33 */2 * * * /usr/bin/flob
35 1 * * * /usr/bin/whatever
45 * * * * /usr/bin/whoever
45 1 * * * /usr/bin/whomever
* * * * * /usr/bin/whenever
````

## Usage

You'll need to pass the contents of a crontab file and two date strings parseable by [cljtime/date-time](https://github.com/KirinDave/clj-time) to act as book-ends for the period of time you're interested in generating dates across. cronviz will return all matching datetimes from the crontab contents...

````
(def earliest "2011 12 08 0 0") 
(def latest   "2011 12 09 23 59")
(def crontab  "0 17 * * 4,5 launch_happy_hour")
(clj-cronviz/main crontab earliest latest)

=> ({:command "launch_happy_hour", :dates (#<DateTime 2011-12-08T17:00:00.000Z> #<DateTime 2011-12-09T17:00:00.000Z>)})
````

## Shortcomings

- Unfortunately there's no simple way to know what time a job finished short of 1) altering the crontab command or the job it fires, and 2) getting that information into cronviz. Minus that, cronviz can only tell you what time a job has *started*.

## Testing

````
lein expectations
````
