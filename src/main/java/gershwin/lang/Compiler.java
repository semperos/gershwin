package gershwin.lang;

import clojure.lang.Fn;
import clojure.lang.IFn;
import clojure.lang.IMapEntry;
import clojure.lang.IMeta;
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
import java.util.IdentityHashMap;
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
    static final Symbol NS = Symbol.intern("ns");
    // @todo Perhaps should be namespaced
    static final Keyword WORD_KW = Keyword.intern(null, "word");
    // DynamicClassLoader
    static final public Var LOADER = Var.create().setDynamic();
    static final public Var LOCAL_ENV = Var.create(null).setDynamic();
    static final public Var LOOP_LOCALS = Var.create().setDynamic();
    static final public Var NEXT_LOCAL_NUM = Var.create(0).setDynamic();
    static final public Var CONSTANTS = Var.create().setDynamic();
    static final public Var CONSTANT_IDS = Var.create().setDynamic();
    static final public Var VARS = Var.create().setDynamic();
    static final public Var KEYWORDS = Var.create().setDynamic();

    /**
     * Simple compilation to a Clojure function.
     *
     * @todo Consider whether inlining or simply referencing
     *   Clojure var's and invoking them as needed would be better.
     */
    public static Object compileDefinition(List rawForms) {
        IPersistentCollection definitionForms = PersistentVector.EMPTY;
        for(int i = 0; i < rawForms.size(); i++) {
            Object rawForm = rawForms.get(i);
            if(rawForm instanceof Symbol) {
                Expr expr = analyzeSymbol((Symbol) rawForm);
                if(expr instanceof WordExpr) {
                    // Word word = ((WordExpr) expr).getWord();
                    // Clojure turtles all the way down.
                    Var var = (Var) maybeSymbolIsWord(rawForm);
                    // @todo Consider using withInvoke instead, for clarity
                    // definitionForms = conj(definitionForms, clojure.lang.RT.list(word.getDefinitionFn()));
                    definitionForms = conj(definitionForms, withInvoke(var));
                } else if(expr instanceof ClojureExpr) {
                    // Var var = (Var) maybeSymbolIsWord(rawForm);
                    ClojureExpr clojureExpr = (ClojureExpr) expr;
                    if(clojureExpr.isWord()) {
                        definitionForms = conj(definitionForms, withInvoke(clojureExpr.val()));
                    } else {
                        ISeq form = withConjIt(rawForm);
                        definitionForms = conj(definitionForms, form);
                    }
                } else {
                    ISeq form = withConjIt(rawForm);
                    definitionForms = conj(definitionForms, form);
                }
            } else if(rawForm instanceof QuotationList) {
                QuotationExpr quotExpr = (QuotationExpr) analyzeQuotation((QuotationList) rawForm);
                Quotation quot = quotExpr.getQuotation();
                // Clojure turtles all the way down, again.
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
                    // A WordExpr means the rawForm is a Symbol that points to
                    // a Clojure Var resolves to a Gershwin word.
                    // @todo If this implementation proves preferable, change
                    //   how analyzeSymbol works to return a VarExpr
                    Var var = (Var) maybeSymbolIsWord(rawForm);
                    // Word word = ((WordExpr) expr).getWord();
                    // Clojure turtles all the way down.
                    // @todo Compare with code that just wraps this in a list to invoke
                    definitionForms = conj(definitionForms, withInvoke(var));
                } else if(expr instanceof ClojureExpr) {
                    ClojureExpr clojureExpr = (ClojureExpr) expr;
                    if(clojureExpr.isWord()) {
                        definitionForms = conj(definitionForms, withInvoke(clojureExpr.val()));
                    } else {
                        ISeq form = withConjIt(rawForm);
                        definitionForms = conj(definitionForms, form);
                    }
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
	Var.pushThreadBindings(
                               clojure.lang.RT.mapUniqueKeys(
                                                LOCAL_ENV, null,
                                                LOOP_LOCALS, null,
                                                NEXT_LOCAL_NUM, 0,
                                                clojure.lang.RT.READEVAL, clojure.lang.RT.T,
                                                clojure.lang.RT.CURRENT_NS, clojure.lang.RT.CURRENT_NS.deref(),
                                                CONSTANTS, PersistentVector.EMPTY,
                                                CONSTANT_IDS, new IdentityHashMap(),
                                                KEYWORDS, PersistentHashMap.EMPTY,
                                                VARS, PersistentHashMap.EMPTY
                                                //    ,LOADER, RT.makeClassLoader()
                                                ));
        try {
            List<String> lines = new ArrayList<String>();
            String internalName = sourcePath
                .replace(File.separator, "/")
                .substring(0, sourcePath.lastIndexOf('.'));
            for(Object r = Parser.read(pushbackReader, false, EOF, false); r != EOF;
                r = Parser.read(pushbackReader, false, EOF, false)) {
                compile1(lines, r);
            }
            writeClojureFile(internalName, lines);
        } catch(Parser.ReaderException e) {
            throw new CompilerException(sourcePath, e.line, e.column, e.getCause());
        } finally {
            Var.popThreadBindings();
        }
        return ret;
    }

    static void compile1(List<String> lines, Object form) {
        Var.pushThreadBindings(
                               clojure.lang.RT.map(LOADER, clojure.lang.RT.makeClassLoader())
                               );
        try {
            Expr expr = analyze(form);
            lines.add(expr.emit());
            expr.eval();
        } finally {
            Var.popThreadBindings();
        }
    }

    public static void writeClojureFile(String internalName, List<String> lines) throws IOException {
        String genPath = (String) clojure.lang.Compiler.COMPILE_PATH.deref();
        if(genPath == null)
            throw Util.runtimeException("*compile-path* not set");
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

    private static ISeq withStackVoid(Object rawForm) {
        return clojure.lang.RT.list(DO, rawForm, RT.STACK_VOID);
    }

    /**
     * "Compile" a non-Word form by wrapping it in a call to
     * Stack.conjIt, so the return value of the given expression
     * ends up on the stack. The Stack.conjIt method has built-in
     * knowledge of :gershwin.core/stack-void and will not add it
     * to the stack.
     */
    private static ISeq withConjIt(Object rawForm) {
        return withStackVoid(cons(DOT,
                                  cons(Symbol.intern("gershwin.lang.Stack"),
                                       cons(clojure.lang.RT.list(Symbol.intern("conjIt"), rawForm),
                                            null))));
    }

    /**
     * For Clojure functions, this is the same as wrapping the form
     * with clojure.lang.RT.list. That said, it depends on implementation
     * detail that IFn exposes an invoke() method, but it's also clearer
     * in this code what the intent is.
     */
    private static ISeq withInvoke(Object rawForm) {
        return clojure.lang.RT.list(Symbol.intern(".invoke"), rawForm);
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
	public Object val() {
            return x;
        }

        public boolean isWord() {
            if(this.x != null && this.x instanceof IMeta) {
                IPersistentMap meta = clojure.lang.RT.meta(this.x);
                if(meta != null) {
                    return clojure.lang.RT.booleanCast(meta.valAt(WORD_KW));
                } else {
                    return false;
                }
            }
            return false;
        }

	public Object eval() {
            Object form = null;
            Object rawForm = val();
            // Handle the ns macro as a special case, to avoid having
            // to clear the stack when requiring in namespaces.
            if(rawForm instanceof IPersistentList) {
                IPersistentList forms = (IPersistentList) rawForm;
                Object fst = clojure.lang.RT.first(forms);
                if(fst instanceof Symbol && fst.equals(NS)) {
                    // Wrap this in a do form and return :gershwin.core/stack-void
                    // to avoid putting anything on the stack.
                    form = clojure.lang.RT.list(DO, forms, RT.STACK_VOID);
                }
            }
            if(form == null)
                form = rawForm;
            Object clojureForm = clojure.lang.Compiler.eval(form, false);
            // Handle functions with ^:word metadata, which are Gershwin words
            // and should be invoked.
            boolean invoked = false;
            if(clojureForm instanceof Var) {
                Var aVar = (Var) clojureForm;
                IPersistentMap metadata = aVar.meta();
                if(metadata.containsKey(WORD_KW)) {
                    if(clojure.lang.RT.booleanCast(metadata.valAt(WORD_KW))) {
                        // This is a word fn, invoke it immediately
                        Object value = aVar.deref();
                        if(value instanceof IFn) {
                            invoked = true;
                            ((IFn) value).invoke();
                        } else if(value instanceof Word) {
                            invoked = true;
                            ((Word) value).invoke();
                        }
                    }
                }
            }
            if(!invoked)
                Stack.conjIt(clojureForm);
            return clojureForm;
	}

        public String emit() {
            Object form = null;
            Object rawForm = val();
            // Handle the ns macro as a special case, to avoid having
            // to clear the stack when requiring in namespaces.
            if(rawForm instanceof IPersistentList) {
                IPersistentList forms = (IPersistentList) rawForm;
                Object fst = clojure.lang.RT.first(forms);
                if(fst instanceof Symbol && fst.equals(NS)) {
                    // Wrap this in a do form and return :gershwin.core/stack-void
                    // to avoid putting anything on the stack.
                    form = clojure.lang.RT.list(DO, forms, RT.STACK_VOID);
                }
            }
            if(form == null)
                form = rawForm;
            // Hmmm
            // Object clojureForm = clojure.lang.Compiler.eval(form, false);
            Object clojureForm = form;
            // Handle functions with ^:word metadata, which are Gershwin words
            // and should be wrapped so that they get invoked.
            Object finalForm = null;
            // @todo NOTE TO SELF: Consider whether quotation fn's fall into this
            //   category and whether or not they need to be auto-invoked here.
            if(clojureForm instanceof Var) {
                Var aVar = (Var) clojureForm;
                IPersistentMap metadata = aVar.meta();
                if(metadata.containsKey(WORD_KW)) {
                    if(clojure.lang.RT.booleanCast(metadata.valAt(WORD_KW))) {
                        // This is a word fn, invoke it immediately
                        finalForm = withInvoke(clojureForm);
                        // IFn fn = (IFn) aVar.deref();
                        // fn.invoke();
                    }
                }
            } // else if(clojureForm instanceof IPersistentList) {
            //     // Leave Clojure well enough alone
            //     finalForm = clojureForm;
            // }
            if(finalForm == null)
                finalForm = withConjIt(clojureForm);
            return finalForm.toString();
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
         * Handle formatting {@link Symbol}s with the Gershwin
         * suffix, excluding definitions of {@code -main}.
         */
        private Symbol gershwinSymbol(Symbol word) {
            Symbol jvmMain = Symbol.intern("-main");
            if(word.equals(jvmMain)) {
                return word;
            } else {
                return Symbol.intern(word.getName() + GERSHWIN_VAR_SUFFIX);
            }
        }

        /**
         * Add a word definition to the current Clojure namespace
         * as a {@link clojure.lang.Var}. Words are instances of
         * {@link Word}.
         */
        public Object eval() {
            Symbol nameSym = (Symbol) this.l.get(0);
            Symbol gershwinName = gershwinSymbol(nameSym);
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
            if(wordMeta == null) {
                wordMeta = PersistentHashMap.EMPTY;
            }
            wordMeta = wordMeta
                .assoc(STACK_EFFECT_KEY, clojure.lang.RT.list(QUOTE, stackEffect))
                .assoc(WORD_KW, true);
            if(docString != null) {
                wordMeta = wordMeta.assoc(DOC_KEY, docString);
            }
            createVar(gershwinName, word, wordMeta);
            return word;
        }

        /**
         * Emit a word definition.
         *
         * @todo Consider outputting code that instantiates an
         *   an actual {@link Word} object and passes in the things it needs,
         *   will likely need it when adding, for example, stack effect analysis
         *   and would help distinguish the output from a random collection of
         *   Clojure functions.
         */
        public String emit() {
            Symbol nameSym = (Symbol) this.l.get(0);
            Symbol gershwinName = gershwinSymbol(nameSym);
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
            Object varForm = withStackVoid(clojure.lang.RT.list(DEF, Symbol.intern("^:word"), gershwinName, fnForm));
            return varForm.toString();
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
            return withConjIt(this.quot.getDefinitionForm()).toString();
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

    public static boolean isWord(Object form) {
        boolean ret = false;
        if(form instanceof Word)
            return true;
        if(form instanceof Var) {
            Var aVar = (Var) form;
            IPersistentMap metadata = aVar.meta();
            if(metadata.containsKey(WORD_KW)) {
                IMapEntry entry = metadata.entryAt(WORD_KW);
                return clojure.lang.RT.booleanCast(entry.getValue());
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static Object eval(Object form) {
        return eval(form, true);
    }

    /**
     * Deal with a single language form.
     *
     * Currently uses Clojure to evaluate form. This eval
     */
    public static Object eval(Object form, boolean freshLoader) {
        boolean createdLoader = false;
        //!LOADER.isBound())
	if(true) {
            Var.pushThreadBindings(clojure.lang.RT.map(LOADER, clojure.lang.RT.makeClassLoader()));
            createdLoader = true;
        }
        try {
            Expr expr = analyze(form);
            return expr.eval();
        } finally {
            if(createdLoader)
                Var.popThreadBindings();
        }
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
        Object maybeVar = maybeSymbolIsWord(form);
        if(maybeVar != null && maybeVar instanceof Var) {
            Var aVar = (Var) maybeVar;
            if(aVar.isBound()) {
                // if(aVar.deref() instanceof Word) {
                //     return analyzeWord((Word) aVar.deref());
                // } else {
                return analyzeClojure(aVar);
                // }
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

    public static Object maybeSymbolIsWord(Object form) {
        String maybeVarName = form.toString() + GERSHWIN_VAR_SUFFIX;
        Namespace currentClojureNs = (Namespace) clojure.lang.RT.CURRENT_NS.deref();
        // Consider whether suffix should be conditionally appended
        return clojure.lang.Compiler.maybeResolveIn(currentClojureNs, Symbol.intern(maybeVarName));
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
        Var.pushThreadBindings(
                               clojure.lang.RT.mapUniqueKeys(LOADER, clojure.lang.RT.makeClassLoader(),
                                                             LOCAL_ENV, null,
                                                             LOOP_LOCALS, null,
                                                             NEXT_LOCAL_NUM, 0,
                                                             clojure.lang.RT.READEVAL, clojure.lang.RT.T,
                                                             clojure.lang.RT.CURRENT_NS, clojure.lang.RT.CURRENT_NS.deref()));
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
        } finally {
            Var.popThreadBindings();
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
