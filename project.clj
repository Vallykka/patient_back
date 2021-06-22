(defproject patient_back "0.1.0-SNAPSHOT"
  :description "Patient catalog project"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [mount "0.1.16"]
                 [cprop "0.1.17"]

                 [org.clojure/tools.logging "1.1.0"]
                 [com.widdindustries/clojure.log4j2 "0.1.3"]

                 [clj-http "3.12.2"]
                 [bidi "2.1.6"]
                 [ring/ring-core "1.9.3"]
                 [ring/ring-jetty-adapter "1.9.3"]
                 [ring/ring-json "0.5.1"]
                 [ring-cors "0.1.13"]
                 [org.eclipse.jetty/jetty-server "9.4.40.v20210413"]

                 [clj-postgresql "0.7.0" :exclusions [org.clojure/java.data org.clojure/java.jdbc]]
                 [conman "0.9.0"]
                 [migratus "1.2.8"]

                 [org.clojure/core.specs.alpha "0.2.44"]
                 [org.clojure/test.check "1.1.0"]]

  :plugins [[migratus-lein "0.7.3"]]

  :main ^:skip-aot patient.core
  :aot [patient.core]
  :repl-options {:init-ns patient.core}
  :clean-targets ^{:protect false} [:target-path "target"]
  :resource-paths ["resources"]
  :profiles {:uberjar {:source-paths ["src"]
                       :resource-paths ["resources"]
                       :aot :all
                       }
             :test          [:project/test :profiles/test]
             :project/test {:resource-paths ["test/resources"]}
             :profiles/test {}
             }
  )
