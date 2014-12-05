(ns app-control.routes.home
  (:use  [clojure.java.shell :only [sh]])
  (:require [compojure.core :refer :all]
            [app-control.layout :as layout]
            [app-control.util :as util]))

; reload this on each request
(defn config []
  (load-file "config.clj"))

(defn home-page []
  (layout/render
    "home.html" {
      :config (config)
      :content (util/md->html "/md/docs.md")}))

(defn result-page [result]
  (layout/render 
    "about.html" {:content (str result)}))

(defn output-to-html [result]
  (util/replace-several 
    result 
    #"\n" "<br/>" 
    #"\t"  "&nbsp;&nbsp;&nbsp;&nbsp;"))

(defn get-sh[_config command]
  (get 
    (first (filter #(= (% :handler) command) (_config :commands))) :sh))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/rest/:command" [command] 
    (let [_config (config) params (get-sh _config command)]
      (:out (apply sh (conj (_config :base) params)))))
  (GET "/action/:command" [command] 
    (let [_config (config) params (get-sh _config command)]
      (result-page
        (output-to-html  (:out (apply sh (conj (_config :base) params))))))))