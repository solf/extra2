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
package com.github.solf.extra2.collection;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Provides a way to iterate (via {@link ForIterable}) over all the individual 
 * elements contained in a collection of collections (e.g. 
 * to iterate over all AtomicIntegers in Map<String, List<AtomicInteger>>.values()).
 * <p>
 * Use any of the static {@link #of(..)} methods to create a {@link ForIterable}
 * <p>
 * You can specify 'subIterableFunction' that remaps elements of top-level
 * collection (iterable, iterator) to iterable/iterator of the final element
 * type, e.g. Map<String, List<AtomicInteger>>.entrySet() can be remapped to 
 * just its value: entry -> entry.getValue()
 * <p>
 * You can also specify a 'nullSupplier' which is used to produce iterable/iterator
 * when top-level collection (iterable, iterator) contains a null element; if
 * 'nullSupplier' is not specified, then null elements are skipped (treated the
 * same way as they were an empty collection).
 * <p>
 * NOTE: 'nullSupplier' may return null value, which is then treated the same
 * way as empty collection/iterator/iterable.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ForIterableOfIterable<T, C> implements Iterator<T>, ForIterable<T>
{
	/**
	 * Top-level iterator (for the collection-of-collections thing).
	 */
	@NonNull
	@Nonnull
	private final Iterator<C> topIterator;
	
	/**
	 * Function to remap items from the top-level iterator to the iterable of
	 * actual final elements.
	 * <p>
	 * NOTE: function may return null value which is treated the same way as
	 * an empty collection.
	 */
	@NonNull
	@Nonnull
	private final Function<C, @Nullable Iterator<T>> subIteratorFunction;
	
	/**
	 * Supplier to provide iterable of final elements for the case when top-level
	 * collection (iterator, iterable) contains a null element.
	 * <p> 
	 * If not specified, then null elements in top-level collection are treated 
	 * as empty collections.
	 * <p>
	 * If returns null, then the result is also treated as empty collection.
	 */
	@Nullable
	private final Supplier<@Nullable Iterator<T>> nullSupplier;
	
	/**
	 * Current sub-level iterator, next element comes out from this iterator
	 * (if available).
	 */
	@Nonnull
	private Iterator<T> subCollectionIterator = Collections.emptyIterator();  

	/**
	 * Gets {@link ForIterable} for individual elements contained in Iterator-of-Iterators.
	 * <p>
	 * Null elements in top-level iterator are treated as empty iterators.
	 */
	@Nonnull 
	public static <T, C extends Iterator<T>> ForIterable<T> of(@Nonnull Iterator<C> topIterator)
	{
		return of(topIterator, e -> e, null);
	}

	/**
	 * Gets {@link ForIterable} for individual elements contained in 
	 * Iterator-of-iterators and requires specification of remapping function
	 * that remaps top-level iterator elements into iterators of the final value.
	 * <p>
	 * E.g. .of(Map<String, List<AtomicInteger>>.values().iterator(), list -> list.iterator())
	 * <p>
	 * Null elements in top-level iterator are treated as empty iterators.
	 */
	@Nonnull 
	public static <T, C> ForIterable<T> of(@Nonnull Iterator<C> topIterator, @Nonnull Function<C, @Nullable Iterator<T>> subIteratorFunction)
	{
		return of(topIterator, subIteratorFunction, null);
	}


	/**
	 * Gets {@link ForIterable} for individual elements contained in 
	 * Iterator-of-iterators and requires specification of remapping function
	 * that remaps top-level iterator elements into iterators of the final value.
	 * <p>
	 * You can also optionally specify 'nullSupplier' in order to provide special
	 * handling for null values in top-level iterator.
	 * <p>
	 * E.g. .of(Map<String, List<AtomicInteger>>.values().iterator(), list -> list.iterator(), () -> List.of(new AtomicInteger(123).iterator())
	 * <p>
	 * If 'nullSupplier' is not specified, then null elements in top-level 
	 * iterator are treated as empty iterators.
	 */
	@Nonnull 
	public static <T, C> ForIterable<T> of(@Nonnull Iterator<C> topIterator, 
		@Nonnull Function<C, @Nullable Iterator<T>> subIteratorFunction,
		@Nullable Supplier<@Nullable Iterator<T>> nullSupplier)
	{
		return new ForIterableOfIterable<>(topIterator, subIteratorFunction, nullSupplier);
	}


	/**
	 * Gets {@link ForIterable} for individual elements contained in Iterable-of-iterables,
	 * e.g. in Map<String, List<AtomicInteger>>.values()
	 * <p>
	 * Null elements in top-level iterator are treated as empty iterators.
	 */
	@Nonnull 
	public static <T, C extends Iterable<T>> ForIterable<T> of(@Nonnull Iterable<C> topIterable)
	{
		return of(topIterable, e -> e);
	}


	/**
	 * Gets {@link ForIterable} for individual elements contained in 
	 * Iterable-of-iterables and requires specification of remapping function
	 * that remaps top-level iterable elements into iterables of the final value.
	 * <p>
	 * E.g. .of(Map<String, List<AtomicInteger>>.entrySet(), entry -> entry.getValue())
	 * <p>
	 * Null elements in top-level iterator are treated as empty iterators.
	 */
	@Nonnull 
	public static <T, C> ForIterable<T> of(@Nonnull Iterable<C> topIterable, 
		@Nonnull Function<C, @Nullable Iterable<T>> subIterableFunction)
	{
		return of(topIterable, subIterableFunction, null);
	}
	

	/**
	 * Gets {@link ForIterable} for individual elements contained in 
	 * Iterable-of-iterables and requires specification of remapping function
	 * that remaps top-level iterable elements into iterables of the final value.
	 * <p>
	 * You can also optionally specify 'nullSupplier' in order to provide special
	 * handling for null values in top-level iterable.
	 * <p>
	 * E.g. .of(Map<String, List<AtomicInteger>>.entrySet(), entry -> entry.getValue(), () -> List.of(new AtomicInteger(123))
	 * <p>
	 * If 'nullSupplier' is not specified, then null elements in top-level 
	 * iterator are treated as empty iterators.
	 */
	@Nonnull 
	public static <T, C> ForIterable<T> of(@Nonnull Iterable<C> topIterable, 
		@Nonnull Function<C, @Nullable Iterable<T>> subIterableFunction,
		@Nullable Supplier<@Nullable Iterator<T>> nullSupplier)
	{
		return new ForIterableOfIterable<>(topIterable.iterator(), 
			e -> {
				@Nullable Iterable<T> it = subIterableFunction.apply(e);
				return it == null ? null : it.iterator();
			},
			nullSupplier);
	}
	
	@Override
	@Nonnull
	@Deprecated
	public Iterator<T> iterator()
	{
		return this; // this may not be strictly correct, but should work
	}

	@Override
	public boolean hasNext()
	{
		while (true)
		{
			if (subCollectionIterator.hasNext())
				return true;
		
			if (!topIterator.hasNext())
				return false;
			
			{
				C nextSubItem = topIterator.next();
				@Nullable Iterator<T> next;
				if (nextSubItem != null)
				{
					next = subIteratorFunction.apply(nextSubItem);
				}
				else
				{
					Supplier<@Nullable Iterator<T>> ns = nullSupplier;
					next = ns == null ? null : ns.get();
				}
				if (next != null)
					subCollectionIterator = next;
			}
		}
	}

	@Override
	public T next()
	{
		if (!hasNext())
			throw new NoSuchElementException();
		
		return subCollectionIterator.next();
	}
	
	
}
