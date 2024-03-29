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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Indicates that element has not been loaded (possibly 'yet' or loading has
 * completely failed).
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public abstract class CacheElementNotLoadedException extends CacheIllegalStateException
{

	/**
	 * 
	 */
	public CacheElementNotLoadedException()
	{
		super();
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	protected CacheElementNotLoadedException(String message, Throwable cause,
		boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public CacheElementNotLoadedException(String message, @Nullable Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public CacheElementNotLoadedException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public CacheElementNotLoadedException(Throwable cause)
	{
		super(cause);
	}
	
}
