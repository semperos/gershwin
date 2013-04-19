package gershwin.lang;

import clojure.lang.IPersistentCollection;

import java.util.List;

/**
 * Class representing a word definition in Gershwin.
 *
 * This class captures the semantics of evaluating a word that
 * itself will consist of other words and primitives (Clojure/Java)
 * that need to be recursively evaluated until executable code is found.
 */
public class Word {
    private final IPersistentCollection stackEffect;
    private final List definitionForms;

    /**
     * The stack effect is entered directly as a Clojure vector, hence
     * it's an {@link IPersistentCollection}. The forms of the definition,
     * however, are entered free-form, and since word definitions are read in
     * as delimited lists (": ... ;"), the default data structure used is
     * a simple {@link java.util.ArrayList}.
     *
     * @todo Stack effect is captured so that, at some point, we could actually
     *   verify the integrity of stack effect declarations programmatically, like
     *   Factor does. For now, it's a type of documentation.
     */
    public Word(IPersistentCollection stackEffect, List definitionForms) {
        this.stackEffect = stackEffect;
        this.definitionForms = definitionForms;
    }

    public List getDefinition() {
        return definitionForms;
    }

    public IPersistentCollection getStackEffect() {
        return stackEffect;
    }
}
