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

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

import lombok.NonNull;

/**
 * A 'better' version of {@link Set} that provides improved functionality --
 * mainly providing a read-only view of the underlying set via {@link ReadOnlySet}
 * (so e.g. methods can declare that they accept {@link ReadOnlySet}) plus
 * some improved type-checking.
 * <p>
 * See {@link BHashSet#create()} methods for creating instances.
 * <p>
 * This particular interface doesn't provide much additional functionality,
 * however the upside is that it is possible to represent any Java set as {@link BSet}
 * via a very thin wrapper via {@link #of(Set)}
 * <p>
 * See {@link ESet} and {@link RSet} for extended functionality that cannot be
 * done via thin wrapper on a pre-existing set.
 * <p>
 * ---------------------------------------------------------------------
 * <p>
 * A collection that contains no duplicate elements.  More formally, sets
 * contain no pair of elements {@code e1} and {@code e2} such that
 * {@code e1.equals(e2)}, and at most one null element.  As implied by
 * its name, this interface models the mathematical <i>set</i> abstraction.
 *
 * <p>The {@code Set} interface places additional stipulations, beyond those
 * inherited from the {@code Collection} interface, on the contracts of all
 * constructors and on the contracts of the {@code add}, {@code equals} and
 * {@code hashCode} methods.  Declarations for other inherited methods are
 * also included here for convenience.  (The specifications accompanying these
 * declarations have been tailored to the {@code Set} interface, but they do
 * not contain any additional stipulations.)
 *
 * <p>The additional stipulation on constructors is, not surprisingly,
 * that all constructors must create a set that contains no duplicate elements
 * (as defined above).
 *
 * <p>Note: Great care must be exercised if mutable objects are used as set
 * elements.  The behavior of a set is not specified if the value of an object
 * is changed in a manner that affects {@code equals} comparisons while the
 * object is an element in the set.  A special case of this prohibition is
 * that it is not permissible for a set to contain itself as an element.
 *
 * <p>Some set implementations have restrictions on the elements that
 * they may contain.  For example, some implementations prohibit null elements,
 * and some have restrictions on the types of their elements.  Attempting to
 * add an ineligible element throws an unchecked exception, typically
 * {@code NullPointerException} or {@code ClassCastException}.  Attempting
 * to query the presence of an ineligible element may throw an exception,
 * or it may simply return false; some implementations will exhibit the former
 * behavior and some will exhibit the latter.  More generally, attempting an
 * operation on an ineligible element whose completion would not result in
 * the insertion of an ineligible element into the set may throw an
 * exception or it may succeed, at the option of the implementation.
 * Such exceptions are marked as "optional" in the specification for this
 * interface.
 *
 * @author Sergey Olefir
 */
public interface BSet<E> extends ReadOnlySet<E>, Set<E>
{
	/**
	 * Represents any given Java set as {@link SerializableBSet} via a thin
	 * wrapper.
	 * <p>
	 * All operations on the wrapper are pass-through to the underlying set instance.
	 * <p>  
	 * NOTE: whether resulting set is actually serializable depends on whether
	 * underlying set is serializable.
	 * <p>
	 * This has very little performance impact and can be used freely as needed.
	 */
	@Nonnull
	public static <E> BSet<E> of(@Nonnull @NonNull Set<E> setToWrap)
	{
		return SerializableBSet.of(setToWrap);
	}
	
	/**
	 * Creates a new sequential stream for this set.
	 */
	@Override
	default public @Nonnull Stream<E> stream()
	{
		return StreamSupport.stream(spliterator(), false);
	}
	
	/**
	 * Returns a live iterator over the elements contained in this set.
	 * <p>
	 * Live iterator can be used to remove the elements from the set.
	 */
	@Nonnull
    public Iterator<E> liveIterator();

    /**
     * @deprecated use {@link #has(Object)} for better type-checking
     */
	@Override
	@Deprecated
	public boolean contains(Object o);
	
    /**
     * @deprecated use {@link #removeAndGet(Object)} for better type-checking
     */
    @Override
    @Deprecated
    public boolean remove(Object o);
    
    /**
     * Removes the specified element from this set if it is present.
     * More formally, removes an element {@code e} such that
     * {@code Objects.equals(o, e)},
     * if this set contains such an element.
     *
     * @param o object to be removed from this set, if present
     * @return true if this set contained the specified element
     * 
     * @see ESet#removeAndGet(Object)
     */
    public boolean removeElement(E o);
}
