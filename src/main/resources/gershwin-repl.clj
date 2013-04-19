(ns gershwin-repl)

(defn main
  "Start a Gershwin REPL. This is required to piggy-back on the Clojure environment properly."
  [& args]
  (gershwin.GershwinRepl/main args))

(main)
