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

import java.time.Duration;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucket;

/**
 * Simple {@link RRLRateLimiter} implementation based on Bucket4j and the
 * given capacity & flat rate.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class RRLBucket4jFlatRateLimiter implements RRLRateLimiter<Object>
{
	/**
	 * Ticket instance.
	 */
	private final Object TICKET_INSTANCE = new Object();
	
	/**
	 * Bucket4j for this instance.
	 */
	private final LocalBucket bucket;
	
	/**
	 * Constructor.
	 */
	public RRLBucket4jFlatRateLimiter(int bucketCapacity, int refillRate, long refillIntervalMs, int initialTokens)
	{
		bucket = Bucket4j.builder()
			.addLimit(Bandwidth.classic(
				bucketCapacity, 
				Refill.greedy(refillRate, Duration.ofMillis(refillIntervalMs)))
				.withInitialTokens(initialTokens))
			.build();
	}

	@Override
	public @Nullable Object obtainTicket(long maxWaitRealMs)
		throws InterruptedException
	{
		boolean success = bucket.asScheduler().tryConsume(1, Duration.ofMillis(maxWaitRealMs));
		
		if (success)
			return TICKET_INSTANCE;
		else
			return null;
	}

	@Override
	public void returnUnusedTicket(Object ticket) throws IllegalArgumentException
	{
		if (ticket != TICKET_INSTANCE)
			throw new IllegalArgumentException("Attempt to return non-ours ticket!");
		
		bucket.addTokens(1);
	}

}
