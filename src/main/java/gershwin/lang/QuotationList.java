package gershwin.lang;

import java.util.Collection;

/**
 * Interface used to uniquely identify quotations.
 */
public class QuotationList extends AGershwinList {
    public QuotationList() {
        super();
    }

    public QuotationList(Collection c) {
        super(c);
    }

    public QuotationList(int initialCapacity) {
        super(initialCapacity);
    }
}
