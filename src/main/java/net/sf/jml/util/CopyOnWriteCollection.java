/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.jml.util;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Thread-safe collection, most used for dispatch event.
 * 
 * @author Roger Chen
 */
public class CopyOnWriteCollection extends AbstractCollection {

    private static int DEF_INIT_CAPACITY = 5;

    private Object[] objects;
    private int size;

    public CopyOnWriteCollection() {
        this(DEF_INIT_CAPACITY);
    }

    public CopyOnWriteCollection(int initialCapacity) {
        if (initialCapacity < 0) {
            initialCapacity = DEF_INIT_CAPACITY;
        }
        objects = new Object[initialCapacity];
    }

    public int size() {
        return size;
    }

    public Iterator iterator() {
        return new Itr(objects, size);
    }

    public boolean add(Object o) {
        synchronized (objects) {
            int len = objects.length;
            if (size() < len) {
                objects[size] = o;
            } else {
                Object[] objs = new Object[len * 3 / 2 + DEF_INIT_CAPACITY];
                System.arraycopy(objects, 0, objs, 0, len);
                objs[len] = o;
                objects = objs;
            }
            size++;
        }
        return true;
    }

    public boolean remove(Object o) {
        synchronized (objects) {
            for (int i = 0; i < size; i++) {
                if (o == null ? objects[i] == null : o.equals(objects[i])) {
                    Object[] objs = new Object[objects.length];
                    System.arraycopy(objects, 0, objs, 0, i);
                    System.arraycopy(objects, i + 1, objs, i, size - i - 1);
                    size--;
                    objects = objs;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean removeAll(Collection c) {
        synchronized (objects) {
            boolean modified = false;
            for (int i = 0; i < size; i++) {
                if (c.contains(objects[i])) {
                    remove(objects[i]);
                    modified = true;
                }
            }
            return modified;
        }
    }

    public void clear() {
        synchronized (objects) {
            objects = new Object[objects.length];
            size = 0;
        }
    }

    private static class Itr implements Iterator {

        private Object[] objs;
        private int cursor = 0;
        private int currentSize = 0;

        private Itr(Object[] objs, int currentSize) {
            this.objs = objs;
            this.currentSize = currentSize;
        }

        public boolean hasNext() {
            return cursor != currentSize;
        }

        public Object next() {
            if (cursor < currentSize) {
                return objs[cursor++];
            }
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
