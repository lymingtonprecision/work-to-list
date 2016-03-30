(defproject work-to-list "1.0.0-SNAPSHOT"
  :description "LPE IFS DBR Work To List display"
  :url "https://github.com/lymingtonprecision/work-to-list"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.5.3"

  :dependencies [;; clojure(script)
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [org.clojure/core.async "0.2.374"
                  :exclusions [org.clojure/tools.reader]]

                 ;; system
                 [com.stuartsierra/component "0.3.1"]
                 [com.netflix.hystrix/hystrix-clj "1.5.1"]
                 [environ "1.0.2"]
                 [clj-time "0.11.0"]
                 [pandect "0.5.4"]

                 ;;;; logging
                 ;; use logback as the main Java logging implementation,
                 ;; immutant/undertow/wunderboss uses it and it's the least
                 ;; amenable to change/exclusion
                 [ch.qos.logback/logback-classic "1.1.6"]
                 [ch.qos.logback/logback-core "1.1.6"]
                 ;; redirect everything else to SLF4J
                 [org.slf4j/slf4j-api "1.7.19"]
                 [org.slf4j/jcl-over-slf4j "1.7.19"]
                 [org.slf4j/log4j-over-slf4j "1.7.19"]
                 [org.apache.logging.log4j/log4j-to-slf4j "2.5"]
                 ;; use timbre for our own logging
                 [com.taoensso/timbre "4.3.1"]

                 ;; database
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.clojars.zentrope/ojdbc "11.2.0.3.0"]
                 [hikari-cp "1.6.1"]
                 [yesql "0.5.2"]

                 ;; data definition/manipulation
                 [prismatic/schema "1.0.5"]
                 [meta-merge "0.1.2-SNAPSHOT"]

                 ;; http
                 [org.immutant/web "2.1.3"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-defaults "0.2.0"]
                 [bidi "2.0.4"]

                 ;; client/server communication
                 [com.taoensso/sente "1.8.1"]

                 ;; output formats
                 [cheshire "5.5.0"]
                 [hiccup "1.0.5"]

                 ;; client side
                 [cljsjs/react-with-addons "0.14.7-0"]
                 [re-frame "0.7.0" :exclusions [cljsjs/react]]
                 [kibu/pushy "0.3.6"]]

  :plugins [[lein-figwheel "0.5.0-6" :exclusions [[org.clojure/clojure]]]
            [lein-cljsbuild "1.1.3"  :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src"]

  :main ^:skip-aot work-to-list.main

  :clean-targets ^{:protect false} ["resources/public/assets/js" "target"]

  :profiles
  {:dev {:dependencies [[org.clojure/test.check "0.9.0"]
                        [com.cemerick/piggieback "0.2.1"]
                        [figwheel-sidecar "0.5.0-6"]
                        [reloaded.repl "0.2.1"]]
         :source-paths ["dev"]}
   :uberjar {:prep-tasks ["compile" ["cljsbuild" "once" "min"]]
             :aot [work-to-list.main]
             :uberjar-name "work-to-list-standalone.jar"}}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]
                 :init-ns user
                 :init (reloaded.repl/init)}

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src" "dev"]
                :figwheel {:on-jsload "work-to-list.client.env/on-js-reload"}
                :compiler {:main work-to-list.client.core
                           :asset-path "/assets/js/deps"
                           :output-to "resources/public/assets/js/work_to_list.js"
                           :output-dir "resources/public/assets/js/deps"
                           :source-map-timestamp true}}
               {:id "min"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/assets/js/work_to_list.js"
                           :main work-to-list.client.core
                           :optimizations :advanced
                           :pretty-print false}}]}

  :figwheel {:css-dirs ["resources/public/assets/css"]}

  :release-tasks
  [["vcs" "assert-committed"]
   ["change" "version" "leiningen.release/bump-version" "release"]
   ["vcs" "commit"]
   ["vcs" "tag"]
   ["change" "version" "leiningen.release/bump-version"]
   ["vcs" "commit"]
   ["vcs" "push"]])
