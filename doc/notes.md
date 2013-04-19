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
