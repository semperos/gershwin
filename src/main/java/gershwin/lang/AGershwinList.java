package gershwin.lang;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Interface used to uniquely identify new language forms that are stored as lists.
 *
 * Implemented as an {@link ArrayList} because {@code LispReader.readDelimitedList}
 * returns {@link List} instances, so barring any particular reason to create
 * an additional Clojure data structure, {@link ArrayList} is fine.
 */
public abstract class AGershwinList extends ArrayList implements IGershwinList {
    public AGershwinList() {
        super();
    }

    public AGershwinList(Collection c) {
        super(c);
    }

    public AGershwinList(int initialCapacity) {
        super(initialCapacity);
    }
}
