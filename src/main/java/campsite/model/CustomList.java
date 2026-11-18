package campsite.model;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A small singly linked list used instead of Java collection classes.
 */
public final class CustomList<T> implements Iterable<T>, Serializable {
    private static final long serialVersionUID = 1L;

    private Node<T> first;
    private Node<T> last;
    private int size;

    public void add(T value) {
        Node<T> node = new Node<>(value);
        if (last == null) {
            first = node;
        } else {
            last.next = node;
        }
        last = node;
        size++;
    }

    public T get(int index) {
        checkIndex(index);
        Node<T> current = first;
        for (int position = 0; position < index; position++) {
            current = current.next;
        }
        return current.value;
    }

    public boolean remove(T value) {
        Node<T> previous = null;
        Node<T> current = first;
        while (current != null) {
            if (current.value == value
                    || (current.value != null && current.value.equals(value))) {
                removeNode(previous, current);
                return true;
            }
            previous = current;
            current = current.next;
        }
        return false;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        first = null;
        last = null;
        size = 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Node<T> next = first;

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public T next() {
                if (next == null) {
                    throw new NoSuchElementException();
                }
                T value = next.value;
                next = next.next;
                return value;
            }
        };
    }

    
/// remove node from the list, given the previous node and the current node to be removed
    private void removeNode(Node<T> previous, Node<T> current) {
        if (previous == null) {
            first = current.next;
        } else {
            previous.next = current.next;
        }
        if (last == current) {
            last = previous;
        }
        size--;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", size: " + size);
        }
    }

    private static final class Node<T> implements Serializable {
        private static final long serialVersionUID = 1L;

        private final T value;
        private Node<T> next;

        private Node(T value) {
            this.value = value;
        }
    }
}
