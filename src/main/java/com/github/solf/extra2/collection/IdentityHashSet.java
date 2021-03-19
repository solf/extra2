/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.solf.extra2.collection;

import static com.github.solf.extra2.util.NullUtil.nn;

import java.util.*;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import com.github.solf.extra2.util.TypeUtil;

/**
 * Identity hash set implementation based on {@link IdentityHashMap}.
 * 
 * The vast majority of the code is taken from IdentityHashSet in 
 * org.apache.pig.impl.util (Apache PIG 0.11.1).
 */
@SuppressWarnings("unlikely-arg-type")
public class IdentityHashSet<E> implements Set<E>, Cloneable
{
	
	/**
	 * Backing {@link IdentityHashMap}
	 */
	private IdentityHashMap<E, Object> map;
	
	/**
	 * Constructor with default initial size settings.
	 */
	public IdentityHashSet()
	{
		map = new IdentityHashMap<E, Object>();
	}

	/**
	 * Constructs a new, empty set with the specified expected maximum size.
	 * Putting more than the expected number of values into the set
	 * may cause the internal data structure to grow, which may be somewhat
	 * time-consuming.
	 * 
	 * @param expectedMaxSize the expected maximum size of the set
	 * @throws IllegalArgumentException if <tt>expectedMaxSize</tt> is negative
	 */
	public IdentityHashSet(int expectedMaxSize)
	{
		if( expectedMaxSize < 0 )
			throw new IllegalArgumentException("expectedMaxSize is negative: "
				+ expectedMaxSize);
		
		map = new IdentityHashMap<E, Object>(expectedMaxSize);
	}
	
	/**
	 * Constructs a new set containing the elements in the specified collection.
	 * The underlying {@link IdentityHashMap} is created with load factor 2/3 and
	 * initial capacity sufficient to contain the elements in the specified
	 * collection.
	 * 
	 * @param c the collection whose elements are to be placed into this set
	 * @throws NullPointerException if the specified collection is null
	 */
	public IdentityHashSet(Collection<? extends E> c)
	{
		map = new IdentityHashMap<E, Object>(Math.max(c.size() * 3 / 2 + 1, 16));
		addAll(c);
	}
	
	/**
	 * Constructs set based on the given {@link IdentityHashMap}.
	 */
	public IdentityHashSet(IdentityHashMap<E, Object> baseMap)
	{
		this.map = baseMap;
	}

	@Override
	public boolean add(E element)
	{
		if( map.containsKey(element) )
		{
			return false;
		}
		else
		{
			map.put(element, null);
			return true;
		}
	}

	@Override
	public boolean addAll(@Nonnull Collection<? extends E> elements)
	{
		boolean anyChanges = false;
		for( E element : elements )
		{
			if( !map.containsKey(element) )
			{
				anyChanges = true;
				map.put(element, null);
			}
		}
		return anyChanges;
	}

	@Override
	public void clear()
	{
		map.clear();
	}

	@Override
	public boolean contains(Object element)
	{
		return map.containsKey(element);
	}

	@Override
	public boolean containsAll(Collection<?> elements)
	{
		for( Object element : elements )
		{
			if( !map.containsKey(element) )
				return false;
		}
		return true;
	}

	@Override
	public boolean isEmpty()
	{
		return map.isEmpty();
	}

	@Override
	@Nonnull
	public Iterator<E> iterator()
	{

		return new Iterator<E>()
		{
			Iterator<Map.Entry<E, Object>> it = map.entrySet().iterator();

		    @Override
			public boolean hasNext()
			{
				return it.hasNext();
			}

		    @Override
			public E next()
			{
				return it.next().getKey();
			}

		    @Override
			public void remove()
			{
				it.remove();
			}
		};
	}

	@Override
	public boolean remove(Object element)
	{
		if( map.containsKey(element) )
		{
			map.remove(element);
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	public boolean removeAll(Collection<?> elements)
	{
		for( Object element : elements )
			map.remove(element);
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean retainAll(Collection<?> elements)
	{
		IdentityHashMap<E, Object> newMap = new IdentityHashMap<E, Object>();

		for( Object element : elements )
		{
			if( map.containsKey(element) )
				newMap.put((E)element, null);
		}

		boolean anyChanges = newMap.size() != map.size();

		map = newMap;
		return anyChanges;
	}

	@Override
	public int size()
	{
		return map.size();
	}

	@Override
	public Object[] toArray()
	{
		return map.keySet().toArray();
	}

	@Override
	public <T> T[] toArray(T[] dummy)
	{
		return map.keySet().toArray(dummy);
	}

	@Override
	public @Nonnull String toString()
	{
		StringBuffer buf = new StringBuffer();
		buf.append("{");

		Iterator<Entry<E, Object>> i = map.entrySet().iterator();
		boolean hasNext = i.hasNext();
		while( hasNext )
		{
			Entry<E, Object> e = i.next();
			E key = e.getKey();
			buf.append(key);
			hasNext = i.hasNext();
			if( hasNext )
				buf.append(", ");
		}

		buf.append("}");
		return buf.toString();
	}

	/**
     * Returns a shallow copy of this identity hash set: the values
     * themselves are not cloned.
     *
     * @return a shallow copy of this set
	 */
	@Override
	public @Nonnull IdentityHashSet<E> clone()
		throws CloneNotSupportedException
	{
		IdentityHashMap<E, Object> cloned = TypeUtil.coerce(nn(map.clone()));
		return new IdentityHashSet<E>(cloned); // Shallow clone underlying map
	}
}