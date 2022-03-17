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

import org.eclipse.jdt.annotation.NonNullByDefault;

import lombok.NonNull;

/**
 * A version of {@link AutoCloseable} that doesn't throw checked exceptions
 * in the {@link #close()} method.
 * <p>
 * See {@link #wrap(AutoCloseable)} method for wrappers.
 * <p>
 * An object that may hold resources (such as file or socket handles)
 * until it is closed. The {@link #close()} method of an {@code AutoCloseable}
 * object is called automatically when exiting a {@code
 * try}-with-resources block for which the object has been declared in
 * the resource specification header. This construction ensures prompt
 * release, avoiding resource exhaustion exceptions and errors that
 * may otherwise occur.
 *
 * @apiNote
 * <p>It is possible, and in fact common, for a base class to
 * implement AutoCloseable even though not all of its subclasses or
 * instances will hold releasable resources.  For code that must operate
 * in complete generality, or when it is known that the {@code AutoCloseable}
 * instance requires resource release, it is recommended to use {@code
 * try}-with-resources constructions. However, when using facilities such as
 * {@link java.util.stream.Stream} that support both I/O-based and
 * non-I/O-based forms, {@code try}-with-resources blocks are in
 * general unnecessary when using non-I/O-based forms.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public interface AutoCloseableUnchecked extends AutoCloseable
{
	@Override
	void close();
	
	/**
	 * Wraps given {@link AutoCloseable} into {@link AutoCloseableUnchecked}
	 * <p>
	 * Any checked exceptions from {@link AutoCloseable#close()} are re-thrown as
	 * unchecked {@link RuntimeException} instead; any unchecked exceptions are
	 * thrown 'as is'.
	 */
	public static AutoCloseableUnchecked wrap(@NonNull AutoCloseable autoCloseable)
	{
		return new AutoCloseableUncheckedWrapper(autoCloseable);
	}
}
