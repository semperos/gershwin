# Todos #

## Bugs ##

 * I'm sure there are plenty...

## Features ##

 * AOT compilation -- likely first pass will be Clojure source output -> AOT-compile that, only need to handle custom output for words, quotations, and then of course recursively output body definitions of words, which will eventually work down to Clojure code.
 * Enhance syntax of defining new words to allow associating metadata with the word
 * Support Factor's backslash which allows putting a word on the stack, to be executed later like a quotation would (prevents need to create a quotation on top of a word, which is already like a quotation)
 * Reconsider how vars default back to Clojure ones if not present as Gershwin ones (likely to cause confusion)
 * Enhancements to Clojure interop for:
     - Turning regular Clojure functions into ones that use the data stack
     - Capturing the (do ... :gershwin.core/stack-void) to prevent a Clojure expression's value from making it onto the data stack.
 * [ONGOING] Hide compiler details in Gershwin's RT class (make a survey of what Clojure's does)

## Maybe's ##

 * Allow identifiers that start with numbers, like Factor (name-munging)

## Done ##

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
