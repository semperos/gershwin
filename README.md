# Gershwin: Stack-based, Concatenative Clojure #

Gershwin is in early development. Take a look at `src/main/gwn/gershwin/core.gwn` to learn more about the language implementation, or the files under `src/main/java/gershwin/lang` and `src/main/clj/gershwin` to peruse the parser, compiler, and runtime infrastructure.

## Usage

Build Gershwin:

```
mvn package
```

Run the executable Jar file:

```
java -jar target/gershwin-${version}-executable.jar
```

You can also run a REPL directly:

```
mvn -Prepl test
```

For a better REPL experience, I suggest running it through rlwrap or ledit. My personal ledit incantation looks like this:

```
ledit -x -h ~/.gershwin_repl_history mvn -Prepl test
```

## Examples

You can read the test suite under `src/test/gwn/gershwin` or take a look at [a small example project](https://github.com/semperos/prez-gwn) to see examples of Gershwin code.

## Editor Support

This repository houses a basic Emacs major mode, `gershwin-mode`, under `support/emacs/gershwin-mode`. This mode is derived from `clojure-mode` and provides some Gershwin-specific syntax highlighting, as well as treating `<` and `>` as proper delimiters.

## License

Copyright © 2013 Daniel L. Gregoire (semperos)

Distributed under the Eclipse Public License, the same as Clojure.
