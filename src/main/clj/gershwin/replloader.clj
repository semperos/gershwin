(ns gershwin.replloader
  (:import gershwin.GershwinRepl)
  (:gen-class))

(defn -main
  "Start a Gershwin REPL. This function is executed from within the 'user' namespace in gershwin/main.clj, hence the runtime call to the gershwin-require Clojure function to pull in Gershwin itself."
  [& args]
  (gershwin.core/gershwin-require '[gershwin.core :refer :all])
  (GershwinRepl/main args))