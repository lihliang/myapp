(defproject myapp "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [buddy/buddy-core "1.6.0"]
                 [clj-http "3.10.0"]
                 [clj-time "0.15.0"]
                 [cheshire/cheshire "5.8.1"]
                 [cprop/cprop "0.1.13"]
                 [org.clojure/clojure "1.10.0"]
                 [metosin/compojure-api "1.1.11"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [ring/ring-devel "1.7.1"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.11.2"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-codec "1.1.2"]
                 [com.novemberain/monger "3.1.0"]
                 [liberator "0.15.3"]]
  :plugins [[lein-ring "0.12.5"]
            [lein-cloverage "1.1.1"]]

  :main ^:skip-aot myapp.core
  :ring {:handler myapp.core/app}
  :profiles {
             :uberjar {
                       :omit-source true
                       :aot :all
                       :uberjar-name "myapp.jar"
                       :resource-path ["resources/dev"]
                       }
             :dev {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]
                                  [ring/ring-mock "0.3.2"]]
                   :resource-paths ["resources/dev"]}
             }
  :repl-options {:init-ns myapp.core})
