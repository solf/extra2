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
package com.github.solf.extra2.collection;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Read-only operations possible on a map.
 *
 * @author Sergey Olefir
 */
public interface ReadOnlyMap<K, V> extends ForIterable<@Nonnull ReadOnlyEntry<K, V>>
{
    /**
     * Exactly like {@link Map#getOrDefault(Object, Object)} except type-checks
     * the key.
     * <p>
     * Returns the value to which the specified key is mapped, or
     * {@code defaultValue} if this map contains no mapping for the key.
     * <p>
     * The default value is NOT added to the {@link Map}!
     *
     * @param key the key whose associated value is to be returned
     * @param defaultValue the default mapping of the key
     * @return the value to which the specified key is mapped, or
     * {@code defaultValue} if this map contains no mapping for the key
     */
	V getOrFallback(K key, V defaultValue);

	/**
	 * Returns {@link ForIterable} view of all values contained in the map; this
	 * is only intended to be used with for (var value : map.values()) kind of loops.
	 * <p>
	 * Attempts to modify contents via underlying iterator's remove method lead
	 * to undefined results. 
	 */
	@Nonnull ForIterable<V> vals();

	/**
	 * Returns {@link ForIterable} view of all keys contained in the map; this
	 * is only intended to be used with for (var key : map.keys()) kind of loops.
	 * <p>
	 * Attempts to modify contents via underlying iterator's remove method lead
	 * to undefined results. 
	 */
	@Nonnull ForIterable<K> keys();

	/**
	 * Returns {@link ForIterable} view of all entries contained in the map; this
	 * is only intended to be used with for (var entry : map.entries()) kind of loops.
	 * <p>
	 * Attempts to modify contents via underlying iterator's remove method lead
	 * to undefined results.
	 * <p>
	 * NOTE: the same result can be achieved simply by iterating over the map
	 * instance, e.g.: for (var entry : map)
	 */
	@Nonnull ForIterable<@Nonnull ReadOnlyEntry<K, V>> entries();

	/**
	 * Gets a read-only view of the entry matching the given key (including the
	 * actual stored key instance).
	 * <p>
	 * This acknowledges the fact that key equality via hashCode/equals doesn't
	 * mean that keys are identical, therefore the actual stored key instance
	 * is returned.
	 * 
	 * @return entry matching the key or null if there are none
	 */
	@Nullable ReadOnlyEntry<K, V> getEntry(K key);

	/**
	 * Gets the current key instance stored in the map that matches the provided
	 * key or null of there's none.
	 * <p>
	 * Since key equality via hashCode/equals doesn't guarantee that items
	 * are fully identical (as an example -- database row representation may use
	 * primary key for equals/hashCode while being completely different in other
	 * columns), therefore this method provides a way to retrieve the actual key 
	 * stored in the map.
	 * 
	 * @return matching key stored in the map or null if there is none
	 */
	@Nullable K getKey(K key);

    /**
     * Exactly equivalent to {@link Map#get(Object)} but with better type-checking.
     * <p>
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
     * key.equals(k))}, then this method returns {@code v}; otherwise
     * it returns {@code null}.  (There can be at most one such mapping.)
     *
     * <p>A return value of {@code null} does not <i>necessarily</i>
     * indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@link #hasKey(Object)} operation may be used to
     * distinguish these two cases.
     */
	@Nullable V getValue(K key);

    /**
     * Exactly equivalent to {@link Map#containsKey(Object)} but with better type-checking.
     * <p>
     * Returns {@code true} if this map contains a mapping for the
     * specified key.
     *
     * @param   key   The key whose presence in this map is to be tested
     * @return {@code true} if this map contains a mapping for the specified
     * key.
     */
	boolean hasKey(K key);

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     *
     * @return {@code true} if this map contains no key-value mappings
     */
	boolean isEmpty();

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
	int size();
	

	/**
	 * Returns an unmodifiable representation of this {@link ReadOnlyMap} as
	 * Java {@link Map} (e.g. for use with methods that require {@link Map} as 
	 * input argument).
	 * <p>
	 * Implementations ought to take steps to make this as inexpensive as possible.
	 */
	@Nonnull Map<K, V> toUnmodifiableJavaMap();

	
	/**
	 * Returns empty read-only map.
	 */
	@Nonnull
	public static <K, V> ReadOnlyMap<K, V> emptyReadOnlyMap()
	{
		return WACollections.emptyReadOnlyMap();
	}
}
