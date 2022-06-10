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

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;

/**
 * A 'better' version of {@link HashMap} that provides improved functionality:
 * - a read-only view of the underlying map via {@link ReadOnlyMap}
 * (so e.g. methods can declare that they accept {@link ReadOnlyMap});
 * - improved/clarified functionality such as {@link #getOrCreateValueNonNull(Object, Function)}
 * and {@link #putIfNoKey(Object, Object)}/{@link #putIfNoValue(Object, Object)};
 * - plus some improved type-checking as in {@link #removeAndGet(Object)}
 * <p>
 * NOTE: this implementation has specific overhead compared to standard {@link HashMap} 
 * -- for every mapping stored in the map an additional wrapper object is 
 * created in order to provide the required functionality.
 * <p>
 * Another option is to use {@link BHashMap2} which has different performance
 * trade-off:
 * The performance impact of using {@link BHashMap2} is very similar to using {@link Collections#unmodifiableMap(Map)} --
 * specifically any entry iteration over the map results in a creation of 
 * intermediate wrapper object ({@link ReadOnlyEntry}) for each entry iterated.
 * <p>
 * ---------------------------------------------------------------------
 * <p>
 * Hash table based implementation of the {@code Map} interface.  This
 * implementation provides all of the optional map operations, and permits
 * {@code null} values and the {@code null} key.  (The {@code HashMap}
 * class is roughly equivalent to {@code Hashtable}, except that it is
 * unsynchronized and permits nulls.)  This class makes no guarantees as to
 * the order of the map; in particular, it does not guarantee that the order
 * will remain constant over time.
 *
 * <p>This implementation provides constant-time performance for the basic
 * operations ({@code get} and {@code put}), assuming the hash function
 * disperses the elements properly among the buckets.  Iteration over
 * collection views requires time proportional to the "capacity" of the
 * {@code HashMap} instance (the number of buckets) plus its size (the number
 * of key-value mappings).  Thus, it's very important not to set the initial
 * capacity too high (or the load factor too low) if iteration performance is
 * important.
 *
 * <p>An instance of {@code HashMap} has two parameters that affect its
 * performance: <i>initial capacity</i> and <i>load factor</i>.  The
 * <i>capacity</i> is the number of buckets in the hash table, and the initial
 * capacity is simply the capacity at the time the hash table is created.  The
 * <i>load factor</i> is a measure of how full the hash table is allowed to
 * get before its capacity is automatically increased.  When the number of
 * entries in the hash table exceeds the product of the load factor and the
 * current capacity, the hash table is <i>rehashed</i> (that is, internal data
 * structures are rebuilt) so that the hash table has approximately twice the
 * number of buckets.
 *
 * <p>As a general rule, the default load factor (.75) offers a good
 * tradeoff between time and space costs.  Higher values decrease the
 * space overhead but increase the lookup cost (reflected in most of
 * the operations of the {@code HashMap} class, including
 * {@code get} and {@code put}).  The expected number of entries in
 * the map and its load factor should be taken into account when
 * setting its initial capacity, so as to minimize the number of
 * rehash operations.  If the initial capacity is greater than the
 * maximum number of entries divided by the load factor, no rehash
 * operations will ever occur.
 *
 * <p>If many mappings are to be stored in a {@code HashMap}
 * instance, creating it with a sufficiently large capacity will allow
 * the mappings to be stored more efficiently than letting it perform
 * automatic rehashing as needed to grow the table.  Note that using
 * many keys with the same {@code hashCode()} is a sure way to slow
 * down performance of any hash table. To ameliorate impact, when keys
 * are {@link Comparable}, this class may use comparison order among
 * keys to help break ties.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a hash map concurrently, and at least one of
 * the threads modifies the map structurally, it <i>must</i> be
 * synchronized externally.  (A structural modification is any operation
 * that adds or deletes one or more mappings; merely changing the value
 * associated with a key that an instance already contains is not a
 * structural modification.)  This is typically accomplished by
 * synchronizing on some object that naturally encapsulates the map.
 *
 * If no such object exists, the map should be "wrapped" using the
 * {@link Collections#synchronizedMap Collections.synchronizedMap}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the map:<pre>
 *   Map m = Collections.synchronizedMap(new HashMap(...));</pre>
 *
 * <p>The iterators returned by all of this class's "collection view methods"
 * are <i>fail-fast</i>: if the map is structurally modified at any time after
 * the iterator is created, in any way except through the iterator's own
 * {@code remove} method, the iterator will throw a
 * {@link ConcurrentModificationException}.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the
 * future.
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
public class SerializableBHashMap
{
    /**
     * Constructs an empty {@link SerializableBMap} with the default initial capacity
     * (16) and the default load factor (0.75).
     * <p>
     * The actual implementation is {@link RHashMap}
     */
	@Nonnull
	public static <K, V> SerializableBMap<K, V> create()
	{
		return RHashMap.create();
	}
	

    /**
     * Constructs an empty {@link SerializableBMap} with the specified initial
     * capacity and load factor.
     * <p>
     * The actual implementation is {@link RHashMap}
     *
     * @param  initialCapacity the initial capacity
     * @param  loadFactor      the load factor
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive
     */
	@Nonnull
	public static <K, V> SerializableBMap<K, V> create(int initialCapacity, float loadFactor) 
	{
		return RHashMap.create(initialCapacity, loadFactor);
    }

    /**
     * Constructs an empty {@link SerializableBMap} with the specified initial
     * capacity and the default load factor (0.75).
     * <p>
     * The actual implementation is {@link RHashMap}
     *
     * @param  initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
	@Nonnull
	public static <K, V> SerializableBMap<K, V> create(int initialCapacity) 
	{
		return RHashMap.create(initialCapacity);
    }

    /**
     * Constructs a new {@link SerializableBMap} with the same mappings as the
     * specified {@code Map}.  The {@link SerializableBMap} is created with
     * default load factor (0.75) and an initial capacity sufficient to
     * hold the mappings in the specified {@code Map}.
     * <p>
     * The actual implementation is {@link RHashMap}
     *
     * @param   m the map whose mappings are to be placed in this map
     * @throws  NullPointerException if the specified map is null
     */
	@Nonnull
	public static <K, V> SerializableBMap<K, V> create(@Nonnull Map<? extends K, ? extends V> m) 
	{
		return RHashMap.create(m); 
    }

    /**
     * Constructs a new {@link SerializableBMap} with the same mappings as the
     * specified {@code ReadOnlyMap}.  The {@link SerializableBMap} is created with
     * default load factor (0.75) and an initial capacity sufficient to
     * hold the mappings in the specified {@code ReadOnlyMap}.
     * <p>
     * The actual implementation is {@link RHashMap}
     *
     * @param   m the map whose mappings are to be placed in this map
     * @throws  NullPointerException if the specified map is null
     */
	@Nonnull
	public static <K, V> SerializableBMap<K, V> createFromReadOnly(@Nonnull ReadOnlyMap<? extends K, ? extends V> m) 
	{
		return RHashMap.createFromReadOnly(m); 
    }
	
}
