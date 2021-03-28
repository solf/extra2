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
package com.github.solf.extra2.exception;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Thrown to indicate that an assertion has failed.
 * <p>
 * Unlike {@link AssertionError} this is an instance of {@link RuntimeException}
 * -- and thus instance of {@link Exception} -- whereas {@link AssertionError} is
 * an {@link Error} and thus is not caught by {@link Exception} catch block.
 * <p>
 * So this exception is much safer to use vs. various error handlers.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class AssertionException extends RuntimeException
{

	/**
	 * 
	 */
	public AssertionException()
	{
		super();
	}

	/**
	 * @param message
	 * @param cause
	 */
	public AssertionException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public AssertionException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public AssertionException(Throwable cause)
	{
		super(cause);
	}

}
