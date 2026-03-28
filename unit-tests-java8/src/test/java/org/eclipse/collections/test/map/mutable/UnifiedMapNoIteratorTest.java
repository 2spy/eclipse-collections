/*
 * Copyright (c) 2021 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.test.map.mutable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.tuple.ImmutableEntry;
import org.eclipse.collections.test.NoIteratorTestCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class UnifiedMapNoIteratorTest implements MutableMapTestCase, NoIteratorTestCase
{
    private static final long CURRENT_TIME_MILLIS = System.currentTimeMillis();

    @SafeVarargs
    @Override
    public final <T> MutableMap<Object, T> newWith(T... elements)
    {
        Random random = new Random(CURRENT_TIME_MILLIS);
        MutableMap<Object, T> result = new UnifiedMapNoIterator<>();
        for (T each : elements)
        {
            assertNull(result.put(random.nextDouble(), each));
        }
        return result;
    }

    @Override
    public <K, V> MutableMap<K, V> newWithKeysValues(Object... elements)
    {
        if (elements.length % 2 != 0)
        {
            fail(String.valueOf(elements.length));
        }

        MutableMap<K, V> result = new UnifiedMapNoIterator<>();
        for (int i = 0; i < elements.length; i += 2)
        {
            assertNull(result.put((K) elements[i], (V) elements[i + 1]));
        }
        return result;
    }

    @Override
    @Test
    public void Iterable_next()
    {
        NoIteratorTestCase.super.Iterable_next();
    }

    @Override
    @Test
    public void Iterable_remove()
    {
        NoIteratorTestCase.super.Iterable_remove();
    }

    @Override
    @Test
    public void RichIterable_getFirst()
    {
        NoIteratorTestCase.super.RichIterable_getFirst();
    }

    @Override
    @Test
    public void RichIterable_getLast()
    {
        NoIteratorTestCase.super.RichIterable_getLast();
    }

    @Override
    @Test
    public void RichIterable_getFirst_and_getLast()
    {
        // Not applicable
    }

    @Override
    @Test
    public void RichIterable_getLast_empty_null()
    {
        // Not applicable
    }

    @Override
    @Test
    public void RichIterable_fused_collectMakeString()
    {
        // Not applicable
    }

    @Override
    @Test
    public void Map_keySet_equals()
    {
        // UnifiedMap.keySet().equals() delegates to the other Map's entrySet()
        Map<Integer, String> map = this.newWithKeysValues(3, "Three", 2, "Two", 1, "One");
        Set<Integer> expected = new MapTestCase.HashSetNoIterator<>();
        expected.add(3);
        expected.add(2);
        expected.add(1);
        assertThrows(AssertionError.class, () -> map.keySet().equals(expected));
    }

    @Override
    @Test
    public void Map_entrySet_equals()
    {
        // UnifiedMap.entrySet().equals() delegates to the other Map's entrySet()
        Map<Integer, String> map = this.newWithKeysValues(1, "One", 2, "Two", 3, "Three");
        Set<Map.Entry<Integer, String>> expected = new MapTestCase.HashSetNoIterator<>();
        expected.add(ImmutableEntry.of(1, "One"));
        expected.add(ImmutableEntry.of(2, "Two"));
        expected.add(ImmutableEntry.of(3, "Three"));
        assertThrows(AssertionError.class, () -> map.entrySet().equals(expected));
    }

    @Override
    @Test
    public void MapIterable_keySet_equals()
    {
        this.Map_keySet_equals();
    }

    @Override
    @Test
    public void MapIterable_entrySet_equals()
    {
        this.Map_entrySet_equals();
    }

    @Override
    @Test
    public void MutableMapIterable_updateValue()
    {
        /*
         * TODO: {@link UnifiedMap#KeySet#equals)} should be optimized to not use an iterator
         */

        /*
         * TODO: {@link org.eclipse.collections.impl.set.mutable.UnifiedSet#addAll(Collection)} should be optimized to not use an iterator when another UnifiedSet is passed in.
         */
    }

    public static class UnifiedMapNoIterator<K, V> extends UnifiedMap<K, V>
    {
        @Override
        public Iterator<V> iterator()
        {
            throw new AssertionError("No iteration patterns should delegate to iterator()");
        }

        @Override
        public Set<Entry<K, V>> entrySet()
        {
            return new NoIteratorSet<>(super.entrySet());
        }

        @Override
        public Set<K> keySet()
        {
            return new NoIteratorSet<>(super.keySet());
        }

        @Override
        public Collection<V> values()
        {
            return new NoIteratorCollection<>(super.values());
        }
    }

    private static final class NoIteratorSet<E> implements Set<E>
    {
        private final Set<E> delegate;

        private NoIteratorSet(Set<E> delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public int size()
        {
            return this.delegate.size();
        }

        @Override
        public boolean isEmpty()
        {
            return this.delegate.isEmpty();
        }

        @Override
        public boolean contains(Object o)
        {
            return this.delegate.contains(o);
        }

        @Override
        public void forEach(Consumer<? super E> action)
        {
            this.delegate.forEach(action);
        }

        @Override
        public Spliterator<E> spliterator()
        {
            return this.delegate.spliterator();
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter)
        {
            return this.delegate.removeIf(filter);
        }

        @Override
        public Iterator<E> iterator()
        {
            throw new AssertionError("No iteration patterns should delegate to iterator()");
        }

        @Override
        public Object[] toArray()
        {
            return this.delegate.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a)
        {
            return this.delegate.toArray(a);
        }

        @Override
        public boolean add(E e)
        {
            return this.delegate.add(e);
        }

        @Override
        public boolean remove(Object o)
        {
            return this.delegate.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c)
        {
            return this.delegate.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends E> c)
        {
            return this.delegate.addAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c)
        {
            return this.delegate.retainAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c)
        {
            return this.delegate.removeAll(c);
        }

        @Override
        public void clear()
        {
            this.delegate.clear();
        }

        @Override
        public boolean equals(Object obj)
        {
            return this.delegate.equals(obj);
        }

        @Override
        public int hashCode()
        {
            return this.delegate.hashCode();
        }

        @Override
        public String toString()
        {
            return this.delegate.toString();
        }
    }

    private static final class NoIteratorCollection<E> implements Collection<E>
    {
        private final Collection<E> delegate;

        private NoIteratorCollection(Collection<E> delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public int size()
        {
            return this.delegate.size();
        }

        @Override
        public boolean isEmpty()
        {
            return this.delegate.isEmpty();
        }

        @Override
        public boolean contains(Object o)
        {
            return this.delegate.contains(o);
        }

        @Override
        public void forEach(Consumer<? super E> action)
        {
            this.delegate.forEach(action);
        }

        @Override
        public Spliterator<E> spliterator()
        {
            return this.delegate.spliterator();
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter)
        {
            return this.delegate.removeIf(filter);
        }

        @Override
        public Iterator<E> iterator()
        {
            throw new AssertionError("No iteration patterns should delegate to iterator()");
        }

        @Override
        public Object[] toArray()
        {
            return this.delegate.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a)
        {
            return this.delegate.toArray(a);
        }

        @Override
        public boolean add(E e)
        {
            return this.delegate.add(e);
        }

        @Override
        public boolean remove(Object o)
        {
            return this.delegate.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c)
        {
            return this.delegate.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends E> c)
        {
            return this.delegate.addAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c)
        {
            return this.delegate.removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c)
        {
            return this.delegate.retainAll(c);
        }

        @Override
        public void clear()
        {
            this.delegate.clear();
        }

        @Override
        public boolean equals(Object obj)
        {
            return this.delegate.equals(obj);
        }

        @Override
        public int hashCode()
        {
            return this.delegate.hashCode();
        }

        @Override
        public String toString()
        {
            return this.delegate.toString();
        }
    }
}
