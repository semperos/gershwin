(ns gershwin.compile
  (:require [gershwin.rt :as rt])
  (:gen-class))

(def ^{:private true}
  compile-path-prop
  "clojure.compile.path")

(defn- compile-path-defined?
  []
  (System/getProperty compile-path-prop))

(defn -main
  [& args]
  (when-not (compile-path-defined?)
    (println (str "ERROR: Must set system property " compile-path-prop
                  "\nto the location for compiled files."
                  "\nThis directory must also be on your CLASSPATH."))
    (System/exit 1))
  (let [path (System/getProperty compile-path-prop)]
    (doseq [lib args]
      (println (str "Compiling Gershwin " lib " to " path))
      (rt/gershwin-compile (symbol lib)))))