package gershwin.lang;

import clojure.lang.IFn;
import clojure.lang.Symbol;
import clojure.lang.Var;

public class ClojureApi {

    private static Symbol asSym(Object o) {
        Symbol s;
        if (o instanceof String) {
            s = Symbol.intern((String) o);
        }  else {
            s = (Symbol) o;
        }
        return s;
    }

    /**
     * Returns the var associated with qualifiedName.
     *
     * @param qualifiedName  a String or clojure.lang.Symbol
     * @return               a clojure.lang.IFn
     */
    public static IFn var(Object qualifiedName) {
        Symbol s = asSym(qualifiedName);
        return var(s.getNamespace(), s.getName());
    }

    /**
     * Returns an IFn associated with the namespace and name.
     *
     * @param ns        a String or clojure.lang.Symbol
     * @param name      a String or clojure.lang.Symbol
     * @return          a clojure.lang.IFn
     */
    public static IFn var(Object ns, Object name) {
        return Var.intern(asSym(ns), asSym(name));
    }

    /**
     * Read one object from the String s.  Reads data in the
     * <a href="http://edn-format.org">edn format</a>.
     * @param     s
     * @return    an Object, or nil.
     */
    public static Object read(String s) {
        // LineNumberingPushbackReader pbr = new LineNumberingPushbackReader(new java.io.StringReader(s));
        // return LispReader.read(pbr, true, null, true);
        return CLOJURE_READ_STRING.invoke(s);
    }

    // static {
    //     Symbol edn = (Symbol) var("clojure.core", "symbol").invoke("clojure.edn");
    //     var("clojure.core", "require").invoke(edn);
    // }
    // private static final IFn EDN_READ_STRING = var("clojure.edn", "read-string");
    private static final IFn CLOJURE_READ_STRING = var("clojure.core", "read-string");
}
