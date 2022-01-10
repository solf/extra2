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
 * Extended/entry version of {@link ReadOnlySet}.
 * <p>
 * Adds functionality such as being able to retrieve element from the set 
 * (relevant because object being {@link #equals(Object)} doesn't mean that
 * everything about the object is the same).
 * <p>
 * Since such functionality is not available in normal Java sets, standard sets
 * cannot be easily represented as {@link EReadOnlySet}; therefore this should
 * only be used when actually necessary.
 * 
 * @see ESet
 * @see EHashSet
 * @see RSet
 * @see RHashSet
 *
 * @author Sergey Olefir
 */
public interface EReadOnlySet<E> extends ReadOnlySet<E>
{
	/**
	 * Since item equality via hashCode/equals doesn't guarantee that items
	 * are fully identical (as an example -- database row representation may use
	 * primary key for equals/hashCode while being completely different in other
	 * columns), this method provides a way to retrieve the actual item stored
	 * in the set.
	 * 
	 * @return matching item stored in the set or null if there is none
	 */
	@Nullable E get(E item);
}
