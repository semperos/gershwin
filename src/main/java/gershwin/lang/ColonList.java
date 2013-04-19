package gershwin.lang;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Interface used to uniquely identify new word definitions.
 *
 * Implemented as an {@link ArrayList} because {@code LispReader.readDelimitedList}
 * returns {@link List} instances, so barring any particular reason to create
 * an additional Clojure data structure, {@link ArrayList} is fine.
 */
public class ColonList extends ArrayList implements IColonList {
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
