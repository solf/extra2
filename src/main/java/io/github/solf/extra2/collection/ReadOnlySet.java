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

import java.util.Set;

import javax.annotation.Nonnull;

import lombok.NonNull;

/**
 * Read-only operations possible on a set.
 *
 * @author Sergey Olefir
 */
public interface ReadOnlySet<E> extends ForIterable<E>
{
	/**
	 * Represents any given Java set as {@link ReadOnlySet} via a thin
	 * wrapper.
	 * <p>
	 * All operations on the wrapper are pass-through to the underlying set instance.
	 * <p>
	 * This has very little performance impact and can be used freely as needed.
	 */
	@Nonnull
	public static <E> ReadOnlySet<E> of(@Nonnull @NonNull Set<E> setToWrap)
	{
		return BSet.of(setToWrap);
	}
	
    /**
     * Returns {@code true} if this set contains the specified element.
     * More formally, returns {@code true} if and only if this set
     * contains an element {@code e} such that
     * {@code Objects.equals(o, e)}.
     *
     * @param o element whose presence in this set is to be tested
     * @return {@code true} if this set contains the specified element
     */
	boolean has(E o);

    /**
     * Returns {@code true} if this set contains no elements.
     *
     * @return {@code true} if this set contains no elements
     */
	boolean isEmpty();

    /**
     * Returns the number of elements in this set (its cardinality).
     *
     * @return the number of elements in this set (its cardinality)
     */
	int size();

	/**
	 * Returns an unmodifiable representation of this {@link ReadOnlySet} as
	 * Java {@link Set} (e.g. for use with methods that require {@link Set} as 
	 * input argument).
	 * <p>
	 * Implementations ought to take steps to make this as inexpensive as possible.
	 */
	@Nonnull Set<E> toUnmodifiableJavaSet();
	
	/**
	 * Returns empty read-only set.
	 */
	@Nonnull
	public static <E> ReadOnlySet<E> emptyReadOnlySet()
	{
		return WACollections.emptyReadOnlySet();
	}
}
