# Gershwin: Emacs Major Mode #

The file `gershwin-mode.el` is a derived mode from `clojure-mode` that provides extra support for Gershwin code. Features currently include:

 * Associates `*.gwn` files with `gershwin-mode`
 * Makes Emacs' treatment of `<` and `>` more like parentheses than default
 * Adds keychord `C-c a` to use Paredit to wrap things in **a**ngle brackets (Gershwin quotations)
 * Highlights builtin words and word definitions

## Todos/Bugs ##

 * Make dependency on Paredit optional
 * Colorize Gershwin builtins like builtins, not like keywords
 * Expand list of Gershwin builtins

## Copyright ##

Copyright (C) 2013 Daniel Gregoire (semperos)
