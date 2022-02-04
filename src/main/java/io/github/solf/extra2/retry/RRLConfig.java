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

import java.util.List;
import java.util.MissingResourceException;
import java.util.concurrent.RejectedExecutionException;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.config.FlatConfiguration;
import io.github.solf.extra2.options.BaseDelegatingOptions;
import io.github.solf.extra2.options.BaseOptions;
import io.github.solf.extra2.options.OptionConstraint;
import lombok.Getter;

/**
 * Configuration for {@link RetryAndRateLimitService}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class RRLConfig extends BaseDelegatingOptions
{
	/** Used in thread names and other places to distinguish from other stuff, e.g. something like WBRBCache[cacheName]-read-thread-pool-1 */
	@Getter
	private final String serviceName = getRawOptions().getString("serviceName");
	
	/** Whether service should use daemon threads; using daemon threads increases risk of data loss (since service won't prevent JVM from exiting) but similarly it may unexpectedly prevent JVM from exiting */
	@Getter
	private final boolean useDaemonThreads = getRawOptions().getBoolean("useDaemonThreads");
	
	
	/** Maximum allowed number of attempts for one requests -- if request doesn't complete successfully in this many attempts, it is abandoned (considired 'failed final') */ 
	@Getter
	private final int maxAttempts = getRawOptions().getIntPositive("maxAttempts");
	
	/** A list of delays applied after each subsequent failure in order; must contain at least one element; if list is shorter than the already-failed attempts count, then the last value in the list is used */
	@Getter
	private final List<Long> delaysAfterFailure = getRawOptions().getTimeIntervalList("delaysAfterFailure", OptionConstraint.NON_EMPTY_COLLECTION, OptionConstraint.NON_NEGATIVE);
	
	/** The maximum number of pending/in-flight requests that the service is allowed to handle; trying to add more will result in {@link RejectedExecutionException} */
	@Getter
	private final int maxPendingRequests = getRawOptions().getIntPositive("maxPendingRequests");
	
	
	/** Grace period that allows requests to be processed this much earlier than intended; since delay mechanisms used by the service are not exact, it's useful for performance reasons to set it to e.g. the same value as shortest delay queue */
	@Getter
	private final long requestEarlyProcessingGracePeriod = getRawOptions().getTimeInterval("requestEarlyProcessingGracePeriod");
	

	/** 
	 * To implement delays internally the service uses a collection of delay queues 
	 * each of which delays an item for the set amount of time.
	 * <p>
	 * It is possible to use just one queue with short delay time, however it
	 * will introduce CPU costs as the queue will constantly rotate its elements;
	 * therefore it is advisable to use a set of queues that cover the expected
	 * delays with not-too-many total trips through all the queues.
	 * <p>
	 * E.g. if you would like to have delay precision of 100ms and maximum delay
	 * is on the order of 5 minutes, you might want to do something like:
	 * 100ms, 500ms, 2s, 10s, 1m
	 */
	@Getter
	private final List<Long> delayQueues = getRawOptions().getTimeIntervalList("delayQueues", OptionConstraint.NON_EMPTY_COLLECTION, OptionConstraint.POSITIVE);
	
	/** Default: 1s; Grace period that allows a 'too long' delay queue to still be used (this accounts for processing delays when delay queue duration is equal to intended delay after an attempt) */
	@Getter
	private final long delayQueueTooLongGracePeriod = getRawOptions().getTimeInterval("delayQueueTooLongGracePeriod", "1s");

	
	/** Rate limiter's bucket size [starts empty] (maximum number of available tokens that can be stored); setting this to zero disables rate limiter -- requests are sent for execution asap */
	@Getter
	private final int rateLimiterBucketSize = getRawOptions().getIntNonNegative("rateLimiterBucketSize");
	
	/** Rate limiter's refill rate per time interval (which defaults to 1 second) */
	@Getter
	private final int rateLimiterRefillRate = getRawOptions().getIntPositive("rateLimiterRefillRate");
	
	/** Default: 1s; rate limiter's refill time interval (refill rate is spread over this time interval) */
	@Getter
	private final long rateLimiterRefillInterval = getRawOptions().getTimeInterval("rateLimiterRefillInterval", "1s");

	
	/** Thread pool size for requests processing, must contain two elements: min size, max size */
	@Getter
	private final List<Integer> requestProcessingThreadPoolConfig = getRawOptions().getIntList("requestProcessingThreadPoolConfig", OptionConstraint.NON_EMPTY_COLLECTION, OptionConstraint.NON_NEGATIVE);
	
	/** Default: 5 (Thread.NORM_PRIORITY); priority to be used for requests processing thread pool */ 
	@Getter
	private final int requestProcessingThreadPriority = getRawOptions().getIntPositive("requestProcessingThreadPriority", Thread.NORM_PRIORITY);
	
	
	/** Default: 500ms; THIS IS IN 'REAL MS' (not affected by time factors) -- no thread is allowed to sleep/wait for blocking operation longer than this time at a time; this helps with e.g. handling shutdown and/or changing time factor and possibly other issues; you might want to increase this if your read/write methods routinely wait for longer */
	@Getter
	private final long maxSleepTime = getRawOptions().getTimeIntervalPositive("maxSleepTime", "500ms");
	

	/** Default: 5; percent time allocated during shutdown to cover 'processing delays' -- i.e. target shutdown time will be actually less than requested time by this amount (in percents) */
	@Getter
	private final int shutdownBufferTimePerc = getRawOptions().getIntNonNegative("shutdownBufferTimePerc", 5);
	{
		if (shutdownBufferTimePerc > 100)
			throw new IllegalStateException("shutdownBufferTimePerc value must be 0-100 (in percent), got: " + shutdownBufferTimePerc);
	}
	
	
	/** Default: 6 (Thread.NORM_PRIORITY + 1); priority to be used for main queue processing thread */ 
	@Getter
	private final int mainQueueProcessingThreadPriority = getRawOptions().getIntPositive("mainQueueProcessingThreadPriority", Thread.NORM_PRIORITY + 1);
	
	/** Default: 10; limit on unexpected {@link InterruptedException} in main queue processing before giving up; giving up renders service inoperable */
	@Getter
	private final int mainQueueUnexpectedInterruptedExceptionLimit = getRawOptions().getIntNonNegative("mainQueueUnexpectedInterruptedExceptionLimit", 10);
	
	/** Default: 10; limit on unhandled {@link RuntimeException} in  main queue processing before giving up; giving up renders service inoperable */
	@Getter
	private final int mainQueueRuntimeExceptionLimit = getRawOptions().getIntNonNegative("mainQueueRuntimeExceptionLimit", 10);
	
	/** Default: 2000ms; how long at a maximum main queue processing will wait for worker thread to pick up request for processing; in practice this should be near-instant; but if this value is exceeded, then error is logged and item processing is aborted (it is re-queued) */ 
	@Getter
	private final long mainQueueMaxRequestHandoverWaitTime = getRawOptions().getTimeIntervalPositive("mainQueueMaxRequestHandoverWaitTime", 2000);
	
	/** Default: 6 (Thread.NORM_PRIORITY + 1); priority to be used for threads processing delay queues */ 
	@Getter
	private final int delayQueueProcessingThreadPriority = getRawOptions().getIntPositive("delayQueueProcessingThreadPriority", Thread.NORM_PRIORITY + 1);
	
	/** Default: 10; limit on unexpected {@link InterruptedException} in delay queue processing before giving up; giving up renders service inoperable */
	@Getter
	private final int delayQueueUnexpectedInterruptedExceptionLimit = getRawOptions().getIntNonNegative("delayQueueUnexpectedInterruptedExceptionLimit", 10);
	
	/** Default: 10; limit on unhandled {@link RuntimeException} in delay queue processing before giving up; giving up renders service inoperable */
	@Getter
	private final int delayQueueRuntimeExceptionLimit = getRawOptions().getIntNonNegative("delayQueueRuntimeExceptionLimit", 10);
	
	/**
	 * @param initializeFrom
	 * @throws MissingResourceException
	 * @throws NumberFormatException
	 */
	public RRLConfig(BaseOptions initializeFrom)
		throws MissingResourceException,
		NumberFormatException
	{
		super(initializeFrom);
	}

	/**
	 * @param configuration
	 * @throws MissingResourceException
	 * @throws NumberFormatException
	 */
	public RRLConfig(FlatConfiguration configuration)
		throws MissingResourceException,
		NumberFormatException
	{
		super(configuration);
	}
}
