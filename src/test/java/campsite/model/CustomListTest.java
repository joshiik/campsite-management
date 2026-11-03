package campsite.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import org.junit.jupiter.api.Test;

class CustomListTest {
    @Test
    void addsValuesAndRetrievesThemByIndex() {
        CustomList<String> list = new CustomList<>();

        assertTrue(list.isEmpty());
        list.add("first");
        list.add("second");

        assertEquals(2, list.size());
        assertEquals("first", list.get(0));
        assertEquals("second", list.get(1));
    }

    @Test
    void removesFirstMiddleLastAndNullValues() {
        CustomList<String> list = new CustomList<>();
        list.add(null);
        list.add("middle");
        list.add("last");

        assertTrue(list.remove(null));
        assertTrue(list.remove("middle"));
        assertTrue(list.remove("last"));
        assertFalse(list.remove("missing"));
        assertTrue(list.isEmpty());
    }

    @Test
    void clearRemovesEveryValue() {
        CustomList<Integer> list = new CustomList<>();
        list.add(1);
        list.add(2);

        list.clear();

        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    void iteratorVisitsValuesInInsertionOrder() {
        CustomList<Integer> list = new CustomList<>();
        list.add(4);
        list.add(7);
        list.add(9);
        Iterator<Integer> iterator = list.iterator();

        assertEquals(4, iterator.next());
        assertEquals(7, iterator.next());
        assertEquals(9, iterator.next());
        assertFalse(iterator.hasNext());
        assertThrows(java.util.NoSuchElementException.class, iterator::next);
    }

    @Test
    void rejectsIndexesOutsideTheList() {
        CustomList<String> list = new CustomList<>();
        list.add("only");

        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
    }
}
