/**
 * Copyright Sergey Olefir
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.solf.extra2.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * An {@link ArrayList} implementation that provides support for {@link ReadOnlyList}
 * interface and some better type-checking (such as {@link #has(Object)} method).
 *
 * @author Sergey Olefir
 */
public class BArrayList<E> extends ArrayList<E> implements SerializableBList<E>
{
    /**
     * Constructs an empty list with an initial capacity of ten.
     * <p>
     * This exists (in addition to constructors) in order to provide interface similar to {@link BHashSet}
     */
	public static <E> BArrayList<E> create()
	{
		return new BArrayList<>();
	}

    /**
     * Constructs a list containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     * <p>
     * This exists (in addition to constructors) in order to provide interface similar to {@link BHashSet}
     *
     * @param c the collection whose elements are to be placed into this list
     * @throws NullPointerException if the specified collection is null
     */
	public static <E> BArrayList<E> create(Collection<? extends E> c)
	{
		return new BArrayList<>(c);
	}

    /**
     * Constructs an empty list with the specified initial capacity.
     * <p>
     * This exists (in addition to constructors) in order to provide interface similar to {@link BHashSet}
     *
     * @param  initialCapacity  the initial capacity of the list
     * @throws IllegalArgumentException if the specified initial capacity
     *         is negative
     */
	public static <E> BArrayList<E> create(int initialCapacity)
	{
		return new BArrayList<>(initialCapacity);
	}
	
    /**
     * Constructs an empty list with an initial capacity of ten.
     */
	public BArrayList()
	{
		super();
	}

    /**
     * Constructs a list containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     *
     * @param c the collection whose elements are to be placed into this list
     * @throws NullPointerException if the specified collection is null
     */
	public BArrayList(Collection<? extends E> c)
	{
		super(c);
	}

    /**
     * Constructs an empty list with the specified initial capacity.
     *
     * @param  initialCapacity  the initial capacity of the list
     * @throws IllegalArgumentException if the specified initial capacity
     *         is negative
     */
	public BArrayList(int initialCapacity)
	{
		super(initialCapacity);
	}

	@Override
	public boolean has(E o)
	{
		return contains(o);
	}

	/**
	 * Cached unmodifiable list if was created previously.
	 */
	transient private volatile List<E> cachedUnmodifiableList = null;
	@Override
	public @Nonnull List<E> toUnmodifiableJavaList()
	{
		List<E> result = cachedUnmodifiableList;
		if (result == null)
		{
			result = Collections.unmodifiableList(this);
			cachedUnmodifiableList = result;
		}
		
		return result;
	}

	@Override
	public @Nonnull Iterator<E> liveIterator()
	{
		return iterator();
	}

	@Override
	public boolean removeElement(E o)
	{
		return remove(o);
	}
	
	@Override
	public @Nonnull BList<E> subList(int fromIndex, int toIndex)
	{
		return new WrapperBList<>(super.subList(fromIndex, toIndex));
	}
}
