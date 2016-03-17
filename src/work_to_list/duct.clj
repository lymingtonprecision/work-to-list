(ns work-to-list.duct
  "A protocol and component based composable system of Ring request handlers.
  Heavily inspired by, if not a direct ripoff of,
  [Duct](https://github.com/weavejester/duct).

  The main difference between this and Duct is that it has no dependency on
  Compojure and formalizes the endpoints with a protocol.

  Some points of note:

  * Both `RingEndpoint`s and `RingRouter`s store the actual Ring request
    processing fn under a `:handler` key when started.
  * Both components can also serve as Ring handlers in their own right: they can
    be called as fns taking a single argument.
  * `RingRouter`s implement the endpoint protocol and so can be nested."
  (:require [com.stuartsierra.component :as component]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Protocols

(defprotocol IRingEndpoint
  (process-request [this req]))

(defprotocol IRingRouter
  (route-request [this req]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Composition fns

(defn find-endpoints
  "Returns the values from the component-map whose keys are specified in an
  `:endpoints` entry. Or, if no such entry exists, returns the collection of
  entries from the map that satisfy the `IRingEndpoint` protocol."
  [{:keys [endpoints] :as component-map}]
  (if endpoints
    (select-keys component-map endpoints)
    (filter
     #(satisfies? IRingEndpoint %)
     (if (map? component-map)
       (vals component-map)
       (seq component-map)))))

(defn route-between
  "Returns a fn that will route a ring request between the given endpoints,
  returning the first non-nil value.

  (The request will be routed through the endpoints _in order_ and only those
  endpoints up to the completing end point will be attempted.)"
  [endpoints]
  (apply some-fn endpoints))

(defn middleware-fn
  "`middleware-def` should be either a fn or a vector of
  `[mw-fn :first-component :second-component ...]` where the `:*-component` keys
  are contained within `component-map`.

  In the later case a fn will be returned which takes a single argument and
  passes it as the first argument to `mw-fn` followed by the values of the
  requested components (in the order specified.)

      (middleware-fn [mw-fn :db :mailer] {:db ...db... :mailer ...mailer...})
      ;=> (fn [h] (mw-fn h ...db... ...mailer...))

  If `middleware-def` is *not* a vector then it is returned as is."
  [middleware-def component-map]
  (if (vector? middleware-def)
    (let [[f & required-components] middleware-def
          args (map #(get component-map %) required-components)]
      #(apply f % args))
    middleware-def))

(defn compose-middleware-wrapper
  "Returns a single wrapper fn that is the composition of the given middlewares
  (each begin either a fn or definition per `middleware-fn`.)"
  [middleware component-map]
  (->> (reverse middleware)
       (map #(middleware-fn % component-map))
       (apply comp identity)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Components

(defrecord RingEndpoint [build-handler]
  component/Lifecycle
  (start [this]
    (if (:handler this)
      this
      (assoc this :handler (build-handler this))))
  (stop [this]
    (dissoc this :handler))

  IRingEndpoint
  (process-request [this req]
    (when-let [f (:handler this)]
      (f req)))

  clojure.lang.Fn
  clojure.lang.IFn
  (invoke [this req]
    (process-request this req)))

(defrecord RingRouter [middleware]
  component/Lifecycle
  (start [this]
    (if (:handler this)
      this
      (let [endpoints (find-endpoints this)
            endpoint-router (route-between endpoints)
            middleware-wrapper (compose-middleware-wrapper middleware this)
            router (middleware-wrapper endpoint-router)]
        (assoc this :handler router))))
  (stop [this]
    (dissoc this :handler))

  IRingRouter
  (route-request [this req]
    (when-let [f (:handler this)]
      (f req)))

  IRingEndpoint
  (process-request [this req]
    (route-request this req))

  clojure.lang.Fn
  clojure.lang.IFn
  (invoke [this req]
    (route-request this req)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn ring-endpoint
  "Returns a new endpoint component that will use the given `build-handler` fn
  to construct it's Ring handler when started.

  `build-handler` must be a fn that accepts a single argument: the component map
  given to the endpoint when started, and returns a Ring request handler."
  [build-handler]
  (->RingEndpoint build-handler))

(defn ring-router
  "Returns a new router component. The supported options are:

  * `:endpoints`
    The list of endpoint component keys this router should wrap. If
    not provided the router will simply wrap all components conforming to the
    `IRingEndpoint` protocol.

  * `:middleware` A vector of Ring middlewares to apply to every request handled
    by the router. Each middleware entry should be either the middleware wrapper
    fn or a vector of `[mw-fn :dependecy1 :dependency2 ...]` (see `middleware-fn`
    for details.) Any middleware dependencies specified in this way must also be
    provided in the options map (or injected via Component.)

  Example usage:

      (-> (component/system-map
           ;; some generic components for external services
           :server (http-server-component)
           :database (database-component)
           ;; our Ring endpoints
           :blog (ring-endpoint blog-handler)
           :admin (ring-endpoint admin-handler)
           ;; ... and a router to tie them together
           :router (ring-router
                    {:endpoints [:blog :admin] ;; not _required_
                     :middleware [[wrap-not-found :not-found]
                                  wrap-webjars
                                  [wrap-defaults :defaults]]
                     :not-found (io/resource \"/errors/404.html\")
                     :defaults site-defaults}))
          ;; ... and the dependencies between them
          (component/system-using
           {:blog [:database]
            :admin [:database]
            :router [:blog :admin]
            :server {:ring-handler :router}}))

  Note that we can supply our `:router` as a Ring handler to our HTTP server and
  it doesn't need to know anything about our protocols: the router is callable
  as a fn taking a single argument, just like a normal Ring handler."
  [options]
  (map->RingRouter options))
