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
 * A version of {@link BSet}/{@link ESet} that supports additional operations
 * for replacing values.
 * <p>
 * Since such functionality is not available in normal Java sets, standard sets
 * cannot be easily represented as {@link RSet}; therefore this should
 * only be used when actually necessary.
 * 
 * @see RHashSet
 * 
 * @author Sergey Olefir
 */
public interface RSet<E> extends ESet<E>
{
	/**
	 * @deprecated use {@link #addIfAbsent(Object)} for semantic clarity or any 
	 * 		of the other add* methods
	 */
	@Deprecated
	@Override
	public boolean add(E e);
	
    /**
     * Adds (or replaces IF it is ALREADY PRESENT) the specified element to this set.
     * <p>
     * Since item equality via hashCode/equals does not guarantee that items are
     * identical otherwise, this method provides a way to actually REPLACE item
     * stored in the set even if a matching elements is already present. 
     *
     * @param e element to be added to this set
     * @return the PREVIOUS value stored in the set or null if there was no
     * 		matching element
     */
    @Nullable
    public E addOrReplace(E e);
    
    
    /**
     * Adds the specified element to this set if it is NOT ALREADY present.
     * More formally, adds the specified element {@code e} to this set if
     * this set contains no element {@code e2} such that
     * {@code Objects.equals(e, e2)}.
     * If this set already contains the element, the call leaves the set
     * unchanged.
     *
     * @param e element to be added to this set
     * @return true if this set did not already contain the specified element
     * 
     * @see #addIfAbsentAndGet(Object)
     */
    public boolean addIfAbsent(E e);
    
    /**
     * Adds the specified element to this set if it is NOT ALREADY present.
     * More formally, adds the specified element {@code e} to this set if
     * this set contains no element {@code e2} such that
     * {@code Objects.equals(e, e2)}.
     * If this set already contains the element, the call leaves the set
     * unchanged.
     *
     * @param e element to be added to this set
     * @return 'previous value': null if there was none (and set was modified);
     * 		existing (=previous) value if set already contains matching element
     * 		(note: this can also be null if set supports null elements)  
     * 
     * @see #addIfAbsentAndGet(Object)
     */
    @Nullable
    public E addIfAbsentAndGetIfPresent(E e);
    
    /**
     * Adds the specified element to this set if it is NOT ALREADY present.
     * More formally, adds the specified element {@code e} to this set if
     * this set contains no element {@code e2} such that
     * {@code Objects.equals(e, e2)}.
     * If this set already contains the element, the call leaves the set
     * unchanged.
     *
     * @param e element to be added to this set
     * @return the CURRENT value stored in the set after the method invocation (either
     * 		the one that was already there before or the new one specified as
     * 		argument)
     * 
     * @see #addIfAbsentAndGetIfPresent(Object)
     */
    @Nullable
    public E addIfAbsentAndGet(E e);
}
