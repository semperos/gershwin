# Notes #

## Clojure Notes ##

These notes are based on Clojure version 1.6.0-master-SNAPSHOT, commit 8be9b2b.

 * Main eval method - Compiler.java:6585
 * Main analyze method - Compiler.java:6325

High-level Clojure evaluation workflow:

 1. Parse input as one of whitespace, a number, a +/- followed by a number, a macro form, or an arbitrary token (Lisp symbols).
 2. Read goes through a single form and returns that. To read through a whole string/stream of input, read must be called in a loop until the end of the input is reached.
 3. The various "load" methods/functions take a given resource (String, stream) and do that looping, calling read on forms and passing that to eval.
 4. The central eval method does macro expansion, function invocation and analysis for other data types.
 5. Analysis takes a given form returned by the reader and analyzes it in the EVAL context to generate the appropriate subclass of the Expr interface. For complex Exprs, an Expr subclass will define an inner Parser class with a parse method that further breaks down how the complex Expr is represented in terms of other Expr's (see ConstantExpr for an example)
 6. A lot of important analysis starts from analyzeSeq, since Clojure is a Lisp and this is th
 7. These Expr subclasses come in groups (literals, assignables, etc.), but the essential methods from the Expr inteface are eval and emit, and most (all?) of the subclasses are also given a val method.
    * The eval method does the type-specific evaluation of the form
    * The val method returns the value of the given expression. For String literals, for example, calling eval() is the same as calling val(), and val() is the same as returning the original String that was passed into the constructor for StringExpr.
    * The emit method encodes how to emit the given data structure as JVM bytecode. This is where, for example, String values are pushed onto the JVM stack.

### Loading Code ###

Clojure relies primarily on loading files from the classpath, to include either already-compiled class files or Clojure source files if no class files exist. The only code that tries to load class files appears to be the `load` method in RT.java.

If there is no class file, Clojure will on-the-fly compile code. This is the `loadResourceScript` method in RT.java which gets called by `load` if there is no appropriate classfile for something you're trying to load. This `loadResourceScript` method calls the compiler's `load` method, which reads in the file and evaluates all its forms.

As a secondary mechanism, probably put in place during Clojure's initial development (although he does add extra metadata to load-file in Clojure, so perhaps it's intentional for some reason), you can load a file directly from the filesystem using the compilers `load-file` method. This calls the same `load` method in the compiler, simply providing a file reader instead of another kind of reader. In this way, it completely bypasses any consideration of the classpath or any AOT-compiled Clojure files that might be there.

Further, in core.clj, the functions `load-reader` and therefore `load-string` also call directly to the Clojure compiler, i.e. they do not look to the classpath for AOT-compiled files.
