package gershwin;

import gershwin.lang.Compiler;
import gershwin.lang.LineNumberingPushbackReader;
import gershwin.lang.Parser;
import gershwin.lang.RT;
import gershwin.lang.Stack;

import clojure.lang.LispReader;
import clojure.lang.SeqEnumeration;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Old-school debug REPL taken from commented-out Clojure main in LispReader
 */
public class GershwinRepl {
    // @todo This should be something like "user>" with gershwin.core having been
    //   referred in.
    private static final String REPL_PROMPT = "gershwin> ";

    public static void main(String[] args) {
	LineNumberingPushbackReader r = new LineNumberingPushbackReader(new InputStreamReader(System.in), 2);
	OutputStreamWriter w = new OutputStreamWriter(System.out);
	Object ret = null;
        // @todo Work on how Stack operations themselves affect the stack (e.g., seq())
        RT.doInit();
        boolean firstPass = true;
	try {
            for(; ;) {
                if(firstPass) {
                    w.write(REPL_PROMPT);
                    w.flush();
                    firstPass = false;
                }
                int ch = LispReader.read1(r);
                if (ch == 10) {
                    if(!firstPass) {
                        w.write("\n--- Data Stack:\n");
                        SeqEnumeration iter = new SeqEnumeration(Stack.seq());
                        while (iter.hasMoreElements()) {
                            clojure.lang.RT.print(iter.nextElement(), w);
                            // w.write(iter.nextElement().toString());
                            w.write('\n');
                        }
                        // w.write(Stack.seq().toString());
                        w.write('\n');
                        w.flush();
                    }
                    w.write(REPL_PROMPT);
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
