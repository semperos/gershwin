# Todos #

## Bugs ##

 * If a docstring is added but a stack effect is ommitted, a word is not defined but no error or warning is issued.

## Features ##

 * Provide Gershwin-sensitive compilation of Clojure vectors, maps, specifically to handle quotations
 * Add gershwin-declare that does proper suffixing.
 * Fix bug in lookup of Java classes where Clojure is handed a name__GWN__ symbol and attempts to resolve it.
 * Gershwin syntax for putting together core Clojure data structures but that allows internal Gershwin forms. Currently, raw Clojure syntax like (), [] and {} represent a switch to Clojure interop, inside of which only Clojure is legal. Possibilities include things like `V{` and `}` words for defining vectors, `H{` or `M{` for maps, etc. Current workarounds include building smaller pure-Clojure datastructures and then building in Gershwin forms as needed, for example: `[:foo] a-word conj`.
 * Consider further special-case handling of ns macro to include gershwin.core (and possibly gershwin.rt) by default in Gershwin files.
 * Make Gershwin 'require' word catch exception that a proper Clojure or class file was not found and then force an on-the-fly compilation. This is supposed to be captured in RT.load, so investigate that as well.
 * Since Clojure's `map` can work on multiple collections, provide at least equivalent `map2` and `map3` Gershwin words
 * Consider accepting either ';' or ';;' to terminate word definitions
 * Unit tests need a "parsing word" that can take in a quotation, execute it, and return what it would put on the stack in a collection. This can be accomplished by a word savvy enough to put something of its own (unique) on the stack, then execute a quotation, then take things off the stack until the special item is reached.
 * Reading/parsing of Gershwin from Gershwin (Parsing words, Factor lexer)
 * Compiler -- make sure line/col numbers are present for Gershwin forms
 * Support Factor's backslash which allows putting a word on the stack, to be executed later like a quotation would (prevents need to create a quotation on top of a word, which is already like a quotation)
 * [ONGOING] Hide compiler details in Gershwin's RT class (make a survey of what Clojure's does)

## Maybe's ##

 * Consider data structure used to store Stack
 * Allow identifiers that start with numbers, like Factor (name-munging like Clojure does for ! => BANG)
 * Consider refactor to write impl. completely in Clojure and use Clojure's tools.reader as exemplar for Gershwin reader, as well as the Gershwin reader's Clojure delegate

## Probably Not's ##

**Use (:arglists (meta #'a-var)) to auto-transform Clojure functions to Gershwin words**

At first glance, this seems like a interesting prospect to avoid a lot of menial transformation work, but in reality, dealing with (1) multiple arities per function, (2) rest args, (3) destructuring, and (4) argument order differences, it's not worth the implicitness/uncertainty that would certainly accompany such a feature.

## Done ##

Done may mean medium-rare, but cooked enough to make meaningful forward progress.

 * [DONE] Change `has-item?` to `member?` and `has-items?` to `has-any?`
 * [DONE] (Is a feature) Reconsider how vars default back to Clojure ones if not present as Gershwin ones (likely to cause confusion)
 * [DONE] Build orchestration to ensure all Gershwin code has been compiled to Clojure
 * [DONE] AOT compilation -- likely first pass will be Clojure source output -> AOT-compile that, only need to handle custom output for words, quotations, and then of course recursively output body definitions of words, which will eventually work down to Clojure code.
 * [DONE] Reuse clojure.main `repl` fn, supplying the necessary read/eval/print functions to use Gershwin
 * [DONE] Testing "Framework"
 * [DONE] The todo below this one is symptomatic of an incomplete implementation of "emit" for all cases in the Gershwin compiler. Just like compileDefinition and emitDefinition handle things differently for Words and Clojure Word functions, the same needs to happen to those forms when encountered outside of word definitions, i.e. when used at the top-level of a file.
 * [DONE] (And not true) Need to document limitations of namespaces that use :gen-class, i.e. they need to be pure Clojure + the -main word definition. No Gershwin-style loading/requiring allowed, and all Gershwin should already be compiled.
 * [DONE] Figure out why Clojure's load doesn't put you in the ns, but Gershwin's does (needed to add Var.pushThreadBindings bindings in appropriate spots in compiler)
 * [DONE] Elementary compilation
 * [DONE] Starting support for Gershwin/Clojure namespaces
 * [DONE] Make exception handling saner at REPL (i.e. catch them)
 * [DONE] Do same parsing for < as we do for : so that < can still be used for names.
 * [DONE] Copy read1 into Parser from LispReader
 * [DONE] Implement 'if' via Clojure interop in core.gwn; keep extra-primitive one as 'if*' for example
 * [DONE] Fix reading of word definitions (Gershwin impl. of readDelimitedList)
 * [DONE] Enhance syntax of defining new words to allow associating metadata with the word
 * [DONE] Add special invoke word to invoke Clojure keywords, so they act like specialized quotations.
 * [DONE] Use of immutable methods on Stack returns `nil` instead of throwing exceptions for stack underflow -- needs to throw exception
 * [DONE] Implement 'if'
 * [DONE] Allow Factor-style comments with '!'
 * [DONE] Implement custom print method in Gershwin's RT class
 * [DONE] When defining a new word, print symbol 'ok' at the REPL
 * [DONE] Implement quotations
 * [DONE] File loading support
 * [DONE] Add prefix to names of Gershwin words, "__GWN__"
 * [DONE] Add special keyword/form that can be returned by Clojure interop that will not get onto the stack
