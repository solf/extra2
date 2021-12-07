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

import org.eclipse.jdt.annotation.NonNullByDefault;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * {@link RetryAndRateLimitService} status (for e.g. monitoring).
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
@ToString
@AllArgsConstructor
public class RRLStatus
{
	/**
	 * Indicates when status was created (e.g. for caching purposes).
	 */
	@Getter
	private final long statusCreatedAt;
	
	/**
	 * Whether service is alive (that is it was started and not stopped yet). 
	 */
	@Getter
	private final boolean serviceAlive;
	
	/**
	 * Whether service is usable -- that is standard submit operations
	 * can be performed; this can differ from {@link #isServiceAlive()} value 
	 * for various reasons such as service shutdown in progress. 
	 */
	@Getter
	private final boolean serviceUsable;

	// zzz add this
//	/**
//	 * Current cache control state as enum -- NOT_STARTED, RUNNING, SHUTDOWN...
//	 */
//	@Getter
//	private final WBRBCacheControlState cacheControlState;
//	
//	/**
//	 * Current cache control state as String (from WBRBCacheControlState) -- NOT_STARTED, RUNNING, SHUTDOWN...
//	 */
//	@Getter
//	private final String cacheControlStateString;
	
	/**
	 * Whether thread is alive.
	 */
	@Getter
	private final boolean mainQueueProcessingThreadAlive;
	
	/**
	 * Whether all delay queue processing threads are alive.
	 */
	@Getter
	private final boolean delayQueueProcessingThreadsAreAlive;
	
	/**
	 * Whether requests executor service is alive.
	 */
	@Getter
	private final boolean requestsExecutorServiceAlive;
	
	/**
	 * Number of currently active threads in the pool.
	 */
	@Getter
	private final int requestsExecutorServiceActiveThreads;
	
	/**
	 * Whether service itself AND all the threads & thread pools required for the 
	 * service operation are still alive.
	 */
	@Getter
	private final boolean everythingAlive;
	
	
	
	/**
	 * Count of the requests currently being processed.
	 */
	@Getter
	private final int currentProcessingRequestsCount;
	
	/**
	 * Main processing queue size.
	 */
	@Getter
	private final int mainQueueSize;

	
	
	
	/**
	 * (Configuration) max attempts per request. 
	 */
	@Getter
	private final int configMaxAttempts;
	
	/**
	 * (Configuration) A list of delays applied after each subsequent failure in order. 
	 */
	@Getter
	private final List<Long> configDelaysAfterFailure;
	
	/**
	 * (Configuration) The maximum number of pending/in-flight requests that the service is allowed to handle. 
	 */
	@Getter
	private final int configMaxPendingRequests;
	
	
	/**
	 * (Configuration) Grace period that allows requests to be processed this much earlier than intended. 
	 */
	@Getter
	private final long configRequestEarlyProcessingGracePeriod;
	
	//zzz add rate limiter stuff
	

	//zzz consider adding counters for events or just rely on listener for that?
	
	//zzz consider adding counts for e.g. assertion errors?
}
