(defproject city-council-html-service "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [etaoin "1.0.40"]
                 [cheshire "5.12.0"]
                 [hiccup "2.0.0-RC2"]
                 [clj-http "3.12.3"]
                 [org.clj-commons/hickory "0.7.3"]
                 [ring/ring-core "1.11.0-RC2"]]
  :repl-options {:init-ns city-council-html-service.core})
