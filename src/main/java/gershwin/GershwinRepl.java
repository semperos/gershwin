package gershwin;

import gershwin.lang.Compiler;
import gershwin.lang.LineNumberingPushbackReader;
import gershwin.lang.Parser;
import gershwin.lang.RT;
import gershwin.lang.Stack;

import clojure.lang.ISeq;
import clojure.lang.LispReader;
import clojure.lang.Namespace;
import clojure.lang.SeqEnumeration;

import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Old-school debug REPL taken from commented-out Clojure main in LispReader
 */
public class GershwinRepl {
    private static String formatPrompt() {
        Namespace ns = (Namespace) clojure.lang.RT.CURRENT_NS.deref();
        return ns.toString() + "> ";
    }

    public static void main(String[] args) throws ClassNotFoundException, IOException {
	LineNumberingPushbackReader r = new LineNumberingPushbackReader(new InputStreamReader(System.in), 2);
	OutputStreamWriter w = new OutputStreamWriter(System.out);
	Object ret = null;
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
                            RT.print(s.first(), w);
                            w.write('\n');
                        }
                        w.write('\n');
                        w.flush();
                    }
                    w.write(formatPrompt());
                    w.flush();
                } else {
                    r.unread(ch);
                }
                ret = Parser.read(r, true, null, false);
                Compiler.eval(ret);
            }
        }
	catch(Exception e) {
            e.printStackTrace();
        }
    }
}
