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

import java.util.Collection;
import java.util.HashSet;

import javax.annotation.Nonnull;

/**
 * Extended/entry version of {@link BHashSet}; instances are created by various static
 * {@link #create()} factory methods.
 * <p>
 * Adds functionality such as being able to retrieve element from the set 
 * (relevant because object being {@link #equals(Object)} doesn't mean that
 * everything about the object is the same).
 * <p>
 * NOTE: the implementation of this on top of standard {@link HashSet} is pretty 
 * cheap and can basically be used everywhere where {@link HashSet}s are normally
 * used.
 * 
 * @see ESet
 * @see EReadOnlySet
 * @see RSet
 * @see RHashSet
 *
 * @author Sergey Olefir
 */
public class EHashSet
{
    /**
     * Constructs a new, empty set; the backing {@code HashMap} instance has
     * default initial capacity (16) and load factor (0.75).
     * <p>
     * The actual implementation is {@link RHashSet}
     */
	@Nonnull
	public static <E> RHashSet<E> create()
	{
		return new RHashSet<E>();
	}
	

    /**
     * Constructs a new, empty set; the backing {@code HashMap} instance has
     * the specified initial capacity and the specified load factor.
     * <p>
     * The actual implementation is {@link RHashSet}
     *
     * @param      initialCapacity   the initial capacity of the hash map
     * @param      loadFactor        the load factor of the hash map
     * @throws     IllegalArgumentException if the initial capacity is less
     *             than zero, or if the load factor is nonpositive
     */
	@Nonnull
	public static <E> RHashSet<E> create(int initialCapacity, float loadFactor) 
	{
		return new RHashSet<E>(initialCapacity, loadFactor);
    }

    /**
     * Constructs a new, empty set; the backing {@code HashMap} instance has
     * the specified initial capacity and default load factor (0.75).
     * <p>
     * The actual implementation is {@link RHashSet}
     *
     * @param      initialCapacity   the initial capacity of the hash table
     * @throws     IllegalArgumentException if the initial capacity is less
     *             than zero
     */
	@Nonnull
	public static <E> RHashSet<E> create(int initialCapacity) 
	{
		return new RHashSet<E>(initialCapacity);
    }

    /**
     * Constructs a new set containing the elements in the specified
     * collection.  The {@code HashMap} is created with default load factor
     * (0.75) and an initial capacity sufficient to contain the elements in
     * the specified collection.
     * <p>
     * The actual implementation is {@link RHashSet}
     *
     * @param c the collection whose elements are to be placed into this set
     * @throws NullPointerException if the specified collection is null
     */
	@Nonnull
	public static <E> RHashSet<E> create(@Nonnull Collection<? extends E> c) 
	{
		return RHashSet.create(c); 
    }

    /**
     * Constructs a new set containing the elements in the specified
     * {@link ReadOnlySet}.  The {@code HashMap} is created with default load factor
     * (0.75) and an initial capacity sufficient to contain the elements in
     * the specified {@link ReadOnlySet}.
     *
     * @param c the {@link ReadOnlySet} whose elements are to be placed into this set
     * @throws NullPointerException if the specified {@link ReadOnlySet} is null
     */
	@Nonnull
	public static <E> RHashSet<E> createFromReadOnly(@Nonnull ReadOnlySet<? extends E> c) 
	{
		return RHashSet.createFromReadOnly(c); 
    }
}
