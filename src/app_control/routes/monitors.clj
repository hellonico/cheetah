(ns app-control.routes.monitors
  (:use  [clojure.java.shell :only [sh]])
  (:use  [app-control.config])
  (:require [compojure.core :refer :all]
            [app-control.layout :as layout]
            [app-control.util :as util]))

(defn monitors-page []
  (layout/render
    "monitors.html" {
      :config (config)
      :content (util/md->html "/md/docs.md")}))

(defn result-page [config result]
  (layout/render 
    "_monitor_result.html" 
    {:monitor result :config config}))

(defn all-page [config results]
  (layout/render 
    "_monitor_all.html" 
    {:config config :monitors results}))

(defroutes monitors-routes
  (context "/monitors" []
      (GET "/all" [] 
        (let[_config (config)]
          (all-page _config (execute-all _config))))
      (GET "/web/:command" [command] 
        (let [_config (config)]
          (result-page 
         _config 
          (conj (get-monitor _config command) {:result (execute-with-threshold _config command)})
          )))
      (GET "/rest/all" []
        (execute-all (config)))
      (GET "/rest/:command" [command]
        {:result (execute-with-threshold (config) command)})
      (GET "/" [] (monitors-page))
      ))