(defproject metabase/jar-compression "1.0.0"
  :min-lein-version "2.5.0"

  :dependencies
  [[commons-io "2.6"]
   [org.tukaani/xz "1.8"]]

  :profiles
  {:dev
   {:dependencies
    [[org.clojure/clojure "1.9.0"]]}}

  :deploy-repositories
  [["clojars" {:sign-releases false}]])
