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

(defn execute-with-threshold[_config command]
  (let [monitor (get-monitor _config command)
        threshold (monitor :threshold)
        result (execute _config :monitors command)
        check (str "(#(" threshold ") " result ") ")
        ev-check (load-string check)
        ]
    (println check)
    (println ":" ev-check)
    (if (= true ev-check) ;#((> (Float. result) threshold)
      "1"
      "0")))

(defn execute-all[_config]
  (let[monitors (_config :monitors)]
    (pmap 
      #(conj 
        % 
        {:result 
          (execute-with-threshold _config (% :handler))}) 
      monitors)))

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
      (GET "/" [] (monitors-page))
      ))