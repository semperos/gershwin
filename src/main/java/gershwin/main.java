package gershwin;

import clojure.lang.Symbol;
import clojure.lang.Var;

import java.io.IOException;

public class main {
    final static private Var REQUIRE = clojure.lang.RT.var("clojure.core", "require");
    final static private Symbol GERSHWIN_MAIN = Symbol.intern("gershwin.main");
    final static private Var MAIN = clojure.lang.RT.var("gershwin.main", "main");

    public static void main(String[] args) throws ClassNotFoundException, IOException {
        REQUIRE.invoke(GERSHWIN_MAIN);
        MAIN.applyTo(clojure.lang.RT.seq(args));
    }
}
