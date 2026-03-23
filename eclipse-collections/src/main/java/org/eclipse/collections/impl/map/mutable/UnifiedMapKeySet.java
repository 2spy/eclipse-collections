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

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.procedure.AppendStringProcedure;
import org.eclipse.collections.impl.parallel.BatchIterable;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

/**
 * Key set view for {@link UnifiedMap}.
 */
final class UnifiedMapKeySet<K, V> implements Set<K>, Serializable, BatchIterable<K>
{
    private static final long serialVersionUID = 1L;

    private final UnifiedMap<K, V> map;

    UnifiedMapKeySet(UnifiedMap<K, V> map)
    {
        this.map = map;
    }

    @Override
    public boolean add(K key)
    {
        throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAll(Collection<? extends K> collection)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void clear()
    {
        this.map.clear();
    }

    @Override
    public boolean contains(Object o)
    {
        return this.map.containsKey(o);
    }

    @Override
    public boolean containsAll(Collection<?> collection)
    {
        for (Object aCollection : collection)
        {
            if (!this.map.containsKey(aCollection))
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
    public Iterator<K> iterator()
    {
        return this.map.new KeySetIterator();
    }

    @Override
    public boolean remove(Object key)
    {
        int oldSize = this.map.occupied;
        this.map.remove(key);
        return this.map.occupied != oldSize;
    }

    @Override
    public boolean removeAll(Collection<?> collection)
    {
        int oldSize = this.map.occupied;
        for (Object object : collection)
        {
            this.map.remove(object);
        }
        return oldSize != this.map.occupied;
    }

    public void putIfFound(Object key, Map<K, V> other)
    {
        int index = this.map.index(key);
        Object cur = this.map.table[index];
        if (cur != null)
        {
            Object val = this.map.table[index + 1];
            if (cur == UnifiedMap.CHAINED_KEY)
            {
                this.putIfFoundFromChain((Object[]) val, (K) key, other);
                return;
            }
            if (this.nonNullTableObjectEquals(cur, (K) key))
            {
                other.put(this.nonSentinel(cur), (V) val);
            }
        }
    }

    private void putIfFoundFromChain(Object[] chain, K key, Map<K, V> other)
    {
        for (int i = 0; i < chain.length; i += 2)
        {
            Object k = chain[i];
            if (k == null)
            {
                return;
            }
            if (this.nonNullTableObjectEquals(k, key))
            {
                other.put(this.nonSentinel(k), (V) chain[i + 1]);
            }
        }
    }

    @Override
    public boolean retainAll(Collection<?> collection)
    {
        int retainedSize = collection.size();
        UnifiedMap<K, V> retainedCopy = (UnifiedMap<K, V>) this.map.newEmpty(retainedSize);
        for (Object key : collection)
        {
            this.putIfFound(key, retainedCopy);
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
    public void forEach(Consumer<? super K> action)
    {
        this.map.forEachKey(action::accept);
    }

    @Override
    public void forEach(Procedure<? super K> procedure)
    {
        this.map.forEachKey(procedure);
    }

    @Override
    public int getBatchCount(int batchSize)
    {
        return this.map.getBatchCount(batchSize);
    }

    @Override
    public void batchForEach(Procedure<? super K> procedure, int sectionIndex, int sectionCount)
    {
        Object[] map = this.map.table;
        int sectionSize = map.length / sectionCount;
        int start = sectionIndex * sectionSize;
        int end = sectionIndex == sectionCount - 1 ? map.length : start + sectionSize;
        if (start % 2 != 0)
        {
            start++;
        }
        for (int i = start; i < end; i += 2)
        {
            Object cur = map[i];
            if (cur == UnifiedMap.CHAINED_KEY)
            {
                this.chainedForEachKey((Object[]) map[i + 1], procedure);
            }
            else if (cur != null)
            {
                procedure.value(this.nonSentinel(cur));
            }
        }
    }

    private void chainedForEachKey(Object[] chain, Procedure<? super K> procedure)
    {
        for (int i = 0; i < chain.length; i += 2)
        {
            Object cur = chain[i];
            if (cur == null)
            {
                return;
            }
            procedure.value(this.nonSentinel(cur));
        }
    }

    protected void copyKeys(Object[] result)
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
                        result[count++] = this.nonSentinel(cur);
                    }
                }
                else
                {
                    result[count++] = this.nonSentinel(x);
                }
            }
        }
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
        int hashCode = 0;
        Object[] table = this.map.table;
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
                        hashCode += cur == UnifiedMap.NULL_KEY ? 0 : cur.hashCode();
                    }
                }
                else
                {
                    hashCode += x == UnifiedMap.NULL_KEY ? 0 : x.hashCode();
                }
            }
        }
        return hashCode;
    }

    @Override
    public String toString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');
        Procedure<K> appendStringProcedure = new AppendStringProcedure<>(stringBuilder, ", ");
        this.forEach(appendStringProcedure);
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    @Override
    public Object[] toArray()
    {
        int size = this.map.size();
        Object[] result = new Object[size];
        this.copyKeys(result);
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
        this.copyKeys(result);
        if (size < result.length)
        {
            result[size] = null;
        }
        return result;
    }

    protected Object writeReplace()
    {
        MutableSet<K> replace = UnifiedSet.newSet(this.map.size());
        for (int i = 0; i < this.map.table.length; i += 2)
        {
            Object cur = this.map.table[i];
            if (cur == UnifiedMap.CHAINED_KEY)
            {
                this.chainedAddToSet((Object[]) this.map.table[i + 1], replace);
            }
            else if (cur != null)
            {
                replace.add(this.nonSentinel(cur));
            }
        }
        return replace;
    }

    private void chainedAddToSet(Object[] chain, MutableSet<K> replace)
    {
        for (int i = 0; i < chain.length; i += 2)
        {
            Object cur = chain[i];
            if (cur == null)
            {
                return;
            }
            replace.add(this.nonSentinel(cur));
        }
    }

    private K nonSentinel(Object key)
    {
        return key == UnifiedMap.NULL_KEY ? null : (K) key;
    }

    private boolean nonNullTableObjectEquals(Object cur, K key)
    {
        return cur == key || (cur == UnifiedMap.NULL_KEY ? key == null : cur.equals(key));
    }
}

