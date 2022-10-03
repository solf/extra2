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
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

import lombok.NonNull;

/**
 * A 'better' version of {@link List} that provides improved functionality --
 * mainly providing a read-only view of the underlying list via {@link ReadOnlyList}
 * (so e.g. methods can declare that they accept {@link ReadOnlyList}) plus
 * some improved type-checking.
 * <p>
 * See {@link BArrayList#create()} methods for creating instances.
 * <p>
 * This particular interface doesn't provide much additional functionality,
 * however the upside is that it is possible to represent any Java list as {@link BList}
 * via a very thin wrapper via {@link #of(List)}
 * <p>
 * ---------------------------------------------------------------------
 * An ordered collection (also known as a <i>sequence</i>).  The user of this
 * interface has precise control over where in the list each element is
 * inserted.  The user can access elements by their integer index (position in
 * the list), and search for elements in the list.<p>
 *
 * Unlike sets, lists typically allow duplicate elements.  More formally,
 * lists typically allow pairs of elements {@code e1} and {@code e2}
 * such that {@code e1.equals(e2)}, and they typically allow multiple
 * null elements if they allow null elements at all.  It is not inconceivable
 * that someone might wish to implement a list that prohibits duplicates, by
 * throwing runtime exceptions when the user attempts to insert them, but we
 * expect this usage to be rare.<p>
 *
 * The {@code List} interface places additional stipulations, beyond those
 * specified in the {@code Collection} interface, on the contracts of the
 * {@code iterator}, {@code add}, {@code remove}, {@code equals}, and
 * {@code hashCode} methods.  Declarations for other inherited methods are
 * also included here for convenience.<p>
 *
 * The {@code List} interface provides four methods for positional (indexed)
 * access to list elements.  Lists (like Java arrays) are zero based.  Note
 * that these operations may execute in time proportional to the index value
 * for some implementations (the {@code LinkedList} class, for
 * example). Thus, iterating over the elements in a list is typically
 * preferable to indexing through it if the caller does not know the
 * implementation.<p>
 *
 * The {@code List} interface provides a special iterator, called a
 * {@code ListIterator}, that allows element insertion and replacement, and
 * bidirectional access in addition to the normal operations that the
 * {@code Iterator} interface provides.  A method is provided to obtain a
 * list iterator that starts at a specified position in the list.<p>
 *
 * The {@code List} interface provides two methods to search for a specified
 * object.  From a performance standpoint, these methods should be used with
 * caution.  In many implementations they will perform costly linear
 * searches.<p>
 *
 * The {@code List} interface provides two methods to efficiently insert and
 * remove multiple elements at an arbitrary point in the list.<p>
 *
 * Note: While it is permissible for lists to contain themselves as elements,
 * extreme caution is advised: the {@code equals} and {@code hashCode}
 * methods are no longer well defined on such a list.
 *
 * <p>Some list implementations have restrictions on the elements that
 * they may contain.  For example, some implementations prohibit null elements,
 * and some have restrictions on the types of their elements.  Attempting to
 * add an ineligible element throws an unchecked exception, typically
 * {@code NullPointerException} or {@code ClassCastException}.  Attempting
 * to query the presence of an ineligible element may throw an exception,
 * or it may simply return false; some implementations will exhibit the former
 * behavior and some will exhibit the latter.  More generally, attempting an
 * operation on an ineligible element whose completion would not result in
 * the insertion of an ineligible element into the list may throw an
 * exception or it may succeed, at the option of the implementation.
 * Such exceptions are marked as "optional" in the specification for this
 * interface.
 *
 * @param <E> the type of elements in this list
 *
 * @author Sergey Olefir
 */
public interface BList<E> extends ReadOnlyList<E>, List<E>
{
	/**
	 * Represents any given Java list as {@link SerializableBList} via a thin
	 * wrapper.
	 * <p>
	 * All operations on the wrapper are pass-through to the underlying list instance.
	 * <p>  
	 * NOTE: whether resulting list is actually serializable depends on whether
	 * underlying list is serializable.
	 * <p>
	 * This has very little performance impact and can be used freely as needed.
	 */
	@Nonnull
	public static <E> BList<E> of(@Nonnull @NonNull List<E> listToWrap)
	{
		return SerializableBList.of(listToWrap);
	}
	
	/**
	 * Creates a new sequential stream for this list.
	 */
	@Override
	default public @Nonnull Stream<E> stream()
	{
		return StreamSupport.stream(spliterator(), false);
	}
	
	/**
	 * Returns a live iterator over the elements contained in this list.
	 * <p>
	 * Live iterator can be used to remove the elements from the list.
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
     * @deprecated use {@link #hasAll(Collection)} for better type-checking
     */
	@Deprecated
    @Override
	boolean containsAll(Collection<?> c);

	/**
     * @deprecated use {@link #removeElement(Object)} for better type-checking
     */
    @Override
    @Deprecated
    public boolean remove(Object o);

    /**
     * @deprecated use {@link #indexOfElement(Object)} instead for better type-checking.
     */
    @Override
    @Deprecated
    int indexOf(Object o);

    /**
     * @deprecated use {@link #lastIndexOfElement(Object)} instead for better type-checking.
     */
    @Override
    @Deprecated
    int lastIndexOf(Object o);
    
    /**
     * Removes the first occurrence of the specified element from this list,
     * if it is present (optional operation).  If this list does not contain
     * the element, it is unchanged.  More formally, removes the element with
     * the lowest index {@code i} such that
     * {@code Objects.equals(o, get(i))}
     * (if such an element exists).  Returns {@code true} if this list
     * contained the specified element (or equivalently, if this list changed
     * as a result of the call).
     *
     * @param o element to be removed from this list, if present
     * @return {@code true} if this list contained the specified element
     */
    public boolean removeElement(E o);

	@Override
	@Nonnull BList<E> subList(int fromIndex, int toIndex);
}
