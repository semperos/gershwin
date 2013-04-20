package gershwin.lang;

import java.util.Collection;

/**
 * Interface used to uniquely identify new word definitions.
 */
public class ColonList extends AGershwinList {
    public ColonList() {
        super();
    }

    public ColonList(Collection c) {
        super(c);
    }

    public ColonList(int initialCapacity) {
        super(initialCapacity);
    }
}
