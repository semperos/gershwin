# Todos #

 * Implement quotations
 * Hide compiler details in Gershwin's RT class (make a survey of what Clojure's does)
 * Implement custom print method in Gershwin's RT class
 * Implement 'if'
 * Enhancements to Clojure interop for:
     - Turning regular Clojure functions into ones that use the data stack
     - Capturing the (do ... :gershwin.core/stack-void) to prevent a Clojure expression's value from making it onto the data stack.

## Maybe's ##

 * Allow Factor-style comments with '!'

## Done ##
 * [DONE] File loading support
 * [DONE] Add prefix to names of Gershwin words, "__GWN__"
 * [DONE] Add special keyword/form that can be returned by Clojure interop that will not get onto the stack
