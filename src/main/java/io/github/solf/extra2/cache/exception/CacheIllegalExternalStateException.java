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
package io.github.solf.extra2.cache.exception;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Indicates that cache operation failed due to the invalid EXTERNAL cache state,
 * such as problems accessing the underlying storage.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public abstract class CacheIllegalExternalStateException extends CacheIllegalStateException
{

	/**
	 * 
	 */
	public CacheIllegalExternalStateException()
	{
		super();
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	protected CacheIllegalExternalStateException(String message, Throwable cause,
		boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public CacheIllegalExternalStateException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public CacheIllegalExternalStateException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public CacheIllegalExternalStateException(Throwable cause)
	{
		super(cause);
	}

}
