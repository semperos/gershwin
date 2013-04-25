package gershwin.lang;

import clojure.lang.IFn;
import clojure.lang.Keyword;
import clojure.lang.Namespace;
import clojure.lang.Symbol;
import clojure.lang.Var;
import static clojure.lang.RT.baseLoader;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

public class RT {
    final static private String GERSHWIN_SUFFIX = "__GWN__";
    final static Symbol LOAD_FILE = Symbol.intern(formatGershwinName("load-file"));
    final static private IFn IN_NS = ClojureApi.var("clojure.core", "in-ns");
    final static private IFn REFER = ClojureApi.var("clojure.core", "refer");
    final static private Symbol STACK_CLASS_SYM = Symbol.intern("Stack");
    final static private Symbol STACK_CLASS_EX_SYM = Symbol.intern("Stack$StackUnderflowException");
    final static private Symbol CLOJURE = Symbol.intern("clojure.core");
    final static private Symbol GERSHWIN = Symbol.intern("gershwin.core");
    final static private Namespace GERSHWIN_NS = Namespace.findOrCreate(GERSHWIN);

    public static Charset UTF8 = Charset.forName("UTF-8");
    public final static Keyword STACK_VOID = Keyword.intern("gershwin.core", "stack-void");

    public static String formatGershwinName(String name) {
        return name + GERSHWIN_SUFFIX;
    }

    public static void loadResourceScript(String name) throws IOException {
	loadResourceScript(name, true);
    }

    public static void loadResourceScript(String name, boolean failIfNotFound) throws IOException {
	loadResourceScript(RT.class, name, failIfNotFound);
    }

    public static void loadResourceScript(Class c, String name) throws IOException {
	loadResourceScript(c, name, true);
    }

    public static void loadResourceScript(Class c, String name, boolean failIfNotFound) throws IOException {
	int slash = name.lastIndexOf('/');
	String file = slash >= 0 ? name.substring(slash + 1) : name;
	InputStream ins = clojure.lang.RT.resourceAsStream(baseLoader(), name);
	if(ins != null) {
            try {
                Compiler.load(new InputStreamReader(ins, UTF8), name, file);
            }
            finally {
                ins.close();
            }
	}
	else if(failIfNotFound) {
            throw new FileNotFoundException("Could not locate Gershwin resource on classpath: " + name);
	}
    }

    static public void load(String scriptbase) throws IOException, ClassNotFoundException {
	load(scriptbase, true);
    }

    static public void load(String scriptbase, boolean failIfNotFound) throws IOException, ClassNotFoundException {
	String classfile = scriptbase + clojure.lang.RT.LOADER_SUFFIX + ".class";
	// String cljfile = scriptbase + ".clj";
        String gwnfile = scriptbase + ".gwn";
	URL classURL = clojure.lang.RT.getResource(baseLoader(),classfile);
	// URL cljURL = clojure.lang.RT.getResource(baseLoader(), cljfile);
        URL gwnURL = clojure.lang.RT.getResource(baseLoader(), gwnfile);
	boolean loaded = false;

	if((classURL != null &&
	    (gwnURL == null
	     || clojure.lang.RT.lastModified(classURL, classfile) > clojure.lang.RT.lastModified(gwnURL, gwnfile)))
	   || classURL == null) {
            loaded = (loadClassForName(scriptbase.replace('/', '.') +
                                       clojure.lang.RT.LOADER_SUFFIX) != null);
	}
	if(!loaded && gwnURL != null) {
            // When Gershwin supports AOT compilation:
            // if(clojure.lang.RT.booleanCast(clojure.lang.Compiler.COMPILE_FILES.deref()))
            //     compile(gwnfile);
            // else
            loadResourceScript(clojure.lang.RT.class, gwnfile);
	}
	else if(!loaded && failIfNotFound)
            throw new FileNotFoundException(String.format("Could not locate %s or %s on classpath: ", classfile, gwnfile));
    }

    static public Class loadClassForName(String name) throws ClassNotFoundException {
	try {
            Class.forName(name, false, baseLoader());
        }
	catch(ClassNotFoundException e) {
            // This means the source file needs to be compiled.
            return null;
        }
	return Class.forName(name, true, baseLoader());
    }

    public static void doInit() throws ClassNotFoundException, IOException {
        // clojure.lang.Compiler.eval(RT.list(IN_NS, GERSHWIN));
        // IN_NS.invoke(GERSHWIN);
        // GERSHWIN_NS.importClass(STACK_CLASS_SYM, Stack.class);
        // GERSHWIN_NS.importClass(STACK_CLASS_EX_SYM, Stack.StackUnderflowException.class);
        // REFER.invoke(CLOJURE);
        load("gershwin/core");
        // Here Clojure also tries to load a user.clj if it's available.

        //
        // Example of creating a Word programmatically.
        //
        // Clojure includes this for some reason in its RT.java, but then
        // calls Compiler.loadFile directly in src/clj/clojure/main.clj.
        // So I'm leaving it here for posterity, but I don't know why it's there.
        // Perhaps just a relic of early development that was never cleaned out.
        // It's still convenient as an alternative to getting things on or setting up
        // the classpath just to load code.
	Var.intern(GERSHWIN_NS, LOAD_FILE,
                   new Word() {
                       public Object invoke() {
                           try {
                               return Compiler.loadFile((String) Stack.popIt());
                           }
                           catch(IOException e) {
                               throw Util.sneakyThrow(e);
                           }
                       }
                   });

    }

    public static void print(Object x, Writer w) throws IOException {
        if(x instanceof Quotation) {
            Quotation q = (Quotation) x;
            w.write("< ");
            printInnerList(q.getQuotationForms(), w);
            w.write(" >");
        } else if(x instanceof QuotationList) {
            w.write("< ");
            printInnerList((List) x, w);
            w.write(" >");
        } else {
            clojure.lang.RT.print(x, w);
            w.flush();
        }
    }

    private static void printInnerList(List x, Writer w) throws IOException {
        Iterator iter = x.iterator();
        while (iter.hasNext()) {
            print(iter.next(), w);
            if(iter.hasNext())
                w.write(' ');
        }
    }
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
