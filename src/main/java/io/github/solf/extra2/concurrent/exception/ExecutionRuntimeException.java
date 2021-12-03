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

import java.util.concurrent.ExecutionException;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A runtime version of {@link ExecutionException}
 * <p>
 * Exception thrown when attempting to retrieve the result of a task
 * that aborted by throwing an exception. This exception can be
 * inspected using the {@link #getCause()} method.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class ExecutionRuntimeException extends RuntimeException
{
	/**
	 * @param cause
	 */
	public ExecutionRuntimeException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public ExecutionRuntimeException(String message,
		Throwable cause)
	{
		super(message, cause);
	}
}
