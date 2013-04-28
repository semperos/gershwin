package gershwin.lang;

import clojure.lang.IFn;

import java.util.Iterator;
import java.util.List;

public class Quotation implements IInvocable {
    private QuotationList quotationForms = null;
    private final IFn definitionFn;

    public Quotation(IFn definitionFn) {
        this.definitionFn = definitionFn;
    }

    public QuotationList getQuotationForms() {
        return this.quotationForms;
    }

    public void setQuotationForms(QuotationList quotationForms) {
        this.quotationForms = quotationForms;
    }

    public IFn getDefinitionFn() {
        return this.definitionFn;
    }

    public Object invoke() {
        return Compiler.eval(this.definitionFn.invoke());
    }
}
