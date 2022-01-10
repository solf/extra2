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
 * Extended/entry version of {@link ReadOnlyMap}.
 * <p>
 * Adds functionality such as being able to retrieve key & entry objects from the map 
 * (relevant because object being {@link #equals(Object)} doesn't mean that
 * everything about the object is the same).
 * <p>
 * Since such functionality is not available in normal Java maps, standard maps
 * cannot be easily represented as {@link EReadOnlyMap}; therefore this should
 * only be used when actually necessary.
 * 
 * @see EMap
 * @see EHashMap
 * @see RMap
 * @see RHashMap
 * 
 *
 * @author Sergey Olefir
 */
public interface EReadOnlyMap<K, V> extends ReadOnlyMap<K, V>
{
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
}
