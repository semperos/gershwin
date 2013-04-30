# Todos #

## Bugs ##

 * If a docstring is added by a stack effect is ommitted, a word is not defined but no error or warning is issued.

## Features ##

 * Reuse clojure.main `repl` fn, supplying the necessary read/eval/print functions to use Gershwin
 * Build orchestration to ensure all Gershwin code has been compiled to Clojure
 * Porting Clojure collection handling to Gershwin (NEXT: Basic math)
 * Porting Factor combinators to Gershwin (NEXT: Conditional combinators)
 * Testing "framework"
 * Programmatic reading/parsing (Parsing words, Factor lexer)
 * Compiler -- make sure line/col numbers are present for Gershwin forms
 * AOT compilation -- likely first pass will be Clojure source output -> AOT-compile that, only need to handle custom output for words, quotations, and then of course recursively output body definitions of words, which will eventually work down to Clojure code.
 * Support Factor's backslash which allows putting a word on the stack, to be executed later like a quotation would (prevents need to create a quotation on top of a word, which is already like a quotation)
 * Reconsider how vars default back to Clojure ones if not present as Gershwin ones (likely to cause confusion)
 * [ONGOING] Hide compiler details in Gershwin's RT class (make a survey of what Clojure's does)

## Maybe's ##

 * Allow identifiers that start with numbers, like Factor (name-munging)
 * Consider refactor to write impl. completely in Clojure and use Clojure's tools.reader as exemplar for Gershwin reader, as well as the Gershwin reader's Clojure delegate

## Probably Not's ##

**Use (:arglists (meta #'a-var)) to auto-transform Clojure functions to Gershwin words**

At first glance, this seems like a interesting prospect to avoid a lot of menial transformation work, but in reality, dealing with (1) multiple arities per function, (2) rest args, (3) destructuring, and (4) argument order differences, it's not worth the implicitness/uncertainty that would certainly accompany such a feature.

## Done ##

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
