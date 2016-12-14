# re-thread

A tiny Clojurescript library for interacting with re-frame when it's running in a Web Worker.

## Usage

In your Web Worker re-frame app:

``` clojure
(ns your-worker-app.core
  (:require
   [re-thread.worker :refer [listen!]]
   ...))

(listen!)

```

Your client/host application will consist of only views and handler calls, delegating everything else to the worker.

To initialize the worker, use `re-thread.client/init!`:

``` clojure
(ns your-client-app.core
  (:require
   [re-thread.client :refer [init!]]
   ...))


;; Remember, in reloadable environments, you only want to run init! once.
(defonce init-worker
  (delay
   ...
   (init! "/js/compiled_worker.js")))

@bootstrap

```

Then, in your components, you can pretty much forget that things are happening in a worker, and use `re-thread.client/dispatch` and `re-thread.client/subscribe` in the manner to which you are accustomed:

``` clojure
(ns your-client-app.views.counter
  (:require [re-thread.client :refer [dispatch subscribe]]))

(defn counter []
  (let [v (subscribe [:counter/value] 0)] ;; <- note that you can provide a default value..
    (fn []
      [:div
       [:p "Counter Value: " (str @v)
        [:span
         [:button
          {:on-click #(dispatch [:counter/inc-value!])} ;; <- registered in the worker
          "Increment"]
         [:button
          {:on-click #(dispatch [:counter/reset-value!])}
          "Reset"]]]])))

```

## Gotchas

Since all of the app state is contained in the worker process, you should take care not to write view code that fails if a subscription returns nil. Using the optional second arg of `subscribe` can help with that.

## License

Copyright © 2016 Yet Analytics Inc.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
