package gershwin.lang;

import clojure.lang.IFn;
import clojure.lang.Keyword;
import clojure.lang.Namespace;
import clojure.lang.Symbol;

public class RT {
    final static private String GERSHWIN_VAR_PREFIX = "__GWN__";
    final static private IFn IN_NS = ClojureApi.var("clojure.core", "in-ns");
    final static private IFn IMPORT = ClojureApi.var("clojure.core", "import");
    final static private IFn REFER = ClojureApi.var("clojure.core", "refer");
    final static private Symbol STACK_CLASS_SYM = Symbol.intern("Stack");
    final static private Symbol CLOJURE = Symbol.intern("clojure.core");
    final static private Symbol GERSHWIN = Symbol.intern("gershwin.core");
    final static private Namespace GERSHWIN_NS = Namespace.findOrCreate(GERSHWIN);

    public static void doInit() {
        // clojure.lang.Compiler.eval(RT.list(IN_NS, GERSHWIN));
        IN_NS.invoke(GERSHWIN);
        GERSHWIN_NS.importClass(STACK_CLASS_SYM, gershwin.lang.Stack.class);
        REFER.invoke(CLOJURE);
    }

    public final static Keyword STACK_VOID = Keyword.intern("gershwin.core", "stack-void");
}

/**
// From clojure.lang.RT. Note the inclusion of a definition for load-file.
static{
	Keyword arglistskw = Keyword.intern(null, "arglists");
	Symbol namesym = Symbol.intern("name");
	OUT.setTag(Symbol.intern("java.io.Writer"));
	CURRENT_NS.setTag(Symbol.intern("clojure.lang.Namespace"));
	AGENT.setMeta(map(DOC_KEY, "The agent currently running an action on this thread, else nil"));
	AGENT.setTag(Symbol.intern("clojure.lang.Agent"));
	MATH_CONTEXT.setTag(Symbol.intern("java.math.MathContext"));
	Var nv = Var.intern(CLOJURE_NS, NAMESPACE, bootNamespace);
	nv.setMacro();
	Var v;
	v = Var.intern(CLOJURE_NS, IN_NAMESPACE, inNamespace);
	v.setMeta(map(DOC_KEY, "Sets *ns* to the namespace named by the symbol, creating it if needed.",
	              arglistskw, list(vector(namesym))));
	v = Var.intern(CLOJURE_NS, LOAD_FILE,
	               new AFn(){
		               public Object invoke(Object arg1) {
			               try
				               {
				               return Compiler.loadFile((String) arg1);
				               }
			               catch(IOException e)
				               {
				               throw Util.sneakyThrow(e);
				               }
		               }
	               });
	v.setMeta(map(DOC_KEY, "Sequentially read and evaluate the set of forms contained in the file.",
	              arglistskw, list(vector(namesym))));
	try {
		doInit();
	}
	catch(Exception e) {
		throw Util.sneakyThrow(e);
	}
}
**/
