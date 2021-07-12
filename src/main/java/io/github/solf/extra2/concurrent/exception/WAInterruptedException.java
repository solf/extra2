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
package io.github.solf.extra2.concurrent.exception;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This is wrapper class to rethrow {@link InterruptedException} as unchecked.
 * <p>
 * There should always be a 'cause' and it should be of {@link InterruptedException}
 * type.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class WAInterruptedException extends RuntimeException
{
	/**
	 * Constructor.
	 */
	public WAInterruptedException(InterruptedException cause)
	{
		super("Rethrown unchecked: " + cause, cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public WAInterruptedException(String message, InterruptedException cause)
	{
		super(message, cause);
	}
}
