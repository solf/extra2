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
 * Extended/entry version of {@link BSet}.
 * <p>
 * Adds functionality such as being able to retrieve element from the set 
 * (relevant because object being {@link #equals(Object)} doesn't mean that
 * everything about the object is the same).
 * <p>
 * Since such functionality is not available in normal Java sets, standard sets
 * cannot be easily represented as {@link ESet}; therefore this should
 * only be used when actually necessary.
 * 
 * @see EReadOnlySet
 * @see EHashSet
 * @see RSet
 * @see RHashSet
 * 
 * @author Sergey Olefir
 */
public interface ESet<E> extends BSet<E>, EReadOnlySet<E>
{
    /**
     * Removes the specified element from this set if it is present.
     * More formally, removes an element {@code e} such that
     * {@code Objects.equals(o, e)},
     * if this set contains such an element.
     *
     * @param o object to be removed from this set, if present
     * @return the matching element that was in the set prior to remove or null
     * 		if there was none
     * 
     * @see #removeElement(Object)
     */
    @Nullable
    public E removeAndGet(E o);
}
