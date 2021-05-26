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
package io.github.solf.extra2.storage.exception;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Base class for storage exceptions.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public abstract class BaseStorageException extends RuntimeException
{

	/**
	 * Constructor.
	 */
	public BaseStorageException()
	{
		super();
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public BaseStorageException(String message, Throwable cause,
		boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public BaseStorageException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public BaseStorageException(String message)
	{
		super(message);
}

	/**
	 * @param cause
	 */
	public BaseStorageException(Throwable cause)
	{
		super(cause);
	}

}