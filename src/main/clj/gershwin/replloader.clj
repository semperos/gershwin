(ns gershwin.replloader
  (:require [gershwin.rt :as rt])
  (:import gershwin.GershwinRepl)
  (:gen-class))

(defn -main
  "Start a Gershwin REPL. This function is executed from within the 'user' namespace in gershwin/main.clj, hence the runtime call to the gershwin-require Clojure function to pull in Gershwin itself."
  [& args]
  (rt/gershwin-require '[gershwin.core :refer :all])
  (GershwinRepl/main args))