package gershwin.lang;

import clojure.lang.IFn;
import clojure.lang.IObj;
import clojure.lang.IPersistentCollection;
import clojure.lang.IPersistentMap;

import java.util.Iterator;

/**
 * Class representing a word definition in Gershwin.
 *
 * This class captures the semantics of evaluating a word that
 * itself will consist of other words and primitives (Clojure/Java)
 * that need to be recursively evaluated until executable code is found.
 */
public class Word implements IInvocable, IObj {
    private final IPersistentCollection stackEffect;
    private final IFn definitionFn;
    private final IPersistentMap _meta;

    public IPersistentCollection getStackEffect() {
        return this.stackEffect;
    }

    public IFn getDefinitionFn() {
        return this.definitionFn;
    }

    /**
     * Word definitions created programmatically don't require the usual.
     */
    public Word() {
        this.stackEffect = null;
        this.definitionFn = null;
        this._meta = null;
    }

    /**
     * @todo Stack effect is captured so that, at some point, we could actually
     *   verify the integrity of stack effect declarations programmatically, like
     *   Factor does. For now, it's a type of documentation.
     */
    public Word(IPersistentCollection stackEffect, IFn definitionFn) {
        this.stackEffect = stackEffect;
        this.definitionFn = definitionFn;
        this._meta = null;
    }

    public Word(IPersistentMap meta, IPersistentCollection stackEffect, IFn definitionFn) {
        this.stackEffect = stackEffect;
        this.definitionFn = definitionFn;
        this._meta = meta;
    }

    /**
     * Invoke a word definition by invoking the Clojure function that is its impl.
     */
    public Object invoke() {
        return this.definitionFn.invoke();
    }

    public IObj withMeta(IPersistentMap meta){
	return new Word(meta, stackEffect, definitionFn);
    }

    public IPersistentMap meta(){
	return _meta;
    }

}
