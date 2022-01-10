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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.NonNull;

/**
 * A 'better' version of {@link Map} that provides improved functionality:
 * - a read-only view of the underlying map via {@link ReadOnlyMap}
 * (so e.g. methods can declare that they accept {@link ReadOnlyMap});
 * - improved/clarified functionality such as {@link #getOrCreateValueNonNull(Object, Function)}
 * and {@link #putIfNoKey(Object, Object)}/{@link #putIfNoValue(Object, Object)};
 * - plus some improved type-checking as in {@link #removeAndGet(Object)}
 * <p>
 * See {@link BHashMap#create()} methods for creating instances.
 * <p>
 * This particular interface has limited additional functionality,
 * however the upside is that it is possible to represent any Java map as {@link BMap}
 * via a thin wrapper via {@link #of(Map)}
 * <p>
 * See {@link EMap} and {@link RMap} for extended functionality that cannot be
 * done via thin wrapper on a pre-existing map.
 * <p>
 * ---------------------------------------------------------------------
 * <p>
 * An object that maps keys to values.  A map cannot contain duplicate keys;
 * each key can map to at most one value.
 *
 * <p>This interface takes the place of the {@code Dictionary} class, which
 * was a totally abstract class rather than an interface.
 *
 * <p>The {@code Map} interface provides three <i>collection views</i>, which
 * allow a map's contents to be viewed as a set of keys, collection of values,
 * or set of key-value mappings.  The <i>order</i> of a map is defined as
 * the order in which the iterators on the map's collection views return their
 * elements.  Some map implementations, like the {@code TreeMap} class, make
 * specific guarantees as to their order; others, like the {@code HashMap}
 * class, do not.
 *
 * <p>Note: great care must be exercised if mutable objects are used as map
 * keys.  The behavior of a map is not specified if the value of an object is
 * changed in a manner that affects {@code equals} comparisons while the
 * object is a key in the map.  A special case of this prohibition is that it
 * is not permissible for a map to contain itself as a key.  While it is
 * permissible for a map to contain itself as a value, extreme caution is
 * advised: the {@code equals} and {@code hashCode} methods are no longer
 * well defined on such a map.
 *
 * <p>All general-purpose map implementation classes should provide two
 * "standard" constructors: a void (no arguments) constructor which creates an
 * empty map, and a constructor with a single argument of type {@code Map},
 * which creates a new map with the same key-value mappings as its argument.
 * In effect, the latter constructor allows the user to copy any map,
 * producing an equivalent map of the desired class.  There is no way to
 * enforce this recommendation (as interfaces cannot contain constructors) but
 * all of the general-purpose map implementations in the JDK comply.
 *
 * <p>The "destructive" methods contained in this interface, that is, the
 * methods that modify the map on which they operate, are specified to throw
 * {@code UnsupportedOperationException} if this map does not support the
 * operation.  If this is the case, these methods may, but are not required
 * to, throw an {@code UnsupportedOperationException} if the invocation would
 * have no effect on the map.  For example, invoking the {@link #putAll(Map)}
 * method on an unmodifiable map may, but is not required to, throw the
 * exception if the map whose mappings are to be "superimposed" is empty.
 *
 * <p>Some map implementations have restrictions on the keys and values they
 * may contain.  For example, some implementations prohibit null keys and
 * values, and some have restrictions on the types of their keys.  Attempting
 * to insert an ineligible key or value throws an unchecked exception,
 * typically {@code NullPointerException} or {@code ClassCastException}.
 * Attempting to query the presence of an ineligible key or value may throw an
 * exception, or it may simply return false; some implementations will exhibit
 * the former behavior and some will exhibit the latter.  More generally,
 * attempting an operation on an ineligible key or value whose completion
 * would not result in the insertion of an ineligible element into the map may
 * throw an exception or it may succeed, at the option of the implementation.
 * Such exceptions are marked as "optional" in the specification for this
 * interface.
 *
 * <p>Many methods in Collections Framework interfaces are defined
 * in terms of the {@link Object#equals(Object) equals} method.  For
 * example, the specification for the {@link #containsKey(Object)
 * containsKey(Object key)} method says: "returns {@code true} if and
 * only if this map contains a mapping for a key {@code k} such that
 * {@code (key==null ? k==null : key.equals(k))}." This specification should
 * <i>not</i> be construed to imply that invoking {@code Map.containsKey}
 * with a non-null argument {@code key} will cause {@code key.equals(k)} to
 * be invoked for any key {@code k}.  Implementations are free to
 * implement optimizations whereby the {@code equals} invocation is avoided,
 * for example, by first comparing the hash codes of the two keys.  (The
 * {@link Object#hashCode()} specification guarantees that two objects with
 * unequal hash codes cannot be equal.)  More generally, implementations of
 * the various Collections Framework interfaces are free to take advantage of
 * the specified behavior of underlying {@link Object} methods wherever the
 * implementor deems it appropriate.
 *
 * <p>Some map operations which perform recursive traversal of the map may fail
 * with an exception for self-referential instances where the map directly or
 * indirectly contains itself. This includes the {@code clone()},
 * {@code equals()}, {@code hashCode()} and {@code toString()} methods.
 * Implementations may optionally handle the self-referential scenario, however
 * most current implementations do not do so.
 *
 * @author Sergey Olefir
 */
public interface BMap<K, V> extends ReadOnlyMap<K, V>, Map<K, V>
{
	/**
	 * Represents any given Java map as {@link SerializableBMap} via a thin
	 * wrapper.
	 * <p>
	 * All operations on the wrapper are pass-through to the underlying map instance.
	 * <p>  
	 * NOTE: whether resulting map is actually serializable depends on whether
	 * underlying map is serializable.
	 * <p>
	 * The performance impact of using this is very similar to using {@link Collections#unmodifiableMap(Map)} --
	 * specifically any entry iteration over the map results in a creation of 
	 * intermediate wrapper object ({@link ReadOnlyEntry}) for each entry iterated.
	 */
	@Nonnull
	public static <K, V> SerializableBMap<K, V> of(@Nonnull @NonNull Map<K, V> mapToWrap)
	{
		return new WrapperBMap<>(mapToWrap);
	}
	
	/**
	 * @deprecated use {@link #hasKey(Object)} for better type-checking
	 */
	@Deprecated
	@Override
	boolean containsKey(Object key);
	
	/**
	 * @deprecated this is highly inefficient, consider adjusting your model
	 */
	@Deprecated
	@Override
	boolean containsValue(Object value);


	/**
	 * @deprecated use {@link #getValue(Object)} for better type-checking
	 */
	@Deprecated
	@Override
	public @Nullable V get(Object key);
	
	/**
	 * If map already contains value mapped to the given key, then that value
	 * is returned; otherwise the provided producer is invoked and the resulting
	 * key-value pair is entered into the map (and the produced value is returned).
	 * 
	 * @return current value associated with the key after this method processing;
	 * 		this is either a pre-existing value or the value created by the producer
	 */
	public V getOrCreateValue(K key, @Nonnull Function<? super K, ? extends V> producer);
	
	/**
	 * If map already contains NON NULL value mapped to the given key, then that 
	 * value is returned; otherwise the provided producer is invoked and the resulting
	 * NON-NULL value is entered into the map (and the produced value is returned).
	 * <p>
	 * Given producer MUST return NON NULL value; otherwise {@link NullPointerException}
	 * is thrown and entry is removed from the map.
	 * 
	 * @return current value associated with the key after this method processing;
	 * 		this is either a pre-existing value or the value created by the producer
	 * 
	 * @throws NullPointerException if producer produces a null value
	 */
	public V getOrCreateValueNonNull(K key, @Nonnull Function<? super K, @Nonnull ? extends V> producer) throws NullPointerException;
	
	/**
	 * A live-view iterator for entries in this map that is backed by the map
	 * itself.
	 * <p>
	 * Items removed via {@link Iterator#remove()} are removed from the map itself.
	 * <p>
	 * It is also possible to change values via {@link Map.Entry#setValue(Object)}
	 */
	@Nonnull
	public Iterator<Map.@Nonnull Entry<K, V>> liveEntries();
	
	/**
	 * A live-view iterator for keys in this map that is backed by the map
	 * itself.
	 * <p>
	 * Items removed via {@link Iterator#remove()} are removed from the map itself.
	 */
	@Nonnull
	public Iterator<K> liveKeys();
	
	/**
	 * A live-view iterator for values in this map that is backed by the map
	 * itself.
	 * <p>
	 * Items removed via {@link Iterator#remove()} are removed from the map itself.
	 */
	@Nonnull
	public Iterator<V> liveVals();
	
    /**
     * Exactly equivalent to {@link Map#remove(Object)} but with better type-checking.
     * <p>
     * Removes the mapping for a key from this map if it is present
     * (optional operation).   More formally, if this map contains a mapping
     * from key {@code k} to value {@code v} such that
     * {@code Objects.equals(key, k)}, that mapping
     * is removed.  (The map can contain at most one such mapping.)
     *
     * <p>Returns the value to which this map previously associated the key,
     * or {@code null} if the map contained no mapping for the key.
     *
     * <p>If this map permits null values, then a return value of
     * {@code null} does not <i>necessarily</i> indicate that the map
     * contained no mapping for the key; it's also possible that the map
     * explicitly mapped the key to {@code null}.
     *
     * <p>The map will not contain a mapping for the specified key once the
     * call returns.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}.
     * @throws UnsupportedOperationException if the {@code remove} operation
     *         is not supported by this map
     * @throws ClassCastException if the key is of an inappropriate type for
     *         this map
     * (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key is null and this
     *         map does not permit null keys
     * (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     */
	public @Nullable V removeAndGet(K key);
	
	/**
	 * @deprecated use {@link #removeAndGet(Object)} for better type-checking
	 */
	@Deprecated
	@Override
	public @Nullable V remove(Object key);

    /**
     * @deprecated use {@link #keys()} or {@link #liveKeys()}
     */
    @Deprecated
	@Override
	public @Nonnull Set<K> keySet();
    
    /**
     * @deprecated use {@link #vals()} or {@link #liveVals()}
     */
    @Deprecated
	@Override
	public @Nonnull Collection<V> values();
    
    /**
     * @deprecated use instance itself (for (var entry : rMap) ...) or {@link #entries()} or {@link #liveEntries()}
     */
    @Deprecated
	@Override
	public @Nonnull Set<Map.@Nonnull Entry<K, V>> entrySet();

	/**
	 * @deprecated use {@link #getOrFallback(Object, Object)} for better type-checking
	 */
	@Deprecated
	@Override
	public V getOrDefault(Object key, V defaultValue);

    /**
     * Exactly equivalent to {@link Map#remove(Object, Object)} but with better type-checking.
     * <p>
     * Removes the entry for the specified key only if it is currently
     * mapped to the specified value.
     *
     * @param key key with which the specified value is associated
     * @param value value expected to be associated with the specified key
     * @return {@code true} if the value was removed
     * @throws UnsupportedOperationException if the {@code remove} operation
     *         is not supported by this map
     *         (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws ClassCastException if the key or value is of an inappropriate
     *         type for this map
     *         (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key or value is null,
     *         and this map does not permit null keys or values
     *         (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @since 1.8
     */
	public boolean removeIfValue(K key, V value);
	
	/**
	 * @deprecated use {@link #removeIfValue(Object, Object)} for better type-checking
	 */
	@Deprecated
	@Override
	public boolean remove(Object key, Object value);

	/**
	 * @deprecated this method has strange semantics, use {@link #putIfNoValue(Object, Object)}
	 * 		instead or possibly {@link #putIfNoKey(Object, Object)} if you need
	 * 		different semantics
	 */
	@Deprecated
	@Override
	public @Nullable V putIfAbsent(K key, V value);
	
    /**
     * If the specified key is not already associated with a value (there is no
     * corresponding mapping or the mapping's value is null) associates it with 
     * the given value and returns {@code null}, else returns the current value.
     * <p>
     * See also {@link #putIfNoKey(Object, Object)} which does NOT modify the
     * map if key is mapped to a null value.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with the key,
     *         if the implementation supports null values.)
     * @throws ClassCastException if the key or value is of an inappropriate
     *         type for this map
     *         (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key or value is null,
     *         and this map does not permit null keys or values
     *         (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws IllegalArgumentException if some property of the specified key
     *         or value prevents it from being stored in this map
     *         (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @since 1.8
     */
	public @Nullable V putIfNoValue(K key, V value);
	
    /**
     * If the specified key is not already in the map, associates it with 
     * the given value and returns {@code null}, else returns the current value
     * (which can also be null if map supports null values).
     * <p>
     * NOTE: unlike {@link #putIfNoValue(Object, Object)} does not modify the
     * map if key is mapped to a null value.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with the key,
     *         if the implementation supports null values.)
     * @throws ClassCastException if the key or value is of an inappropriate
     *         type for this map
     *         (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key or value is null,
     *         and this map does not permit null keys or values
     *         (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws IllegalArgumentException if some property of the specified key
     *         or value prevents it from being stored in this map
     *         (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @since 1.8
     */
	public @Nullable V putIfNoKey(K key, V value);
}
