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

import javax.annotation.Nonnull;

/**
 * Read-only operations possible on a collection.
 *
 * @author Sergey Olefir
 */
public interface ReadOnlyCollection<E> extends ForIterable<E>
{
    /**
     * Returns {@code true} if this collection contains the specified element.
     * More formally, returns {@code true} if and only if this collection
     * contains at least one element {@code e} such that
     * {@code Objects.equals(o, e)}.
     *
     * @param o element whose presence in this collection is to be tested
     * @return {@code true} if this collection contains the specified
     *         element
     */
	boolean has(E o);

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * This implementation iterates over the specified collection,
     * checking each element returned by the iterator in turn to see
     * if it's contained in this collection.  If all elements are so
     * contained {@code true} is returned, otherwise {@code false}.
     *
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @see #contains(Object)
     */
    default public boolean hasAll(Collection<E> c) {
        for (E e : c)
            if (!has(e))
                return false;
        return true;
    }

    /**
     * Returns {@code true} if this collection contains no elements.
     *
     * @return {@code true} if this collection contains no elements
     */
	boolean isEmpty();

    /**
     * Returns the number of elements in this collection.  If this collection
     * contains more than {@code Integer.MAX_VALUE} elements, returns
     * {@code Integer.MAX_VALUE}.
     *
     * @return the number of elements in this collection
     */
	int size();

	/**
	 * Returns an unmodifiable representation of this {@link ReadOnlyCollection} as
	 * Java {@link Collection} (e.g. for use with methods that require {@link Collection} as 
	 * input argument).
	 * <p>
	 * Implementations ought to take steps to make this as inexpensive as possible.
	 */
	@Nonnull Collection<E> toUnmodifiableJavaCollection();
}
