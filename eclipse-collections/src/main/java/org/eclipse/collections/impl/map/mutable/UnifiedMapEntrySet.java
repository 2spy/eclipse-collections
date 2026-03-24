/*
 * Copyright (c) 2024 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.map.mutable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.impl.block.procedure.AppendStringProcedure;
import org.eclipse.collections.impl.parallel.BatchIterable;
import org.eclipse.collections.impl.tuple.ImmutableEntry;

/**
 * Entry set view for {@link UnifiedMap}.
 */
final class UnifiedMapEntrySet<K, V> implements Set<Map.Entry<K, V>>, Serializable, BatchIterable<Map.Entry<K, V>>
{
    private static final long serialVersionUID = 1L;

    private final UnifiedMap<K, V> map;
    private transient WeakReference<UnifiedMap<K, V>> holder;

    UnifiedMapEntrySet(UnifiedMap<K, V> map)
    {
        this.map = map;
        this.holder = new WeakReference<>(map);
    }

    @Override
    public void forEach(Consumer<? super Map.Entry<K, V>> action)
    {
        Objects.requireNonNull(action);
        this.forEach(each -> action.accept(each));
    }

    @Override
    public boolean add(Map.Entry<K, V> entry)
    {
        throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAll(Collection<? extends Map.Entry<K, V>> collection)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void clear()
    {
        this.map.clear();
    }

    public boolean containsEntry(Map.Entry<?, ?> entry)
    {
        return this.getEntry(entry) != null;
    }

    private Map.Entry<K, V> getEntry(Map.Entry<?, ?> entry)
    {
        K key = (K) entry.getKey();
        V value = (V) entry.getValue();
        int index = this.map.index(key);

        Object cur = this.map.table[index];
        Object curValue = this.map.table[index + 1];
        if (cur == UnifiedMap.CHAINED_KEY)
        {
            return this.chainGetEntry((Object[]) curValue, key, value);
        }
        if (cur == null)
        {
            return null;
        }
        if (this.map.nonNullTableObjectEquals(cur, key))
        {
            if (UnifiedMap.nullSafeEquals(value, curValue))
            {
                return ImmutableEntry.of(this.map.nonSentinel(cur), (V) curValue);
            }
        }
        return null;
    }

    private Map.Entry<K, V> chainGetEntry(Object[] chain, K key, V value)
    {
        for (int i = 0; i < chain.length; i += 2)
        {
            Object cur = chain[i];
            if (cur == null)
            {
                return null;
            }
            if (this.map.nonNullTableObjectEquals(cur, key))
            {
                Object curValue = chain[i + 1];
                if (UnifiedMap.nullSafeEquals(value, curValue))
                {
                    return ImmutableEntry.of(this.map.nonSentinel(cur), (V) curValue);
                }
            }
        }
        return null;
    }

    @Override
    public boolean contains(Object o)
    {
        return o instanceof Map.Entry && this.containsEntry((Map.Entry<?, ?>) o);
    }

    @Override
    public boolean containsAll(Collection<?> collection)
    {
        for (Object obj : collection)
        {
            if (!this.contains(obj))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isEmpty()
    {
        return this.map.isEmpty();
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator()
    {
        return this.map.new EntrySetIterator(this.holder);
    }

    @Override
    public boolean remove(Object e)
    {
        if (!(e instanceof Map.Entry))
        {
            return false;
        }
        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) e;
        K key = (K) entry.getKey();
        V value = (V) entry.getValue();

        int index = this.map.index(key);

        Object cur = this.map.table[index];
        if (cur != null)
        {
            Object val = this.map.table[index + 1];
            if (cur == UnifiedMap.CHAINED_KEY)
            {
                return this.removeFromChain((Object[]) val, key, value, index);
            }
            if (this.map.nonNullTableObjectEquals(cur, key) && UnifiedMap.nullSafeEquals(value, val))
            {
                this.map.table[index] = null;
                this.map.table[index + 1] = null;
                this.map.occupied--;
                return true;
            }
        }
        return false;
    }

    private boolean removeFromChain(Object[] chain, K key, V value, int index)
    {
        for (int i = 0; i < chain.length; i += 2)
        {
            Object k = chain[i];
            if (k == null)
            {
                return false;
            }
            if (this.map.nonNullTableObjectEquals(k, key))
            {
                V val = (V) chain[i + 1];
                if (UnifiedMap.nullSafeEquals(val, value))
                {
                    this.map.overwriteWithLastElementFromChain(chain, index, i);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> collection)
    {
        boolean changed = false;
        for (Object obj : collection)
        {
            if (this.remove(obj))
            {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> collection)
    {
        int retainedSize = collection.size();
        UnifiedMap<K, V> retainedCopy = (UnifiedMap<K, V>) this.map.newEmpty(retainedSize);

        for (Object obj : collection)
        {
            if (obj instanceof Map.Entry)
            {
                Map.Entry<?, ?> otherEntry = (Map.Entry<?, ?>) obj;
                Map.Entry<K, V> thisEntry = this.getEntry(otherEntry);
                if (thisEntry != null)
                {
                    retainedCopy.put(thisEntry.getKey(), thisEntry.getValue());
                }
            }
        }
        if (retainedCopy.size() < this.size())
        {
            this.map.maxSize = retainedCopy.maxSize;
            this.map.occupied = retainedCopy.occupied;
            this.map.table = retainedCopy.table;
            return true;
        }
        return false;
    }

    @Override
    public int size()
    {
        return this.map.size();
    }

    @Override
    public void forEach(Procedure<? super Map.Entry<K, V>> procedure)
    {
        for (int i = 0; i < this.map.table.length; i += 2)
        {
            Object cur = this.map.table[i];
            if (cur == UnifiedMap.CHAINED_KEY)
            {
                this.chainedForEachEntry((Object[]) this.map.table[i + 1], procedure);
            }
            else if (cur != null)
            {
                procedure.value(new UnifiedMap.WeakBoundEntry<>(this.map.nonSentinel(cur), (V) this.map.table[i + 1], this.holder));
            }
        }
    }

    private void chainedForEachEntry(Object[] chain, Procedure<? super Map.Entry<K, V>> procedure)
    {
        for (int i = 0; i < chain.length; i += 2)
        {
            Object cur = chain[i];
            if (cur == null)
            {
                return;
            }
            procedure.value(new UnifiedMap.WeakBoundEntry<>(this.map.nonSentinel(cur), (V) chain[i + 1], this.holder));
        }
    }

    @Override
    public int getBatchCount(int batchSize)
    {
        return this.map.getBatchCount(batchSize);
    }

    @Override
    public void batchForEach(Procedure<? super Map.Entry<K, V>> procedure, int sectionIndex, int sectionCount)
    {
        Object[] tab = this.map.table;
        int sectionSize = tab.length / sectionCount;
        int start = sectionIndex * sectionSize;
        int end = sectionIndex == sectionCount - 1 ? tab.length : start + sectionSize;
        if (start % 2 != 0)
        {
            start++;
        }
        for (int i = start; i < end; i += 2)
        {
            Object cur = tab[i];
            if (cur == UnifiedMap.CHAINED_KEY)
            {
                this.chainedForEachEntry((Object[]) tab[i + 1], procedure);
            }
            else if (cur != null)
            {
                procedure.value(ImmutableEntry.of(this.map.nonSentinel(cur), (V) tab[i + 1]));
            }
        }
    }

    protected void copyEntries(Object[] result)
    {
        Object[] table = this.map.table;
        int count = 0;
        for (int i = 0; i < table.length; i += 2)
        {
            Object x = table[i];
            if (x != null)
            {
                if (x == UnifiedMap.CHAINED_KEY)
                {
                    Object[] chain = (Object[]) table[i + 1];
                    for (int j = 0; j < chain.length; j += 2)
                    {
                        Object cur = chain[j];
                        if (cur == null)
                        {
                            break;
                        }
                        result[count++] =
                                new UnifiedMap.WeakBoundEntry<>(this.map.nonSentinel(cur), (V) chain[j + 1], this.holder);
                    }
                }
                else
                {
                    result[count++] = new UnifiedMap.WeakBoundEntry<>(this.map.nonSentinel(x), (V) table[i + 1], this.holder);
                }
            }
        }
    }

    @Override
    public Object[] toArray()
    {
        Object[] result = new Object[this.map.size()];
        this.copyEntries(result);
        return result;
    }

    @Override
    public <T> T[] toArray(T[] result)
    {
        int size = this.map.size();
        if (result.length < size)
        {
            result = (T[]) Array.newInstance(result.getClass().getComponentType(), size);
        }
        this.copyEntries(result);
        if (size < result.length)
        {
            result[size] = null;
        }
        return result;
    }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        this.holder = new WeakReference<>(this.map);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Set)
        {
            Set<?> other = (Set<?>) obj;
            if (other.size() == this.size())
            {
                return this.containsAll(other);
            }
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return this.map.hashCode();
    }

    @Override
    public String toString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');
        Procedure<Map.Entry<K, V>> appendStringProcedure = new AppendStringProcedure<>(stringBuilder, ", ");
        this.forEach(appendStringProcedure);
        stringBuilder.append(']');
        return stringBuilder.toString();
    }
}
