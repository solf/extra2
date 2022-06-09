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
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;

import javax.annotation.Nonnull;

/**
 * A 'better' version of {@link HashSet}; instances are created by various static
 * {@link #create()} factory methods.
 * <p>
 * The implementation should have very little performance impact compared to
 * {@link HashSet} and therefore can be used freely.
 * <p>
 * Provides improved functionality --
 * mainly providing a read-only view of the underlying set via {@link ReadOnlySet}
 * (so e.g. methods can declare that they accept {@link ReadOnlySet}) plus
 * some improved type-checking. See {@link BSet}
 * <p>
 * ---------------------------------------------------------------------
 * <p>
 * This class implements the {@code Set} interface, backed by a hash table
 * (actually a {@code HashMap} instance).  It makes no guarantees as to the
 * iteration order of the set; in particular, it does not guarantee that the
 * order will remain constant over time.  This class permits the {@code null}
 * element.
 *
 * <p>This class offers constant time performance for the basic operations
 * ({@code add}, {@code remove}, {@code contains} and {@code size}),
 * assuming the hash function disperses the elements properly among the
 * buckets.  Iterating over this set requires time proportional to the sum of
 * the {@code HashSet} instance's size (the number of elements) plus the
 * "capacity" of the backing {@code HashMap} instance (the number of
 * buckets).  Thus, it's very important not to set the initial capacity too
 * high (or the load factor too low) if iteration performance is important.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a hash set concurrently, and at least one of
 * the threads modifies the set, it <i>must</i> be synchronized externally.
 * This is typically accomplished by synchronizing on some object that
 * naturally encapsulates the set.
 *
 * If no such object exists, the set should be "wrapped" using the
 * {@link Collections#synchronizedSet Collections.synchronizedSet}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the set:<pre>
 *   Set s = Collections.synchronizedSet(new HashSet(...));</pre>
 *
 * <p>The iterators returned by this class's {@code iterator} method are
 * <i>fail-fast</i>: if the set is modified at any time after the iterator is
 * created, in any way except through the iterator's own {@code remove}
 * method, the Iterator throws a {@link ConcurrentModificationException}.
 * Thus, in the face of concurrent modification, the iterator fails quickly
 * and cleanly, rather than risking arbitrary, non-deterministic behavior at
 * an undetermined time in the future.
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw {@code ConcurrentModificationException} on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness: <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 * 
 * @author Sergey Olefir
 */
public class BHashSet
{
    /**
     * Constructs a new, empty set; the backing {@code HashMap} instance has
     * default initial capacity (16) and load factor (0.75).
     */
	@Nonnull
	public static <E> SerializableBSet<E> create()
	{
		return BSet.of(new HashSet<E>());
	}
	

    /**
     * Constructs a new, empty set; the backing {@code HashMap} instance has
     * the specified initial capacity and the specified load factor.
     *
     * @param      initialCapacity   the initial capacity of the hash map
     * @param      loadFactor        the load factor of the hash map
     * @throws     IllegalArgumentException if the initial capacity is less
     *             than zero, or if the load factor is nonpositive
     */
	@Nonnull
	public static <E> SerializableBSet<E> create(int initialCapacity, float loadFactor) 
	{
		return BSet.of(new HashSet<E>(initialCapacity, loadFactor));
    }

    /**
     * Constructs a new, empty set; the backing {@code HashMap} instance has
     * the specified initial capacity and default load factor (0.75).
     *
     * @param      initialCapacity   the initial capacity of the hash table
     * @throws     IllegalArgumentException if the initial capacity is less
     *             than zero
     */
	@Nonnull
	public static <E> SerializableBSet<E> create(int initialCapacity) 
	{
		return BSet.of(new HashSet<E>(initialCapacity));
    }

    /**
     * Constructs a new set containing the elements in the specified
     * collection.  The {@code HashMap} is created with default load factor
     * (0.75) and an initial capacity sufficient to contain the elements in
     * the specified collection.
     *
     * @param c the collection whose elements are to be placed into this set
     * @throws NullPointerException if the specified collection is null
     */
	@Nonnull
	public static <E> SerializableBSet<E> create(@Nonnull Collection<? extends E> c) 
	{
		return BSet.of(new HashSet<E>(c)); 
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
	public static <E> SerializableBSet<E> createFromReadOnly(@Nonnull ReadOnlySet<? extends E> c) 
	{
		if (c instanceof Collection)
		{
			@SuppressWarnings("unchecked") Collection<E> collection = (Collection<E>)c;
			return create(collection);
		}
		
		return create(c.toUnmodifiableJavaSet()); 
    }
	
}
