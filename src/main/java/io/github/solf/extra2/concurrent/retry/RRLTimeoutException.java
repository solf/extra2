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
package io.github.solf.extra2.concurrent.retry;

import org.eclipse.jdt.annotation.NonNullByDefault;

import lombok.Getter;

/**
 * Used by {@link RetryAndRateLimitService} to indicate in {@link RRLFuture}
 * that processing was aborted due to timeout.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class RRLTimeoutException extends RuntimeException
{
	/**
	 * Total time the request was being processed for until timeout.
	 */
	@Getter
	private final long totalProcessingTime;
	
	/**
	 * Constructor.
	 */
	public RRLTimeoutException(long totalProcessingTime)
	{
		super("Request timed out after: " + totalProcessingTime + "ms");
		
		this.totalProcessingTime = totalProcessingTime;
	}

	/**
	 * Constructor for wrapping timeout exception in another timeout exception
	 * for use in futures.
	 */
	public RRLTimeoutException(RRLTimeoutException cause)
	{
		super(cause);
		
		this.totalProcessingTime = cause.getTotalProcessingTime();
	}
}
