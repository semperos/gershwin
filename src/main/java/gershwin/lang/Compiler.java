package gershwin.lang;

import clojure.lang.Fn;
import clojure.lang.IFn;
import clojure.lang.IObj;
import clojure.lang.IPersistentCollection;
import clojure.lang.ISeq;
import clojure.lang.Keyword;
import clojure.lang.LispReader;
import clojure.lang.Namespace;
import clojure.lang.PersistentList;
import clojure.lang.PersistentVector;
import clojure.lang.Symbol;
import clojure.lang.Var;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;

public class Compiler {
    static final String GERSHWIN_VAR_PREFIX = "__GLS__";
    static final Symbol DEF = Symbol.intern("def");
    static final Symbol FN = Symbol.intern("fn");

    interface Expr {
	Object eval() ;

	// void emit(C context, ObjExpr objx, GeneratorAdapter gen);

	// boolean hasJavaClass() ;

	// Class getJavaClass() ;
    }

    /**
     * Let Clojure do its thing.
     *
     * @todo Make private
     */
    public static class ClojureExpr implements Expr {
        final Object x;

        public ClojureExpr(Object x) {
            this.x = x;
        }

        /**
         * Mimicking Clojure's literal exprs
         */
	Object val() {
            return x;
        }

	public Object eval() {
            Object clojureForm = clojure.lang.Compiler.eval(val(), false);
            // System.out.println("Clojure Evaluated: " + clojureForm.getClass().getName() + ", " + clojureForm);
            if(!clojureForm.equals(RT.STACK_VOID))
                Stack.conjMutable(clojureForm);
            return clojureForm;
	}

        // public static Expr parse(Object form) {
        //     if(form instanceof Number) {
        //         return new ClojureExpr(form);
        //     }
        // }
    }

    public static class FnExpr implements Expr {
        final ISeq fnForm;

        public FnExpr(ISeq fnForm) {
            this.fnForm = fnForm;
        }

        /**
         * Clojure functions are evaluated as soon as they are
         * encountered and the return value is put on the stack.
         *
         * Unless I'm crazy, this is the right way to go, and there will
         * need to be a separate idea of a "quotation" that acts as an
         * evaluation-delayer, so that Gershwin controls the semantics of
         * of that eventual evaluation, and not Clojure itself.
         */
        public Object eval() {
            IFn clojureForm = (IFn) clojure.lang.Compiler.eval(this.fnForm, false);
            return clojureForm.invoke();
        }
    }

    /**
     * Word-creation expr
     *
     * @todo Make private
     */
    public static class ColonExpr implements Expr {
        final IColonList l;

        public ColonExpr(IColonList l) {
            this.l = l;
        }

        /**
         * Add a word definition to the current Clojure namespace
         * as a {@link clojure.lang.Var}. Words are instances of
         * {@link Word}.
         */
        public Object eval() {
            // System.out.println("COLON: " + l.getClass().getName() + ", " + (l instanceof ArrayList) + ", " + l);
            Symbol nameSym = (Symbol) this.l.get(0);
            Symbol gershwinName = Symbol.intern(GERSHWIN_VAR_PREFIX + nameSym.getName());
            // System.out.println("COLON NAME IS: " + name);
            IPersistentCollection stackEffect = (IPersistentCollection) this.l.get(1);
            // System.out.println("COLON STACK EFFECT IS: " + stackEffect);
            List definition = this.l.subList(2, l.size());
            // System.out.println("COLON DEFINITION IS: " + definition);
            Word word = new Word(stackEffect, definition);
            createVar(gershwinName, word);
            return word;
        }
    }

    /**
     * Word expr
     *
     * @todo Make private
     */
    public static class WordExpr implements Expr {
        final Word word;

        public WordExpr(Word word) {
            this.word = word;
        }

        /**
         * @todo Reconsider returning values for eval, since
         *   things happen on the stack.
         */
        public Object eval() {
            // System.out.println("Eval'ing a Word");
            List wordDefinition = word.getDefinition();
            Object ret = null;
            Iterator iter = wordDefinition.iterator();
            while (iter.hasNext()) {
                ret = Compiler.eval(iter.next());
            }
            return ret;
        }
    }

    /**
     * Deal with a single language form.
     *
     * Currently uses Clojure to evaluate form. This eval
     */
    public static Object eval(Object form) {
        // System.out.println("Gershwin Eval, raw form: " +
        //                    form.getClass().getName() +
        //                    ", " + form);
        // Object clojureForm = clojure.lang.Compiler.eval(form, false);
        Expr expr = analyze(form);
        return expr.eval();
    }

    // @todo Make private
    public static Expr analyze(Object form) {
        // System.out.println("ANALYZE FORM: " + form.getClass().getName() + ", " + form);
        if(form instanceof IColonList) {
            // System.out.println("GERSHWIN Word Definition: " + form);
            return analyzeColon((IColonList) form);
        } else if(form instanceof Symbol) {
            Symbol formSym = (Symbol) form;
            String maybeVarName = formSym.getName();
            Namespace currentClojureNs = (Namespace) clojure.lang.RT.CURRENT_NS.deref();
            Object maybeVar = currentClojureNs.findInternedVar(Symbol.intern(GERSHWIN_VAR_PREFIX + maybeVarName));
            if(maybeVar != null && maybeVar instanceof Var) {
                Var aVar = (Var) maybeVar;
                if(aVar.isBound() && aVar.deref() instanceof Word) {
                    // System.out.println("You tried to use a Gershwin word definition!");
                    return analyzeWord((Word) aVar.deref());
                } else {
                    return new ClojureExpr(form);
                }
            } else {
                return new ClojureExpr(form);
            }
        } else if(form instanceof ISeq && clojure.lang.RT.first(form).equals(FN)) {
            return new FnExpr((ISeq) form);
        } else {
            return new ClojureExpr(form);
        }
    }

    /**
     * Here is where Clojure's analyze method would inspect the seq for a `def`
     * and ensure it has a vector of arguments, optional docstring, etc.
     * and ensure it's formed sanely, throwing an error about
     * too many/too few args to `def` if not.
     */
    public static Expr analyzeColon(IColonList form) {
        if (form.size() < 3) {
            throw clojure.lang.Util.runtimeException("Too few arguments to ':'. You must include:\n\t(1) The name of the word\n\t(2) The intended stack effect of the word\n\t(3) The word definition.\n");
        } else if(!(form.get(0) instanceof Symbol)) {
            throw clojure.lang.Util.runtimeException("First argument to ':' must be a Symbol");
        }
        return new ColonExpr(form);
    }

    /**
     * Do something with an existing Gershwin word definition.
     */
    public static Expr analyzeWord(Word word) {
        // System.out.println("ANALYZE WORD: " + word.getStackEffect() + ", " + word.getDefinition());
        return new WordExpr(word);
    }

    // Try loading: (Compiler/load (java.io.StringReader. \"(fn [] (+ (Stack/popIt) (Stack/popIt)))\"))

    public static Object load(Reader rdr) {
	return load(rdr, null, "NO_SOURCE_FILE");
    }

    public static Object load(Reader rdr, String sourcePath, String sourceName) {
	Object EOF = new Object();
	Object ret = null;
	LineNumberingPushbackReader pushbackReader =
            (rdr instanceof LineNumberingPushbackReader) ? (LineNumberingPushbackReader) rdr :
            new LineNumberingPushbackReader(rdr);
	try {
            for(Object r = Parser.read(pushbackReader, false, EOF, false);
                r != EOF;
                r = Parser.read(pushbackReader, false, EOF, false)) {
                // LINE_AFTER.set(pushbackReader.getLineNumber());
                // COLUMN_AFTER.set(pushbackReader.getColumnNumber());
                ret = eval(r);
                // LINE_BEFORE.set(pushbackReader.getLineNumber());
                // COLUMN_BEFORE.set(pushbackReader.getColumnNumber());a

            }
        }
	catch(Parser.ReaderException e) {
            throw new clojure.lang.Compiler.CompilerException(sourcePath, e.line, e.column, e.getCause());
        }
	return ret;
    }

    public static Object loadFile(String file) throws IOException {
	FileInputStream f = new FileInputStream(file);
	try {
            return load(new InputStreamReader(f, clojure.lang.RT.UTF8), new File(file).getAbsolutePath(), (new File(file)).getName());
        }
	finally {
            f.close();
        }
    }

    /**
     * Create a Clojure {@link clojure.lang.Var} and bind it
     * to {@code form}.
     */
    public static void createVar(Symbol name, Object form) {
        IObj newVar = (IObj) clojure.lang.RT.list(DEF, name, form);
        clojure.lang.Compiler.eval(newVar, false);
        // ArrayList varParts = new ArrayList();
        // // Create fake var to prove concept
        // // See Nakkaya as reference, the definition of ":"
        // // replaces the word definition with a Clojure function that
        // // doseq's its way through the words in a new word definition,
        // // eval'ing them alon the way.
        // //
        // // clojure.lang.RT.list(FN, PersistentVector.EMPTY, l)
        // // for(; i < l.size(); i++) {
        // //     Object aForm = l.get(i);
        // //     varParts.add(eval())
        // // }
        // clojure.lang.RT.list(DEF, name, clojure.lang.RT.list(FN, PersistentVector.EMPTY, l));
        // varParts.add(DEF);
        // varParts.add(name);
        // varParts.add(form);
        // IObj list = (IObj) PersistentList.create(varParts);
        // Object clojureForm = clojure.lang.Compiler.eval(list, false);
    }

    public static Object resolveClojure(Symbol sym) {
        // Namespace, symbol, allowPrivate
        return clojure.lang.Compiler.resolveIn((Namespace) clojure.lang.RT.CURRENT_NS.deref(), sym, false);
    }
}
