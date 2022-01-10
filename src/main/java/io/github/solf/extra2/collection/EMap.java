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

import javax.annotation.Nullable;

/**
 * Extended/entry version of {@link BMap}.
 * <p>
 * Adds functionality such as being able to retrieve key & entry objects from the map 
 * (relevant because object being {@link #equals(Object)} doesn't mean that
 * everything about the object is the same).
 * <p>
 * Since such functionality is not available in normal Java maps, standard maps
 * cannot be easily represented as {@link EMap}; therefore this should
 * only be used when actually necessary.
 * 
 * @see EReadOnlyMap
 * @see EHashMap
 * @see RMap
 * @see RHashMap
 * 
 * @author Sergey Olefir
 */
public interface EMap<K, V> extends BMap<K, V>, EReadOnlyMap<K, V>
{
	/**
	 * Gets a live view (can change value) of the entry matching the given key 
	 * (including the actual stored key instance).
	 * <p>
	 * This acknowledges the fact that key equality via hashCode/equals doesn't
	 * mean that keys are identical, therefore the actual stored key instance
	 * is returned.
	 * 
	 * @return entry matching the key or null if there are none
	 */
	public Map.@Nullable Entry<K, V> getLiveEntry(K key);
}
