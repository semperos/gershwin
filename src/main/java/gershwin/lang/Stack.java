package gershwin.lang;

import clojure.lang.Atom;
import clojure.lang.IFn;
import clojure.lang.ISeq;
import clojure.lang.IPersistentStack;
import clojure.lang.PersistentVector;

/**
 * Internally, the stack is a Clojure vector held inside an Atom. Normal
 * method names treat the stack as a persistent data structure, dereferencing
 * the Atom as needed. Method names suffixed with "Mutable" denote mutable versions
 * that affect the state of the Atom holding the stack.
 */
public class Stack {
    private static Atom stackAtom = new Atom(PersistentVector.EMPTY);;
    private static IFn CLOJURE_CONJ = ClojureApi.var("clojure.core", "conj");
    private static IFn CLOJURE_POP = ClojureApi.var("clojure.core", "pop");
    private static String STACK_UNDERFLOW_MSG = "Data stack underflow. Can't take something off an empty data stack.";

    public static IPersistentStack conj(Object form) {
        return (IPersistentStack) CLOJURE_CONJ.invoke(stackAtom.deref(), form);
    }

    public static IPersistentStack conjMutable(Object form) {
        stackAtom.swap(CLOJURE_CONJ, form);
        return (IPersistentStack) stackAtom.deref();
    }

    public static Object peek() {
        IPersistentStack rawStack = (IPersistentStack) stackAtom.deref();
        return rawStack.peek();
    }

    public static IPersistentStack pop() {
        try {
            return (IPersistentStack) CLOJURE_POP.invoke(stackAtom.deref());
        } catch(IllegalStateException e) {
            throw new StackUnderflowException(STACK_UNDERFLOW_MSG, e);
        }
    }

    public static IPersistentStack popMutable() {
        try {
            return (IPersistentStack) stackAtom.swap(CLOJURE_POP);
        } catch(IllegalStateException e) {
            throw new StackUnderflowException(STACK_UNDERFLOW_MSG);
        }
    }

    /**
     * This is what a traditional mutable stack would simply call
     * 'pop', but I've reserved 'pop' to be used in accordance with
     * the Clojure idiom, which returns the remaining collection instead
     * of the item popped.
     */
    public static Object popIt() {
        IPersistentStack rawStack = (IPersistentStack) stackAtom.deref();
        Object item = rawStack.peek();
        // What was peeked above and what gets mutably popped below
        // are not guaranteed to be the same thing. Reconsider use of Atom.
        try {
            stackAtom.swap(CLOJURE_POP);
        } catch(IllegalStateException e) {
            throw new StackUnderflowException(STACK_UNDERFLOW_MSG);
        }
        return item;
    }

    public static IPersistentStack clear() {
        return (IPersistentStack) stackAtom.reset(PersistentVector.EMPTY);
    }

    public static ISeq seq() {
        IPersistentStack rawStack = (IPersistentStack) stackAtom.deref();
        return rawStack.seq();
    }

    public static class StackUnderflowException extends IllegalStateException {
        public StackUnderflowException() {
            super();
        }

        public StackUnderflowException(String message) {
            super(message);
        }

        public StackUnderflowException(String message, Throwable e) {
            super(message, e);
        }
    }

}
