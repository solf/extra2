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

import java.util.Enumeration;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

/**
 * This is the iterable that is ONLY intended for use with 'for (var item : collection)'
 * constructs or .stream() (i.e. you're not supposed to use underlying iterator directly).
 * <p>
 * This can also be called an ImmutableIterable or ReadOnlyIterable -- i.e.
 * enclosed iterator is not supposed to be used for modifications (via {@link Iterator#remove()})
 * and may throw exceptions in this case or produce undefined results.
 *
 * @author Sergey Olefir
 */
public interface ForIterable<T> extends Iterable<T>
{
    /**
     * @deprecated should not be used directly as per interface contract; method
     * 		is only present for use with 'for (var item : collection)' constructs;
     * 		use {@link #enumeration()} if you need some kind of iterator
     */
	@Deprecated
	@Override
	@Nonnull
    public Iterator<T> iterator();
	
	/**
	 * If the {@link ForIterable} contains exactly one element, returns that element; 
	 * otherwise throws exception.
	 * <p>
	 * This might be useful in e.g. testing.
	 * 
	 * @throws IllegalStateException if {@link ForIterable} doesn't contain exactly one element
	 */
	default T only() throws IllegalStateException
	{
		Iterator<T> iter = iterator();
		
		if (!iter.hasNext())
			throw new IllegalStateException("Iterable has no elements.");
		
		T result = iter.next();

		if (iter.hasNext())
			throw new IllegalStateException("Iterable has more than one element.");
		
		return result;
	}
	
	/**
	 * If the {@link ForIterable} contains at least one element, returns one 
	 * element at random; otherwise throws exception.
	 * <p>
	 * This might be useful in e.g. testing.
	 * 
	 * @throws IllegalStateException if {@link ForIterable} doesn't contain any elements
	 */
	default T any() throws IllegalStateException
	{
		Iterator<T> iter = iterator();
		
		if (!iter.hasNext())
			throw new IllegalStateException("Iterable has no elements.");
		
		return iter.next();
	}
	
	/**
	 * Gets {@link Enumeration} for this iterable -- unlike Iterable/Iterator,
	 * Enumeration doesn't provide a way to modify underlying collection.
	 */
	@Nonnull
	default public Enumeration<T> enumeration()
	{
		@Nonnull Iterator<T> iter = iterator();
		
		return new Enumeration<T>()
		{
			@Override
			public boolean hasMoreElements()
			{
				return iter.hasNext();
			}

			@Override
			public T nextElement()
			{
				return iter.next();
			}
		};
	}
	
	/**
	 * Creates an unmodifiable (remove() throws exception) {@link ForIterable}
	 * for the given {@link Iterable}.
	 * <p>
	 * NOTE: if argument is already {@link ForIterable}, then it is returned
	 * unmodified and may still allow {@link Iterator#remove()}
	 */
	@Nonnull
	public static <T> ForIterable<T> of(final @Nonnull Iterable<T> src)
	{
		if (src instanceof ForIterable)
			return (ForIterable<T>)src;
		
		return new ForIterable<T>()
		{
			@SuppressWarnings("deprecation")
			@Override
			public @Nonnull Iterator<T> iterator()
			{
				final Iterator<T> iter = src.iterator(); 
				return new Iterator<T>() // default Iterator doesn't support remove -- which is the goal here. 
				{

					@Override
					public boolean hasNext()
					{
						return iter.hasNext();
					}

					@Override
					public T next()
					{
						return iter.next();
					}
				};
			}
		};
	}
	
	
	/**
	 * Creates an unmodifiable (remove() throws exception) {@link ForIterable}
	 * for the given {@link Iterator}.
	 */
	@Nonnull
	public static <T> ForIterable<T> of(final @Nonnull Iterator<T> iterator)
	{
		return new ForIterable<T>()
		{
			@SuppressWarnings("deprecation")
			@Override
			public @Nonnull Iterator<T> iterator()
			{
				return new Iterator<T>() // default Iterator doesn't support remove -- which is the goal here. 
				{

					@Override
					public boolean hasNext()
					{
						return iterator.hasNext();
					}

					@Override
					public T next()
					{
						return iterator.next();
					}
				};
			}
		};
	}
	
	
	/**
	 * Creates a new sequential stream for this set.
	 */
	default public @Nonnull Stream<T> stream()
	{
		return StreamSupport.stream(spliterator(), false);
	}
}
