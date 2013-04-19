# Gershwin #

Clojure + stack-based language.

The easiest way to get started is to clone this repository and run `lein run` at its root. This obviously means you need [Leiningen](https://github.com/technomancy/leiningen) installed first.

```
lein run
```

This will open up a Gershwin REPL. Try out the following:

```
gershwin> 20

--- Data Stack:
20

gershwin> 22

--- Data Stack:
20
22

gershwin> : add [] (+ (Stack/popIt) (Stack/popIt)) ;

--- Data Stack:
20
22

gershwin> add

--- Data Stack:
42

gershwin> : add-2 [] 2 add ;

--- Data Stack:
42

gershwin> add-2

--- Data Stack:
44

gershwin> add-2 add-2

--- Data Stack:
46

gershwin>
--- Data Stack:
48
```

How about Clojure ratios?

```
gershwin> 1/3

--- Data Stack:
1/3

gershwin> 1/4

--- Data Stack:
1/3
1/4

gershwin> add

--- Data Stack:
7/12
```

## Installation

For now, just clone this repo.

## Usage

Run a REPL:

```
lein run
```

## License

Copyright © 2013 Daniel L. Gregoire (semperos)

Distributed under the Eclipse Public License, the same as Clojure.
