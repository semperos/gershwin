package gershwin.lang;

import clojure.lang.IFn;
import clojure.lang.Keyword;
import clojure.lang.Namespace;
import clojure.lang.Symbol;
import clojure.lang.Var;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

public class RT {
    final static private String GERSHWIN_PREFIX = "__GWN__";
    final static Symbol LOAD_FILE = Symbol.intern(GERSHWIN_PREFIX + "load-file");
    final static private IFn IN_NS = ClojureApi.var("clojure.core", "in-ns");
    final static private IFn REFER = ClojureApi.var("clojure.core", "refer");
    final static private Symbol STACK_CLASS_SYM = Symbol.intern("Stack");
    final static private Symbol CLOJURE = Symbol.intern("clojure.core");
    final static private Symbol GERSHWIN = Symbol.intern("gershwin.core");
    final static private Namespace GERSHWIN_NS = Namespace.findOrCreate(GERSHWIN);

    public static Charset UTF8 = Charset.forName("UTF-8");
    public final static Keyword STACK_VOID = Keyword.intern("gershwin.core", "stack-void");

    public static void doInit() {
        // clojure.lang.Compiler.eval(RT.list(IN_NS, GERSHWIN));
        IN_NS.invoke(GERSHWIN);
        GERSHWIN_NS.importClass(STACK_CLASS_SYM, gershwin.lang.Stack.class);
        REFER.invoke(CLOJURE);
        // Clojure defines this in RT.java. Not entirely sure why,
        // perhaps just to avoid a call to the language's compiler
        // from the language itself.
	Var.intern(GERSHWIN_NS, LOAD_FILE,
                   new Word() {
                       public Object invoke() {
                           try {
                               return Compiler.loadFile((String) Stack.popIt());
                           }
                           catch(IOException e) {
                               throw clojure.lang.Util.sneakyThrow(e);
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
        } else {
            clojure.lang.RT.print(x, w);
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
static public void print(Object x, Writer w) throws IOException{
		if(x == null)
			w.write("nil");
		else if(x instanceof ISeq || x instanceof IPersistentList) {
			w.write('(');
			printInnerSeq(seq(x), w);
			w.write(')');
		}
		else if(x instanceof IPersistentMap) {
			w.write('{');
			for(ISeq s = seq(x); s != null; s = s.next()) {
				IMapEntry e = (IMapEntry) s.first();
				print(e.key(), w);
				w.write(' ');
				print(e.val(), w);
				if(s.next() != null)
					w.write(", ");
			}
			w.write('}');
		}
		else if(x instanceof IPersistentVector) {
			IPersistentVector a = (IPersistentVector) x;
			w.write('[');
			for(int i = 0; i < a.count(); i++) {
				print(a.nth(i), w);
				if(i < a.count() - 1)
					w.write(' ');
			}
			w.write(']');
		}
		else if(x instanceof IPersistentSet) {
			w.write("#{");
			for(ISeq s = seq(x); s != null; s = s.next()) {
				print(s.first(), w);
				if(s.next() != null)
					w.write(" ");
			}
			w.write('}');
		}
		else if(x instanceof Character) {
			char c = ((Character) x).charValue();
			if(!readably)
				w.write(c);
			else {
				w.write('\\');
				switch(c) {
					case '\n':
						w.write("newline");
						break;
					case '\t':
						w.write("tab");
						break;
					case ' ':
						w.write("space");
						break;
					case '\b':
						w.write("backspace");
						break;
					case '\f':
						w.write("formfeed");
						break;
					case '\r':
						w.write("return");
						break;
					default:
						w.write(c);
				}
			}
		}
		else if(x instanceof Class) {
			w.write("#=");
			w.write(((Class) x).getName());
		}
		else if(x instanceof BigDecimal && readably) {
			w.write(x.toString());
			w.write('M');
		}
		else if(x instanceof BigInt && readably) {
			w.write(x.toString());
			w.write('N');
		}
		else if(x instanceof BigInteger && readably) {
			w.write(x.toString());
			w.write("BIGINT");
		}
		else if(x instanceof Var) {
			Var v = (Var) x;
			w.write("#=(var " + v.ns.name + "/" + v.sym + ")");
		}
		else if(x instanceof Pattern) {
			Pattern p = (Pattern) x;
			w.write("#\"" + p.pattern() + "\"");
		}
		else w.write(x.toString());
	}

}
 **/

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
