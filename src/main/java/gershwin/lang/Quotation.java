package gershwin.lang;

import clojure.lang.IFn;

import java.util.Iterator;
import java.util.List;

public class Quotation implements IInvocable {
    private final IFn definitionFn;

    public Quotation(IFn definitionFn) {
        this.definitionFn = definitionFn;
    }

    public IFn getDuotationFn() {
        return this.definitionFn;
    }

    public Object invoke() {
        return Compiler.eval(this.definitionFn.invoke());
    }
}
