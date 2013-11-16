(defproject com.tombooth/friend-token "0.1.1-SNAPSHOT"
  :description "A token workflow for the Friend authentication middleware"
  :url "https://github.com/tombooth/friend-token"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [ring/ring-core "1.2.0"]
                 [com.cemerick/friend "0.1.5"]
                 [clj-time "0.5.1"]
                 [cheshire "5.2.0"]])
