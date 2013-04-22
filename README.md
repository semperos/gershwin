# Gershwin: Stack-based Clojure #

Gershwin is in early development. Take a look at `src/main/gwn/gershwin/core.gwn` to learn more about the language implementation, or the files under `src/main/java/gershwin/lang/` to peruse the parser, compiler, and runtime infrastructure.

## Usage

Run a REPL:

```
mvn -Prepl package
```

Or just build the uberjar:

```
mvn package
```

After which you can run it (replace x's with the version you built):

```
java -jar target/gershwin-x.x.x-uberjar.jar
```

## License

Copyright © 2013 Daniel L. Gregoire (semperos)

Distributed under the Eclipse Public License, the same as Clojure.
