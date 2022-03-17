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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * A wrapper for {@link AutoCloseable} that makes it into {@link AutoCloseableUnchecked}
 * <p>
 * See {@link AutoCloseableUnchecked#wrap(AutoCloseable)} for creating wrappers.
 * <p>
 * Any checked exceptions from {@link AutoCloseable#close()} are re-thrown as
 * unchecked {@link RuntimeException} instead; any unchecked exceptions are
 * thrown 'as is'.
 * 
 * @see AutoCloseableUnchecked#wrap(AutoCloseable)
 * 
 * @author Sergey Olefir
 */
@RequiredArgsConstructor
/*package*/ class AutoCloseableUncheckedWrapper implements AutoCloseableUnchecked
{
	/**
	 * Wrapped {@link AutoCloseable}
	 */
	@Getter
	@NonNull
	private final AutoCloseable wrappedAutoCloseable;

	@Override
	public void close()
	{
		try
		{
			wrappedAutoCloseable.close();
		} catch (Exception e)
		{
			if (e instanceof RuntimeException)
				throw (RuntimeException)e;
			
			throw new RuntimeException(e);
		}
	}

}
