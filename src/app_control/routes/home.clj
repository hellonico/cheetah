(ns app-control.routes.home
  (:use  [clojure.java.shell :only [sh]])
  (:use  [app-control.config])
  (:require [compojure.core :refer :all]
            [app-control.layout :as layout]
            [app-control.util :as util]))

(defn home-page []
  (layout/render
    "home.html" {
      :config (config)
      :content (util/md->html "/md/docs.md")}))

(defn result-page [config result]
  (layout/render 
    "about.html" {:config config :content (str result)}))

(defn output-to-html [result]
  (util/replace-several 
    result 
    #"\n" "<br/>" 
    #"\t"  "&nbsp;&nbsp;&nbsp;&nbsp;"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/rest/:command" [command] 
      (execute (config) :commands command))
  (GET "/web/:command" [command] 
    (let [_config (config)]
      (result-page
        _config 
        (output-to-html (execute _config :commands command))))))