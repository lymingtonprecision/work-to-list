(ns work-to-list.components.frontend
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [cheshire.core :as cheshire]
            [clj-time.core :as time]
            [clj-time.coerce :as time.coerce]
            [hiccup.element :refer [javascript-tag]]
            [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.util :refer [to-str to-uri with-base-url escape-html]]
            [pandect.algo.sha1 :refer [sha1]]
            [ring.middleware.resource :refer [resource-request]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.util.time :refer [format-date]]
            [work-to-list.duct :as duct]
            [work-to-list.routes :as routes]
            [work-to-list.protocols.work-to-list :as wtl])
  (:import java.net.JarURLConnection))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Terminal Identification

(defn remote-host [ip]
  (let [ip (java.net.InetAddress/getByName ip)]
    (try
      (.getCanonicalHostName ip)
      (catch java.net.UnknownHostException e
        (str ip)))))

(defn terminal-id [hostname]
  (when-let [id (second (re-find #"(?i)^lpe-tml-(\d{3})(\.|$)" hostname))]
    (Integer. (string/replace id #"^0+" ""))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Asset fingerprinting

(def asset-fingerprints (atom {}))

(defn last-modified [^java.net.URL resource]
  (case (.getProtocol resource)
    "jar" (.getLastModified
           (cast JarURLConnection (.openConnection resource)))
    (.lastModified (io/file resource))))

(defn fingerprint [^java.net.URL resource]
  (with-open [in (io/input-stream resource)]
    (sha1 in)))

(defn get-or-set-fingerprint [fingerprints k ^java.net.URL resource]
  (if-let [fp (get @fingerprints k)]
    fp
    (get (swap! fingerprints assoc k (fingerprint resource)) k)))

(defn asset [path]
  (if-let [resource (io/resource (str "public" path))]
    (let [lm (last-modified resource)
          k [path lm]
          fp (get-or-set-fingerprint asset-fingerprints k resource)]
      (str path "?v=" fp))
    path))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utility fns

(defn url
  [path]
  (-> path to-uri to-str))

(defn update-terminal-id
  [html req]
  (let [id (-> (:remote-addr req) remote-host terminal-id)]
    (string/replace html #"\$\{TERMINAL_ID\}" (cheshire/generate-string id))))

(defn ten-years-from-now []
  (-> (time/now)
      (time/plus (time/years 10))
      (time.coerce/to-date)))

(defn remove-prefix [uri prefix]
  (if (string/starts-with? uri prefix)
    (subs uri (dec (count prefix)))
    uri))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; HTML

(defn index
  [uri-prefix work-to-list]
  (with-base-url uri-prefix
    (html5
     [:head
      [:title "Work To List"]
      [:meta {:charset "UTf-8"}]
      [:meta {:name "viewport" :content "width=device-width,initial-scale=1"}]
      [:link {:rel "icon" :href (url (asset "/favicon.ico")) :type "image/x-icon"}]
      [:link {:rel "shortcut icon" :href (url (asset "/favicon.ico")) :type "image/x-icon"}]
      (include-css (asset "/assets/css/normalize.min.css"))
      (include-css (asset "/assets/css/font-awesome.min.css"))
      (include-css (asset "/assets/css/main.css"))]
     [:body
      [:div#app]
      (javascript-tag
       (str "var BASE_URL=\"" (url "/") "\";"
            "var TERMINAL_ID=${TERMINAL_ID};"
            "var INIT_STATE=" (cheshire/generate-string (pr-str work-to-list)) ";"))
      (include-js (asset "/assets/js/work_to_list.js"))
      (javascript-tag
       (str "window.onload = function () {work_to_list.client.core.run();}"))])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Ring

(defn handler
  [uri-prefix work-to-list]
  (let [routes (routes/frontend uri-prefix)
        generate-index (partial index uri-prefix)
        spa-page (atom (generate-index (wtl/tasks work-to-list)))
        last-modified (atom nil)
        update-last-modified!
        (fn [] (reset! last-modified (format-date (java.util.Date.))))]
    (update-last-modified!)
    (wtl/add-watch
     work-to-list
     (java.util.UUID/randomUUID)
     (fn [k ref old-state new-state]
       (reset! spa-page (generate-index new-state))
       (update-last-modified!)))
    (fn [{:keys [uri path-info] :as req}]
      (when (routes/match-route routes (or path-info uri))
        {:status 200
         :headers {"Content-Type" "text/html;charset=utf-8"
                   "Last-Modified" @last-modified}
         :body (update-terminal-id @spa-page req)}))))

(defn wrap-prefixed-resource [handler uri-prefix]
  (let [uri-prefix (routes/sanitize-prefix uri-prefix)]
    (fn [req]
      (let [resource-uri (remove-prefix
                          (or (:path-info req) (:uri req))
                          uri-prefix)
            resource-resp (resource-request
                           (assoc req :path-info resource-uri)
                           "public")]
        (if resource-resp
          (assoc-in resource-resp [:headers "Expires"] (ten-years-from-now))
          (handler req))))))

(defn wrap-handler
  [handler uri-prefix]
  (-> handler
      wrap-not-modified
      (wrap-prefixed-resource uri-prefix)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn frontend
  [{{:keys [uri-prefix] :or {uri-prefix "/"}} :http-config
    work-to-list :work-to-list}]
  (wrap-handler (handler uri-prefix work-to-list) uri-prefix))

(defn frontend-endpoint []
  (duct/ring-endpoint frontend))
