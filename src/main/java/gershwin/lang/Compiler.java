package gershwin.lang;

import clojure.lang.Fn;
import clojure.lang.IFn;
import clojure.lang.IObj;
import clojure.lang.IPersistentCollection;
import clojure.lang.IPersistentMap;
import clojure.lang.IPersistentStack;
import clojure.lang.PersistentHashMap;
import clojure.lang.ISeq;
import clojure.lang.Keyword;
import clojure.lang.LispReader;
import clojure.lang.Namespace;
import clojure.lang.PersistentList;
import clojure.lang.PersistentVector;
import clojure.lang.Symbol;
import clojure.lang.Var;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;

public class Compiler {
    static final String GERSHWIN_VAR_PREFIX = "__GWN__";
    static final Keyword DOC_KEY = Keyword.intern(null, "doc");
    static final Keyword STACK_EFFECT_KEY = Keyword.intern(null, "stack-effect");
    static final Symbol DEF = Symbol.intern("def");
    static final Symbol FN = Symbol.intern("fn");
    static final Symbol IF = Symbol.intern("if*");

    static final public IPersistentMap specials =
        PersistentHashMap
        .create(IF, new IfExpr.Parser());

    /**
     * Instead of having separate top-level {@code analyzeFoo}
     * methods for every possible language form,
     * {@code Expr} classes can contain an implemention
     * of this interface to handle analysis of a specific
     * language form.
     *
     * In this way, the implementation of parse becomes
     * something of a "factory" method, handling the particulars
     * of instantiating a particular kind of {@code Expr}.
     */
    interface IParser{
	// Expr parse(C context, Object form) ;
        Expr parse(Object form);
    }

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
            if(clojureForm == null) {
                Stack.conjMutable(clojureForm);
            } else if(!clojureForm.equals(RT.STACK_VOID)) {
                Stack.conjMutable(clojureForm);
            }
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
        final IGershwinList l;

        public ColonExpr(IGershwinList l) {
            this.l = l;
        }

        /**
         * Add a word definition to the current Clojure namespace
         * as a {@link clojure.lang.Var}. Words are instances of
         * {@link Word}.
         */
        public Object eval() {
            Symbol nameSym = (Symbol) this.l.get(0);
            Symbol gershwinName = Symbol.intern(GERSHWIN_VAR_PREFIX + nameSym.getName());
            IPersistentMap wordMeta = null;
            String docString = null;
            if (this.l.get(1) instanceof IPersistentMap) {
                // Shortcut to add metadata
                wordMeta = (IPersistentMap) this.l.get(1);
                this.l.remove(1);
            } else if (this.l.get(1) instanceof String) {
                // Shortcut to add a docstring
                docString = (String) this.l.get(1);
                this.l.remove(1);
            }
            IPersistentCollection stackEffect = (IPersistentCollection) this.l.get(1);
            List definition = this.l.subList(2, l.size());
            Word word = new Word(stackEffect, definition);
            if(wordMeta != null) {
                createVar(gershwinName, word, wordMeta.assoc(STACK_EFFECT_KEY, stackEffect));
            } else if(docString != null) {
                createVar(gershwinName, word, docString, clojure.lang.RT.map(STACK_EFFECT_KEY, stackEffect));
            } else {
                createVar(gershwinName, word, clojure.lang.RT.map());
            }
            return word;
        }
    }

    /**
     * Quotation expr
     *
     * @todo Make private
     */
    public static class QuotationExpr implements Expr {
        final IGershwinList l;

        public QuotationExpr(IGershwinList l) {
            this.l = l;
        }

        public Object eval() {
            Quotation quotation = new Quotation(l);
            Stack.conjMutable(quotation);
            return quotation;
        }
    }

    /**
     * Word expr. This is the {@code Expr} used to handle a word
     * when it is entered for evaluation. See {@link ColonExpr} for
     * the case of defining a new word.
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
            return word.invoke();
        }
    }

    public static class IfExpr implements Expr {
        final Object condition;
        final Quotation thenQuotation;
        final Quotation elseQuotation;

        public IfExpr(Object condition, Quotation thenQuotation, Quotation elseQuotation) {
            this.condition = condition;
            this.thenQuotation = thenQuotation;
            this.elseQuotation = elseQuotation;
        }

        public Object eval() {
            if(condition != null && condition != Boolean.FALSE)
                return thenQuotation.invoke();
            return elseQuotation.invoke();
        }

        static class Parser implements IParser {
            /**
             * Unlike in Clojure, these forms have already been
             * analyzed and are on the stack in evaluated form, so there's
             * no need to analyze these forms and create expr's again.
             */
            public Expr parse(Object form) {
                IPersistentStack sCdr = Stack.pop();
                IPersistentStack sCddr = sCdr.pop();
                Object condition = sCddr.peek();
                Object thenQ = sCdr.peek();
                Object elseQ = Stack.peek();
                if(!(thenQ instanceof Quotation)) {
                    throw Util.runtimeException("The 'then' branch of an if expression must be a quotation.");
                } else if(!(elseQ instanceof Quotation)) {
                    throw Util.runtimeException("The 'else' branch of an if expression must be a quotation.");
                }
                // Easier to use immutable methods above and then just
                // pop everything off the stack in one go.
                for(int i = 0; i < 3; i++) { Stack.popIt(); }
                return new IfExpr(condition, (Quotation) thenQ, (Quotation) elseQ);
            }
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
        IParser p;
        // System.out.println("ANALYZE FORM: " + form.getClass().getName() + ", " + form);
        // @todo Make interfaces for these if appropriate and use them for dispatch
        if(form instanceof ColonList) {
            // System.out.println("GERSHWIN Word Definition: " + form);
            return analyzeColon((ColonList) form);
        } else if(form instanceof QuotationList) {
            // System.out.println("GERSHWIN Quotation: " + form);
            return analyzeQuotation((QuotationList) form);
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
            } else if((p = (IParser) specials.valAt(form)) != null) {
                // return p.parse(context, form);
                return p.parse(form);
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
    public static Expr analyzeColon(ColonList form) {
        if (form.size() < 3) {
            throw Util.runtimeException("Too few arguments to ':'. At a minimum, you must include:\n\t(1) The name of the word\n\t(2) The intended stack effect of the word\n\t(3) The word definition.\n");
        } else if(!(form.get(0) instanceof Symbol)) {
            throw Util.runtimeException("First argument to ':' must be a Symbol");
        }
        return new ColonExpr(form);
    }

    public static Expr analyzeQuotation(QuotationList form) {
        return new QuotationExpr(form);
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
            throw new CompilerException(sourcePath, e.line, e.column, e.getCause());
        }
	return ret;
    }

    public static Object loadFile(String file) throws IOException {
	FileInputStream f = new FileInputStream(file);
	try {
            return load(new InputStreamReader(f, RT.UTF8), new File(file).getAbsolutePath(), (new File(file)).getName());
        }
	finally {
            f.close();
        }
    }

    /**
     * If meta-data is passed in, we assume form is an IObj, which allows
     * adding meta-data to itself.
     *
     * For now, going to put meta-data on the {@link clojure.lang.Var}, since that
     * seems to be the trend, though I confess not understanding all the ramifications.
     * Will leave commented-out line for adding it directly to a custom
     * {@link clojure.lang.IObj} like {@link Word}
     */
    public static void createVar(Symbol name, IObj form, IPersistentMap formMeta) {
        // IObj formWithMeta = form.withMeta(formMeta);
        IObj varForm = (IObj) clojure.lang.RT.list(DEF, name, form);
        Var newVar = (Var) clojure.lang.Compiler.eval(varForm, false);
        if(formMeta != null) {
            newVar.setMeta(formMeta);
        }
    }

    /**
     * Create a Clojure {@link clojure.lang.Var} and bind it
     * to {@code form}.
     */
    public static void createVar(Symbol name, Object form, String docString, IPersistentMap formMeta) {
        IObj varForm = (IObj) clojure.lang.RT.list(DEF, name, form);
        Var newVar = (Var) clojure.lang.Compiler.eval(varForm, false);
        if(docString != null) {
            newVar.setMeta(formMeta.assoc(DOC_KEY, docString));
        }
    }

    public static class CompilerException extends RuntimeException {
	final public String source;

	final public int line;

	public CompilerException(String source, int line, int column, Throwable cause) {
            super(errorMsg(source, line, column, cause.toString()), cause);
            this.source = source;
            this.line = line;
	}

	public String toString(){
            return getMessage();
	}
    }

    static String errorMsg(String source, int line, int column, String s) {
	return String.format("%s, compiling:(%s:%d:%d)", s, source, line, column);
    }
}
