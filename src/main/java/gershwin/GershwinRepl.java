package gershwin;

import gershwin.lang.Compiler;
import gershwin.lang.LineNumberingPushbackReader;
import gershwin.lang.Parser;
import gershwin.lang.RT;
import gershwin.lang.Stack;

import clojure.lang.ISeq;
import clojure.lang.Keyword;
import clojure.lang.LazySeq;
import clojure.lang.LispReader;
import clojure.lang.Namespace;
import clojure.lang.SeqEnumeration;

import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Old-school debug REPL taken from commented-out Clojure main in LispReader
 */
public class GershwinRepl {
    private static String formatLog(String msg) {
        Namespace ns = (Namespace) clojure.lang.RT.CURRENT_NS.deref();
        return String.format("[%s] %s", ns.toString(), msg);
    }

    private static String formatPrompt() {
        Namespace ns = (Namespace) clojure.lang.RT.CURRENT_NS.deref();
        return ns.toString() + "> ";
    }

    private static void handleEval(Object form, Writer w) throws IOException {
        if(form instanceof gershwin.lang.Word) {
            w.write("ok -- word defined\n");
            w.flush();
        }
    }

    private static void checkIfExit(Object form, Writer w) throws IOException {
        Keyword exitKw = Keyword.intern("gershwin.core", "exit");
        Keyword quitKw = Keyword.intern("gershwin.core", "quit");
        if(form instanceof Keyword
           && (form.equals(exitKw) || form.equals(quitKw))) {
            w.write("\n");
            w.write(formatLog("Exiting Gershwin REPL."));
            w.write("\n\n");
            w.flush();
            System.exit(0);
        }
    }

    public static void main(String[] args) throws ClassNotFoundException, IOException {
	LineNumberingPushbackReader r = new LineNumberingPushbackReader(new InputStreamReader(System.in));
	OutputStreamWriter w = new OutputStreamWriter(System.out);
	Object readRet = null;
        Object evalRet = null;
        RT.doInit();
        boolean firstPass = true;
	try {
            for(; ;) {
                if(firstPass) {
                    w.write(formatPrompt());
                    w.flush();
                    firstPass = false;
                }
                int ch = LispReader.read1(r);
                if (ch == 10) {
                    if(!firstPass) {
                        w.write("\n--- Data Stack:\n");
                        for (ISeq s = Stack.seq(); s != null; s = s.next()) {
                            Object item = s.first();
                            // At the REPL, do not immediately realize lazy seq's
                            if(item instanceof LazySeq) {
                                w.write("(...LazySeq...)");
                            } else {
                                RT.print(item, w);
                            }
                            w.write('\n');
                        }
                        // w.write('\n');
                        w.flush();
                    }
                    w.write(formatPrompt());
                    w.flush();
                } else {
                    r.unread(ch);
                }
                readRet = Parser.read(r, true, null, false);
                checkIfExit(readRet, w);
                evalRet = Compiler.eval(readRet);
                handleEval(evalRet, w);
            }
        }
	catch(Exception e) {
            e.printStackTrace();
        }
    }
}
