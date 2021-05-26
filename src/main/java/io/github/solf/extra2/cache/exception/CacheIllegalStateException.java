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

import javax.annotation.Nullable;
import javax.annotation.NonNullByDefault;

/**
 * Indicates that cache operation failed due to the invalid cache state.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class CacheIllegalStateException extends BaseCacheException
{

	/**
	 * 
	 */
	public CacheIllegalStateException()
	{
		super();
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	protected CacheIllegalStateException(String message, @Nullable Throwable cause,
		boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public CacheIllegalStateException(String message, @Nullable Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public CacheIllegalStateException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public CacheIllegalStateException(@Nullable Throwable cause)
	{
		super(cause);
	}

}
