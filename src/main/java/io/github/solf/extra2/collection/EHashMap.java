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

import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Extended/entry version of {@link BHashMap}; instances are created by various static
 * {@link #create()} factory methods.
 * <p>
 * Adds functionality such as being able to retrieve key & entry objects from the map 
 * (relevant because object being {@link #equals(Object)} doesn't mean that
 * everything about the object is the same).
 * <p>
 * Since such functionality is not available in normal Java maps, standard maps
 * cannot be easily represented as {@link EMap}; therefore this should
 * only be used when actually necessary.
 * 
 * @see EMap
 * @see EReadOnlyMap
 * @see RMap
 * @see RHashMap
 * 
 * @author Sergey Olefir
 */
public class EHashMap
{
    /**
     * Constructs an empty {@link EMap} with the default initial capacity
     * (16) and the default load factor (0.75).
     * <p>
     * The actual implementation is {@link RHashMap}
     */
	@Nonnull
	public static <K, V> RHashMap<K, V> create()
	{
		return new RHashMap<K, V>();
	}
	

    /**
     * Constructs an empty {@link EMap} with the specified initial
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
	public static <K, V> RHashMap<K, V> create(int initialCapacity, float loadFactor) 
	{
		return new RHashMap<K, V>(initialCapacity, loadFactor);
    }

    /**
     * Constructs an empty {@link EMap} with the specified initial
     * capacity and the default load factor (0.75).
     * <p>
     * The actual implementation is {@link RHashMap}
     *
     * @param  initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
	@Nonnull
	public static <K, V> RHashMap<K, V> create(int initialCapacity) 
	{
		return new RHashMap<K, V>(initialCapacity);
    }

    /**
     * Constructs a new {@link EMap} with the same mappings as the
     * specified {@code Map}.  The {@link EMap} is created with
     * default load factor (0.75) and an initial capacity sufficient to
     * hold the mappings in the specified {@code Map}.
     * <p>
     * The actual implementation is {@link RHashMap}
     *
     * @param   m the map whose mappings are to be placed in this map
     * @throws  NullPointerException if the specified map is null
     */
	@Nonnull
	public static <K, V> RHashMap<K, V> create(@Nonnull Map<? extends K, ? extends V> m) 
	{
		return new RHashMap<K, V>(m); 
    }
	
}
