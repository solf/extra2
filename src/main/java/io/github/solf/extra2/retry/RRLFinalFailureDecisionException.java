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
package io.github.solf.extra2.retry;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Used by {@link RetryAndRateLimitService} to indicate in {@link RRLFuture}
 * that processing was aborted due to 'final failure' decision (without an
 * actual exception being available).
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class RRLFinalFailureDecisionException extends Throwable
{

	/**
	 * 
	 */
	public RRLFinalFailureDecisionException()
	{
		super();
	}

	/**
	 * @param message
	 * @param cause
	 */
	public RRLFinalFailureDecisionException(String message,
		@Nullable Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public RRLFinalFailureDecisionException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public RRLFinalFailureDecisionException(@Nullable Throwable cause)
	{
		super(cause);
	}
	
}
