(ns app-control.handler
  (:use app-control.routes.monitors)
  (:use ring.server.standalone)
  (:use jakemcc.clojure-gntp.gntp)
  (:require [app-control.config :as config])
  (:gen-class)
  (:require [compojure.core :refer [defroutes]]
            [postal.core :as postal]
            [app-control.routes.home :refer [home-routes]]
            [app-control.routes.monitors :refer [monitors-routes]]
            [app-control.middleware :refer [load-middleware]]
            [app-control.session-manager :as session-manager]
            [noir.response :refer [redirect]]
            [noir.util.middleware :refer [app-handler]]
            [compojure.route :as route]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.rotor :as rotor]
            [selmer.parser :as parser]
            [environ.core :refer [env]]
            [cronj.core :as cronj]))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(defn notify[_config res]
  (if (-> _config :notification :growl)
    (future (message "SHIMO" (str (res :title) " is probably ↓"))))
  (if (-> _config :notification :mail)
    (future
      (postal/send-message 
              (-> _config :notification :mail :stmp)
              {:from (-> _config :notification :mail :from)
               :to (-> _config :notification :mail :to)
               :subject (str "Hi!" (res :title) " is probably ↓")
               :body "☆"
               :X-Tra "Something else"}))))

(defn monitor-handler [t opts]
  (let [
    _config (config/config)
    results (config/execute-all _config)]
    (doseq [res results]
        (if (= (res :result) "1")
          (timbre/debug (res :title) " ↑")
          (do 
            (notify _config res)
            (timbre/debug (res :title) " ↓"))))))

(def monitoring-job 
  (cronj/cronj :entries [
    {:id "monitoring"
     :handler monitor-handler
     :schedule ((config/config) :cron)
     :opts {}}
    ]))

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []
  (timbre/set-config!
    [:appenders :rotor]
    {:min-level :info
     :enabled? true
     :async? false ; should be always false for rotor
     :max-message-per-msecs nil
     :fn rotor/appender-fn})

  (timbre/set-config!
    [:shared-appender-config :rotor]
    {:path "app_control.log" :max-size (* 512 1024) :backlog 10})

  (if (env :dev) (parser/cache-off!))
  ;;start the expired session cleanup job
  (cronj/start! session-manager/cleanup-job)

  (let [_config (config/config) _cron (-> _config :cron)]
   (if _cron
    (do 
      (timbre/info "Starting background monitoring:" _cron)
      (cronj/start! monitoring-job))))
  (timbre/info "app-control started successfully"))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (timbre/info "app-control is shutting down...")
  (cronj/shutdown! session-manager/cleanup-job)

  (cronj/shutdown! monitoring-job)
  (timbre/info "shutdown complete!"))

(def app (app-handler
           ;; add your application routes here
           [monitors-routes home-routes app-routes]
           ;[monitors-routes app-routes]
           ;; add custom middleware here
           :middleware (load-middleware)
           ;; timeout sessions after 30 minutes
           :session-options {:timeout (* 60 30)
                             :timeout-response (redirect "/")}
           ;; add access rules here
           :access-rules []
           ;; serialize/deserialize the following data formats
           ;; available formats:
           ;; :json :json-kw :yaml :yaml-kw :edn :yaml-in-html
           :formats [:json-kw :edn]))

(defn -main[& args]
  (if args
    (do 
    (timbre/info "Using config file " (first args))
    (dosync 
      (ref-set config/config-ref (first args)))))
  (serve app))