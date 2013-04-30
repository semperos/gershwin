(ns gershwin.replloader
  (:gen-class))

(defn -main
  "Start a Gershwin REPL. This is required to piggy-back on the Clojure environment properly."
  [& args]
  (gershwin.core/gershwin-compile 'gershwin.core)
  (gershwin.GershwinRepl/main args))