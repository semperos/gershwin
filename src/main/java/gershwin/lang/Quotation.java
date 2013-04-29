package gershwin.lang;

import clojure.lang.IFn;

import java.util.Iterator;
import java.util.List;

public class Quotation implements IInvocable {
    private QuotationList quotationForms = null;
    private Object definitionForm = null;
    private final IFn definitionFn;

    public Quotation(IFn definitionFn) {
        this.definitionFn = definitionFn;
    }

    public Quotation(IFn definitionFn, Object definitionForm) {
        this.definitionFn = definitionFn;
        this.definitionForm = definitionForm;
    }

    public QuotationList getQuotationForms() {
        return this.quotationForms;
    }

    public void setQuotationForms(QuotationList quotationForms) {
        this.quotationForms = quotationForms;
    }

    public Object getDefinitionForm() {
        return this.definitionForm;
    }

    public void setDefinitionForm(Object definitionForm) {
        this.definitionForm = definitionForm;
    }

    public IFn getDefinitionFn() {
        return this.definitionFn;
    }

    public Object invoke() {
        return Compiler.eval(this.definitionFn.invoke());
    }
}
