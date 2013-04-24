package gershwin.lang;

import java.util.Iterator;
import java.util.List;

public class Quotation implements IInvocable {
    private final List quotationForms;

    public Quotation(List quotationForms) {
        this.quotationForms = quotationForms;
    }

    public List getQuotationForms() {
        return this.quotationForms;
    }

    public Object invoke() {
        Object ret = null;
        Iterator iter = quotationForms.iterator();
        while (iter.hasNext()) {
            ret = Compiler.eval(iter.next());
        }
        return ret;
    }
}
