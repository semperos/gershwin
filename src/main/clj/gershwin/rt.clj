(ns gershwin.rt
  (:import [gershwin.lang Stack Stack$StackUnderflowException]))

(defmacro stack-void
  "The keyword :gershwin.core/stack-void is a special value that the compiler will not put on the data stack. Use this to evaluate Clojure but to prevent the return value of the Clojure expression from being added to the stack."
  [& body]
  `(do ~@body :gershwin.core/stack-void))

(defn stack-clear
  "Clear the stack"
  []
  (Stack/clear))

(defmacro gershwin-ns
  "Wrap ns with stack void. Having do or some stack popping at the beginning of every file is ugly."
  [& body]
  `(stack-void
    (ns ~@body)
    (clojure.core/refer 'gershwin.core)))

(defn gershwin-resolve
  "Resolve the `name` of a language form as a var, checking Gershwin var definitions with a fallback to Clojure ones. Gershwin does some mangling of names to prevent clashes with core Clojure forms, so this replicates that."
  [s]
  (let [vr (-> s name gershwin.lang.RT/formatGershwinName
               symbol resolve)]
    (if vr
      vr
      (resolve (symbol s)))))

(defn gershwin-var
  [s]
  (-> s name gershwin.lang.RT/formatGershwinName
      symbol resolve))

(defn st-peek [] (Stack/peek))

(defn peek* [coll]
  "Like Clojure's peek, but throws an exception if the stack is empty."
  (if (zero? (count coll))
    (throw (Stack$StackUnderflowException. "Data stack underflow. Can't take something off an empty data stack."))
    (peek coll)))

(defn st-pop
  "Call Stack's pop method. Immutable."
  []
  (Stack/pop))

(defn pop-it
  "Remove and return the item on TOS. Mutable."
  []
  (Stack/popIt))

(defn conj-it
  "Conj the item onto the data stack. Mutable."
  [x] (Stack/conjMutable x))

(defn ap
  "Apply the function to TOS"
  [a-fn] (a-fn (pop-it)))

(defn ap2
  "Apply the function to the top two items on TOS"
  [a-fn] (a-fn (pop-it) (pop-it)))

(defn ap3
  "Apply the function to the top three items on TOS"
  [a-fn] (a-fn (pop-it) (pop-it) (pop-it)))

(defn pop-n-swap
  "Remove top two items from TOS, swap, then apply the function."
  [a-fn] (let [a (pop-it) b (pop-it)] (a-fn b a)))

(defn pop-n-swap2
  "x y z --> z y x and pass it that way to a Clojure function."
  [a-fn] (let [z (pop-it) y (pop-it) x (pop-it)] (a-fn x y z)))

;;;;
;; BEGIN Copying of Clojure's core.clj, to allow loading using
;; Gershwin's loader instead of Clojure's.
;;;;

(defonce ^:dynamic
  ^{:private true
    :doc "A stack of paths currently being loaded by this thread"}
  *pending-paths* ())

(defonce ^:dynamic
  ^{:private true :doc "True while a verbose load is pending"}
  *loading-verbosely* false)

(defn- check-cyclic-dependency
  "Detects and rejects non-trivial cyclic load dependencies. The
    exception message shows the dependency chain with the cycle
    highlighted. Ignores the trivial case of a file attempting to load
    itself because that can occur when a gen-class'd class loads its
    implementation."
  [path]
  (when (some #{path} (rest *pending-paths*))
    (let [pending (map #(if (= % path) (str "[ " % " ]") %)
                       (cons path *pending-paths*))
          chain (apply str (interpose "->" pending))]
      (throw (Exception. (str "Cyclic load dependency: " chain))))))

(defn- root-resource
  "Returns the root directory path for a lib"
  {:tag String}
  [lib]
  (str \/
       (.. (name lib)
           (replace \- \_)
           (replace \. \/))))

(defn- root-directory
  "Returns the root resource path for a lib"
  [lib]
  (let [d (root-resource lib)]
    (subs d 0 (.lastIndexOf d "/"))))

(defn gershwin-load
  "Loads Clojure code from resources in classpath. A path is interpreted as
    classpath-relative if it begins with a slash or relative to the root
    directory for the current namespace otherwise."
  {:added "1.0"}
  [& paths]
  (doseq [^String path paths]
    (let [^String path (if (.startsWith path "/")
                         path
                         (str (root-directory (ns-name *ns*)) \/ path))]
      (when *loading-verbosely*
        (printf "(gershwin.core/gershwin-load \"%s\")\n" path)
        (flush))
      (check-cyclic-dependency path)
      (when-not (= path (first *pending-paths*))
        (binding [*pending-paths* (conj *pending-paths* path)]
          ;; This is the main departure from Clojure's load.
          ;; This calls Compiler.compile when *compile-files*
          ;; is set to true, as in the `compile` fn below.
          (gershwin.lang.RT/load (.substring path 1)))))))

;;;;
;; END Copying
;;;;

(let [properties (with-open [version-stream (.getResourceAsStream
                                             (clojure.lang.RT/baseLoader)
                                             "gershwin/version.properties")]
                   (doto (new java.util.Properties)
                     (.load version-stream)))
      version-string (.getProperty properties "version")
      [_ major minor incremental qualifier snapshot]
      (re-matches
       #"(\d+)\.(\d+)\.(\d+)(?:-([a-zA-Z0-9_]+))?(?:-(SNAPSHOT))?"
       version-string)
      gershwin-version {:major       (Integer/valueOf ^String major)
                        :minor       (Integer/valueOf ^String minor)
                        :incremental (Integer/valueOf ^String incremental)
                        :qualifier   (if (= qualifier "SNAPSHOT") nil qualifier)}]
  (def ^{:dynamic true
         :doc "The version info for Gershwin core, as a map containing :major :minor
:incremental and :qualifier keys. Feature releases may increment
:minor and/or :major, bugfix releases will increment :incremental.
Possible values of :qualifier include \"GA\", \"SNAPSHOT\", \"RC-x\" \"BETA-x\""}
    *gershwin-version*
    (if (.contains version-string "SNAPSHOT")
      (clojure.lang.RT/assoc gershwin-version :interim true)
      gershwin-version)))

(defn
  gershwin-version
  "Returns Gershwin version as a printable string."
  {:added "1.0"}
  []
  (str (:major *gershwin-version*)
       "."
       (:minor *gershwin-version*)
       (when-let [i (:incremental *gershwin-version*)]
         (str "." i))
       (when-let [q (:qualifier *gershwin-version*)]
         (when (pos? (count q)) (str "-" q)))
       (when (:interim *gershwin-version*)
         "-SNAPSHOT")))

(defn gershwin-read
  "Reads the next object from stream, which must be an instance of
  java.io.PushbackReader or some derivee.  stream defaults to the
  current value of *in*.

  Note that read can execute code (controlled by *read-eval*),
  and as such should be used only with trusted sources.

  For data structure interop use clojure.edn/read"
  {:static true}
  ([]
   (read *in*))
  ([stream]
   (read stream true nil))
  ([stream eof-error? eof-value]
   (read stream eof-error? eof-value false))
  ([stream eof-error? eof-value recursive?]
   (. gershwin.lang.Parser (read stream (boolean eof-error?) eof-value recursive?))))

(defn gershwin-eval
  "Evaluates the form data structure (not text!) and returns the result."
  {:static true}
  [form]
  (. gershwin.lang.Compiler (eval form)))

(defn gershwin-compile
  "Compiles the namespace named by the symbol lib into a set of
  classfiles. The source for the lib must be in a proper
  classpath-relative directory. The output files will go into the
  directory specified by *compile-path*, and that directory too must
  be in the classpath."
  [lib]
  (binding [*compile-files* true]
    ;; Part of load-one definition
    (gershwin-load (root-resource lib)))
  lib)

(defn gershwin-require
  "For now, this support requiring a single lib at a time. "
  [spec]
  (if (coll? spec)
    (let [suffix gershwin.lang.RT/GERSHWIN_SUFFIX
          lib (first spec)
          kw (-> spec next first)
          words (let [ws (-> spec next next first)]
                  (when (coll? ws) ;; e.g., :all
                    (map #(symbol (str % suffix)) ws)))]
      (gershwin-compile lib)
      (if words
        (require [lib kw words])
        (require spec)))
    (do
      (gershwin-compile spec)
      (require spec))))