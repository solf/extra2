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
package io.github.solf.extra2.io;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Represents a collection of {@link AutoCloseable} instances as a single
 * {@link AutoCloseable}.
 * <p>
 * See factory methods for creating instances: {@link #of(Collection)}, {@link #of(AutoCloseable...)}
 * <p>
 * When closing, an attempt is made to sequentially close all the underlying
 * {@link AutoCloseable}s; exceptions don't prevent attempts to close remaining
 * {@link AutoCloseable}s; if any exceptions occur during the process, the last
 * one is thrown by {@link #close()}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class AutoCloseables implements AutoCloseable
{
	/**
	 * Wraps a collection of {@link AutoCloseable}s into a single {@link AutoCloseable}.
	 */
	public static AutoCloseable of(Collection<AutoCloseable> wrappedCloseables)
	{
		return new AutoCloseables(wrappedCloseables);
	}
	
	/**
	 * Wraps a collection of {@link AutoCloseable}s into a single {@link AutoCloseable}.
	 */
	public static AutoCloseable of(AutoCloseable... wrappedCloseables)
	{
		return of(Arrays.asList(wrappedCloseables));
	}
	
	/**
	 * Closeables wrapped in this aggregate.
	 */
	@Getter
	@NonNull // validated in constructor
	private final Collection<AutoCloseable> wrappedCloseables;

	@Override
	public void close()
		throws Exception
	{
		Exception exc = null;
		
		for (AutoCloseable closeable : wrappedCloseables)
		{
			try
			{
				closeable.close();
			} catch (Exception e)
			{
				exc = e;
			}
		}
		
		if (exc != null)
			throw exc;
	}
}
