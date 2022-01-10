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

import javax.annotation.Nullable;

/**
 * A version of {@link BMap}/{@link EMap} that supports additional operations
 * for replacing values.
 * <p>
 * Since such functionality is not available in normal Java maps, standard maps
 * cannot be easily represented as {@link RMap}; therefore this should
 * only be used when actually necessary.
 * 
 * @see RHashMap
 * 
 * @author Sergey Olefir
 */
public interface RMap<K, V> extends EMap<K, V>
{
	
	/**
	 * @deprecated use {@link #putRetainKey(Object, Object)} for clarity 
	 */
	@Deprecated
	@Override
	public @Nullable V put(K key, V value);
	
    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value AND THE OLD KEY ARE REPLACED.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with {@code key}.)
     */
	public @Nullable V putWithNewKey(K key, V value);
	
    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value is replaced but the OLD KEY IS RETAINED.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with {@code key}.)
     */
	public @Nullable V putRetainKey(K key, V value);
}
