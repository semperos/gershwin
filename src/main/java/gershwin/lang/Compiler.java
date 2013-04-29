package gershwin.lang;

import clojure.lang.Fn;
import clojure.lang.IFn;
import clojure.lang.IMapEntry;
import clojure.lang.IObj;
import clojure.lang.IPersistentCollection;
import clojure.lang.IPersistentList;
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
import static clojure.lang.RT.cons;
import static clojure.lang.RT.conj;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;

public class Compiler {
    static final String GERSHWIN_VAR_SUFFIX = "__GWN__";
    static final Keyword DOC_KEY = Keyword.intern(null, "doc");
    static final Keyword STACK_EFFECT_KEY = Keyword.intern(null, "stack-effect");
    static final Symbol DEF = Symbol.intern("def");
    static final Symbol FN = Symbol.intern("fn");
    static final Symbol LIST = Symbol.intern("list");
    static final Symbol QUOTE = Symbol.intern("quote");
    static final Symbol DO = Symbol.intern("do");
    static final Symbol DOT = Symbol.intern(".");
    // @todo Perhaps should be namespaced
    static final Keyword WORD_KW = Keyword.intern(null, "word");
    // static final Symbol IF = Symbol.intern("if*");
    // static final public Var COMPILE_PATH = Var.intern(Namespace.findOrCreate(Symbol.intern("gershwin.core")),
    //                                                   Symbol.intern("*gershwin-compile-path*"), null).setDynamic();

    // static final public IPersistentMap specials =
    //     PersistentHashMap
    //     .create(IF, new IfExpr.Parser());

    /**
     * Simple compilation to a Clojure function.
     */
    public static Object compileDefinition(List rawForms) {
        IPersistentCollection definitionForms = PersistentVector.EMPTY;
        for(int i = 0; i < rawForms.size(); i++) {
            Object rawForm = rawForms.get(i);
            if(rawForm instanceof Symbol) {
                Expr expr = analyzeSymbol((Symbol) rawForm);
                if(expr instanceof WordExpr) {
                    Word word = ((WordExpr) expr).getWord();
                    // Clojure turtles all the way down.
                    definitionForms = conj(definitionForms, clojure.lang.RT.list(word.getDefinitionFn()));
                } else {
                    ISeq form = withConjIt(rawForm);
                    definitionForms = conj(definitionForms, form);
                }
            } else if(rawForm instanceof QuotationList) {
                QuotationExpr quotExpr = (QuotationExpr) analyzeQuotation((QuotationList) rawForm);
                Quotation quot = quotExpr.getQuotation();
                // Clojure turtles all the way down, again.
                // @todo Simplistic alternative would be to construct
                //   an IPersistentList of (Quotation. (fn [] ...)). Write now this
                //   function has invoke() called, which just happens to work since
                //   quotations are always zero-arity functions.
                ISeq form = withConjIt(quot.getDefinitionFn());
                definitionForms = conj(definitionForms, form);
            } else {
                ISeq form = withConjIt(rawForm);
                definitionForms = conj(definitionForms, form);
            }
        }
        return cons(FN, cons(PersistentVector.EMPTY, clojure.lang.RT.seq(definitionForms)));
    }

    public static Object emitDefinition(List rawForms) {
        IPersistentCollection definitionForms = PersistentVector.EMPTY;
        for(int i = 0; i < rawForms.size(); i++) {
            Object rawForm = rawForms.get(i);
            if(rawForm instanceof Symbol) {
                Expr expr = analyzeSymbol((Symbol) rawForm);
                if(expr instanceof WordExpr) {
                    Word word = ((WordExpr) expr).getWord();
                    // Clojure turtles all the way down.
                    definitionForms = conj(definitionForms, word.getDefinitionForm());
                } else {
                    ISeq form = withConjIt(rawForm);
                    definitionForms = conj(definitionForms, form);
                }
            } else if(rawForm instanceof QuotationList) {
                QuotationExpr quotExpr = (QuotationExpr) analyzeQuotation((QuotationList) rawForm);
                Quotation quot = quotExpr.getQuotation();
                // Clojure turtles all the way down, again.
                ISeq form = withConjIt(quot.getDefinitionForm());
                definitionForms = conj(definitionForms, form);
            } else {
                ISeq form = withConjIt(rawForm);
                definitionForms = conj(definitionForms, form);
            }
        }
        return cons(FN, cons(PersistentVector.EMPTY, clojure.lang.RT.seq(definitionForms)));
    }

    public static Object compile(Reader rdr, String sourcePath, String sourceName) throws IOException {
        Object EOF = new Object();
        Object ret = null;
        LineNumberingPushbackReader pushbackReader =
            (rdr instanceof LineNumberingPushbackReader) ? (LineNumberingPushbackReader) rdr :
            new LineNumberingPushbackReader(rdr);
        List<String> lines = new ArrayList<String>();
        String internalName = sourcePath
            .replace(File.separator, "/")
            .substring(0, sourcePath.lastIndexOf('.'));
        try {
            for(Object r = Parser.read(pushbackReader, false, EOF, false); r != EOF;
                r = Parser.read(pushbackReader, false, EOF, false)) {
                compile1(lines, r);
            }
            writeClojureFile(internalName, lines);
        } catch(Parser.ReaderException e) {
            throw new CompilerException(sourcePath, e.line, e.column, e.getCause());
        }
        return ret;
    }

    static void compile1(List<String> lines, Object form) {
        Expr expr = analyze(form);
        lines.add(expr.emit());
        // Hmmm
        // expr.eval();
    }

    public static void writeClojureFile(String internalName, List<String> lines) throws IOException {
        String genPath = (String) clojure.lang.Compiler.COMPILE_PATH.deref();
        if(genPath == null)
            throw Util.runtimeException("*gershwin-compile-path* not set");
        String [] dirs = internalName.split("/");
        String p = genPath;
        for(int i = 0; i < dirs.length - 1; i++) {
            p += File.separator + dirs[i];
            (new File(p)).mkdir();
        }
        String path = genPath + File.separator + internalName + ".clj";
        File cf = new File(path);
        cf.createNewFile();
        FileWriter cfw = new FileWriter(cf);
        StringBuilder sb = new StringBuilder();
        for(String s : lines) {
            sb.append(s).append("\n");
        }
        String sourceCode = sb.toString();
        try {
            cfw.write(sourceCode);
            cfw.flush();
        } finally {
            cfw.close();
        }
    }

    /**
     * "Compile" a non-Word form by wrapping it in a call to
     * Stack.conjIt, so the return value of the given expression
     * ends up on the stack. The Stack.conjIt method has built-in
     * knowledge of :gershwin.core/stack-void and will not add it
     * to the stack.
     */
    private static ISeq withConjIt(Object rawForm) {
        return clojure.lang.RT.list(DO,
                                    cons(DOT,
                                         cons(Symbol.intern("gershwin.lang.Stack"),
                                              cons(clojure.lang.RT.list(Symbol.intern("conjIt"), rawForm),
                                                   null))),
                                    RT.STACK_VOID);
    }

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

	String emit();

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
            boolean invoked = false;
            if(clojureForm instanceof Var) {
                Var aVar = (Var) clojureForm;
                IPersistentMap metadata = aVar.meta();
                if(metadata.containsKey(WORD_KW)) {
                    IMapEntry entry = metadata.entryAt(WORD_KW);
                    if(clojure.lang.RT.booleanCast(entry.getValue())) {
                        // This is a word fn, invoke it immediately
                        invoked = true;
                        IFn fn = (IFn) aVar.deref();
                        fn.invoke();
                    }
                }
            }
            if(!invoked)
                Stack.conjIt(clojureForm);
            return clojureForm;
	}

        public String emit() {
            return this.x.toString();
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
            Symbol gershwinName = Symbol.intern(nameSym.getName() + GERSHWIN_VAR_SUFFIX);
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
            // What the reader gives us
            List rawForms = this.l.subList(2, l.size());
            // What we're going to store as the word's definition
            Object fnForm = compileDefinition(rawForms);
            IFn definition = (IFn) clojure.lang.Compiler.eval(fnForm, false);
            // Clojure source
            Object defForm = emitDefinition(rawForms);
            Word word = new Word(stackEffect, definition, defForm);
            if(wordMeta != null) {
                createVar(gershwinName, word, wordMeta.assoc(STACK_EFFECT_KEY, clojure.lang.RT.list(QUOTE, stackEffect)));
            } else if(docString != null) {
                createVar(gershwinName, word, docString, clojure.lang.RT.map(STACK_EFFECT_KEY, clojure.lang.RT.list(QUOTE, stackEffect)));
            } else {
                createVar(gershwinName, word, clojure.lang.RT.map());
            }
            return word;
        }

        public String emit() {
            Symbol nameSym = (Symbol) this.l.get(0);
            Symbol gershwinName = Symbol.intern(nameSym.getName() + GERSHWIN_VAR_SUFFIX);
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
            // What the reader gives us
            List rawForms = this.l.subList(2, l.size());
            // What we're going to store as the word's definition.
            Object fnForm = emitDefinition(rawForms);
            // @todo Attach metadata
            Object varForm = clojure.lang.RT.list(DEF, Symbol.intern("^:word"), gershwinName, fnForm);
            return varForm.toString();
            // IFn definition = (IFn) clojure.lang.Compiler.eval(fnForm, false);
            // Word word = new Word(stackEffect, definition);
            // if(wordMeta != null) {
            //     createVar(gershwinName, word, wordMeta.assoc(STACK_EFFECT_KEY, clojure.lang.RT.list(QUOTE, stackEffect)));
            // } else if(docString != null) {
            //     createVar(gershwinName, word, docString, clojure.lang.RT.map(STACK_EFFECT_KEY, clojure.lang.RT.list(QUOTE, stackEffect)));
            // } else {
            //     createVar(gershwinName, word, clojure.lang.RT.map());
            // }
            // return word;
        }
    }

    /**
     * Quotation expr
     *
     * @todo Make private
     */
    public static class QuotationExpr implements Expr {
        final QuotationList l;
        private Quotation quot;

        public Quotation getQuotation() {
            return this.quot;
        }

        public QuotationExpr(QuotationList l) {
            this.l = l;
            // What we're going to store as the word's definition
            Object fnForm = compileDefinition(l);
            IFn definition = (IFn) clojure.lang.Compiler.eval(fnForm, false);
            Object defForm = emitDefinition(l);
            this.quot = new Quotation(definition, defForm);
            // Used for print output
            this.quot.setQuotationForms(l);
        }

        public Object eval() {
            Stack.conjIt(quot);
            return quot;
        }

        public String emit() {
            return this.quot.getDefinitionForm().toString();
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

        public Word getWord() {
            return this.word;
        }

        /**
         * @todo Reconsider returning values for eval, since
         *   things happen on the stack.
         */
        public Object eval() {
            return word.invoke();
        }

        public String emit() {
            // We need to make sure that, when we encounter a word,
            // it already has its definition forms saved off in the object.
            // Look at ColonExpr's own eval/emit methods and make sure this happens.
            return word.getDefinitionForm().toString();
        }
    }

    /**
     * Here to be instructive, fuels if*. Regular if is simply
     * implemented in Clojure, since it relies on quotations.
     */
    // public static class IfExpr implements Expr {
    //     final Object condition;
    //     final Quotation thenQuotation;
    //     final Quotation elseQuotation;

    //     public IfExpr(Object condition, Quotation thenQuotation, Quotation elseQuotation) {
    //         this.condition = condition;
    //         this.thenQuotation = thenQuotation;
    //         this.elseQuotation = elseQuotation;
    //     }

    //     public Object eval() {
    //         if(condition != null && condition != Boolean.FALSE)
    //             return thenQuotation.invoke();
    //         return elseQuotation.invoke();
    //     }

    //     static class Parser implements IParser {
    //         /**
    //          * Unlike in Clojure, these forms have already been
    //          * analyzed and are on the stack in evaluated form, so there's
    //          * no need to analyze these forms and create expr's again.
    //          */
    //         public Expr parse(Object form) {
    //             IPersistentStack sCdr = Stack.pop();
    //             IPersistentStack sCddr = sCdr.pop();
    //             Object condition = sCddr.peek();
    //             Object thenQ = sCdr.peek();
    //             Object elseQ = Stack.peek();
    //             if(!(thenQ instanceof Quotation)) {
    //                 throw Util.runtimeException("The 'then' branch of an if expression must be a quotation.");
    //             } else if(!(elseQ instanceof Quotation)) {
    //                 throw Util.runtimeException("The 'else' branch of an if expression must be a quotation.");
    //             }
    //             // Easier to use immutable methods above and then just
    //             // pop everything off the stack in one go.
    //             for(int i = 0; i < 3; i++) { Stack.popIt(); }
    //             return new IfExpr(condition, (Quotation) thenQ, (Quotation) elseQ);
    //         }
    //     }
    // }

    /**
     * Deal with a single language form.
     *
     * Currently uses Clojure to evaluate form. This eval
     */
    public static Object eval(Object form) {
        Expr expr = analyze(form);
        return expr.eval();
    }

    // @todo Make private
    public static Expr analyze(Object form) {
        // @todo Make interfaces for these if appropriate and use them for dispatch
        if(form instanceof ColonList) {
            return analyzeColon((ColonList) form);
        } else if(form instanceof QuotationList) {
            return analyzeQuotation((QuotationList) form);
        } else if(form instanceof Symbol) {
            return analyzeSymbol((Symbol) form);
        } else {
            return analyzeClojure(form);
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

    public static Expr analyzeSymbol(Symbol form) {
        IParser p;
        String maybeVarName = form.toString();
        Namespace currentClojureNs = (Namespace) clojure.lang.RT.CURRENT_NS.deref();
        // Consider whether suffix should be conditionally appended
        Object maybeVar = clojure.lang.Compiler.maybeResolveIn(currentClojureNs, Symbol.intern(maybeVarName + GERSHWIN_VAR_SUFFIX));
        if(maybeVar != null && maybeVar instanceof Var) {
            Var aVar = (Var) maybeVar;
            if(aVar.isBound()) {
                if(aVar.deref() instanceof Word) {
                    return analyzeWord((Word) aVar.deref());
                } else {
                    return analyzeClojure(aVar);
                }
            } else {
                return analyzeClojure(form);
            }
            // If we need to support any more special forms:
            // } else if((p = (IParser) specials.valAt(form)) != null) {
            //     // return p.parse(context, form);
            //     return p.parse(form);
        } else {
            return analyzeClojure(form);
        }
    }

    /**
     * Do something with an existing Gershwin word definition.
     */
    public static Expr analyzeWord(Word word) {
        return new WordExpr(word);
    }

    public static Expr analyzeClojure(Object form) {
        return new ClojureExpr(form);
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
    public static void createVar(Symbol name, Object form, IPersistentMap formMeta) {
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
