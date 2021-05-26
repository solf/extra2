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
package io.github.solf.extra2.cache.wbrb;

import java.util.List;
import java.util.MissingResourceException;

import javax.annotation.ParametersAreNonnullByDefault;

import org.javatuples.Pair;

import io.github.solf.extra2.cache.exception.CacheFullException;
import io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry;
import io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBInitialReadFailedFinalDecision;
import io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBMergeDecision;
import io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBResyncFailedFinalDecision;
import io.github.solf.extra2.config.FlatConfiguration;
import io.github.solf.extra2.options.BaseDelegatingOptions;
import io.github.solf.extra2.options.BaseOptions;
import io.github.solf.extra2.options.OptionConstraint;
import lombok.Getter;

/**
 * Configuration for {@link WriteBehindResyncInBackgroundCache}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class WBRBConfig extends BaseDelegatingOptions
{
	/** Used in thread names and other places to distinguish from other stuff, e.g. something like WBRBCache[cacheName]-read-thread-pool-1 */
	@Getter
	private final String cacheName = getRawOptions().getString("cacheName");
	
	/** Cache maintains an internal 'main' data queue for the data that was cached and may need to be written to the storage; this indicates maximum target size for this queue (i.e. should not exceed the size under normal circumstances); note that this is not the whole size of the cache, there's also 'return' queue TO-DO more details? */
	@Getter
	private final long mainQueueMaxTargetSize = getRawOptions().getLong("mainQueueMaxTargetSize");
	
	/** Default value: 2x {@link #dataQueueMaxTargetSize}; if cache size (including data queue and other internal queue(s) such as 'return' queue) exceeds this value then no further elements can be added and attempts to do so fail with {@link CacheFullException}  */
	@Getter
	private final long maxCacheElementsHardLimit = getRawOptions().getLong("maxCacheElementsHardLimit", mainQueueMaxTargetSize * 2);
	
	/** How many updates (per cache item) we are allowed to collect */
	@Getter
	private final int maxUpdatesToCollect = getRawOptions().getIntNonNegative("maxUpdatesToCollect");

	/** Target time for cache elements to spend in the main processing queue; they can be processed faster if cache is exceeding size limits */
	@Getter
	private final long mainQueueCacheTime = getRawOptions().getTimeIntervalPositive("mainQueueCacheTime");
	
	/** Default: 1/10 of mainQueueCacheTime; minimum time for cache elements to stay in the main processing queue; if there's not enough cache size to accommodate this value, new elements will be rejected; used to avoid busy loops and similar problems */  
	@Getter
	private final long mainQueueCacheTimeMin = getRawOptions().getTimeIntervalPositive("mainQueueCacheTimeMin", mainQueueCacheTime / 10);

	/** Minimum (also target) time that cache elements spend in the return queue -- this is intended to give time for the write operation to complete before cache eviction is considered */  
	@Getter
	private final long returnQueueCacheTimeMin = getRawOptions().getTimeIntervalPositive("returnQueueCacheTimeMin");
	
	/** Zero is the valid value; time that cache item must be 'untouched' before it can be expired in return queue processing; zero means that only items touched while in return queue will be retained; this can still be ignored (and item expired from cache) if current main queue size exceeds target size */
	@Getter
	private final long untouchedItemCacheExpirationDelay = getRawOptions().getTimeInterval("untouchedItemCacheExpirationDelay");
	
	/** Whether cache can merge writes -- specifically if some background write has failed AND there are new updates to the cache value, can cache produce a single write data that merges these values in {@link WriteBehindResyncInBackgroundCache#splitForWrite(Object, Object, io.github.solf.extra2.nullable.NullableOptional)}; if not, then previously failed write can only be re-attempted by itself (thus delaying when further in-memory updates will be written out) */
	@Getter
	private final boolean canMergeWrites = getRawOptions().getBoolean("canMergeWrites");
	
	/** When inital read fails (final fail after possible retries) what action should be taken; options are in WBRBInitialReadFailedFinalDecision: REMOVE_FROM_CACHE, KEEP_AND_THROW_CACHE_READ_EXCEPTIONS */ 
	@Getter
	private final WBRBInitialReadFailedFinalDecision initialReadFailedFinalAction = WBRBInitialReadFailedFinalDecision.valueOf(getRawOptions().getString("initialReadFailedFinalAction"));
	
	/** When background resync is too late (not all updates are collected) what action should be taken; options are in WBRBMergeDecision: SET_DIRECTLY (use data from storage, ignore in-memory data), MERGE_DATA (even though updates were not properly tracked, CLEAR_READ_PENDING_STATUS (keep current in-memory data), REMOVE_FROM_CACHE, DO_NOTHING (!!! this is dangerous, should not be used in most cases, see CLEAR_READ_PENDING_STATUS instead) */ 
	@Getter
	private final WBRBMergeDecision resyncTooLateAction = WBRBMergeDecision.valueOf(getRawOptions().getString("resyncTooLateAction"));
	
	/** When background resync fails (final fail after possible retries) what action should be taken; options are in WBRBResyncFailedFinalDecision: REMOVE_FROM_CACHE, STOP_COLLECTING_UPDATES, KEEP_COLLECTING_UPDATES */ 
	@Getter
	private final WBRBResyncFailedFinalDecision resyncFailedFinalAction = WBRBResyncFailedFinalDecision.valueOf(getRawOptions().getString("resyncFailedFinalAction"));
	
	/** Whether cache is allowed to write out data that needed a resync when that resync failed and there is no option to attempt resync again; either value may result in data loss */
	@Getter
	private final boolean allowDataWritingAfterResyncFailedFinal = getRawOptions().getBoolean("allowDataWritingAfterResyncFailedFinal");
	
	/** Whether cache is allowed to read (provide to clients) data that needed a resync when that resync failed and there is no option to attempt resync again; either value may result in data loss */
	@Getter
	private final boolean allowDataReadingAfterResyncFailedFinal = getRawOptions().getBoolean("allowDataReadingAfterResyncFailedFinal");

	/** Default: true; whether it is allowed to keep collecting updates when read/resync has been failing for more than 1 full cycle (allowing it has obvious memory costs) */
	@Getter
	private final boolean allowUpdatesCollectionForMultipleFullCycles = getRawOptions().getBoolean("allowUpdatesCollectionForMultipleFullCycles", true);
	
	
	/** Default: Thread.NORM_PRIORITY + 1; priority to be used for read queue processing thread */ 
	@Getter
	private final int readQueueProcessingThreadPriority = getRawOptions().getIntPositive("readQueueProcessingThreadPriority", Thread.NORM_PRIORITY + 1);
	
	/** Default: 100ms; zero value disables batching functionality; how long read queue processor will wait for the next read item before declaring the batch finished; only useful if batching is used; TO-DO add info */
	@Getter
	private final long readQueueBatchingDelay = getRawOptions().getTimeInterval("readQueueBatchingDelay", "100ms"); 
	
	/** Min value and max value for read thread pool size (reads are normally executed in separate threads); -1, -1 indicates that there's no read thread pool and instead reads are performed in the read queue processing thread -- in this case you should typically provide your own async processing to e.g. support batching */
	@Getter
	private final Pair<Integer, Integer> readThreadPoolSize = getRawOptions().getIntPairNegOneOrMore("readThreadPoolSize");
	
	/** Default: Thread.NORM_PRIORITY; priority for threads in read pool */
	@Getter
	private final int readThreadPoolPriority = getRawOptions().getIntPositive("readThreadPoolPriority", Thread.NORM_PRIORITY);
	
	/** Default: 60s; max idle time for threads in read pool (if idle exceeds minimum thread count) */
	@Getter
	private final long readThreadPoolMaxIdleTime = getRawOptions().getTimeInterval("readThreadPoolMaxIdleTime", "60s");
	
	/** Specifies how many times read failures are retried; this only affects standard implementation of retry checking */
	@Getter
	private final int readFailureMaxRetryCount = getRawOptions().getIntNonNegative("readFailureMaxRetryCount");


	/** Default: Thread.NORM_PRIORITY + 1; priority to be used for write queue processing thread */ 
	@Getter
	private final int writeQueueProcessingThreadPriority = getRawOptions().getIntPositive("writeQueueProcessingThreadPriority", Thread.NORM_PRIORITY + 1);
	
	/** Default: 100ms; zero value disables batching functionality; how long write queue processor will wait for the next write item before declaring the batch finished; only useful if batching is used; TO-DO add info */
	@Getter
	private final long writeQueueBatchingDelay = getRawOptions().getTimeInterval("writeQueueBatchingDelay", "100ms"); 
	
	/** Min value and max value for write thread pool size (writes are normally executed in separate threads); -1, -1 indicates that there's no write thread pool and instead writes are performed in the write queue processing thread -- in this case you should typically provide your own async processing to e.g. support batching */
	@Getter
	private final Pair<Integer, Integer> writeThreadPoolSize = getRawOptions().getIntPairNegOneOrMore("writeThreadPoolSize");
	
	/** Default: Thread.NORM_PRIORITY; priority for threads in write pool */
	@Getter
	private final int writeThreadPoolPriority = getRawOptions().getIntPositive("writeThreadPoolPriority", Thread.NORM_PRIORITY);
	
	/** Default: 60s; max idle time for threads in write pool (if idle exceeds minimum thread count) */
	@Getter
	private final long writeThreadPoolMaxIdleTime = getRawOptions().getTimeInterval("writeThreadPoolMaxIdleTime", "60s");
	
	/** Specifies how many times write failures are retried; this only affects standard implementation of retry checking */
	@Getter
	private final int writeFailureMaxRetryCount = getRawOptions().getIntNonNegative("writeFailureMaxRetryCount");

	
	/** Default: Thread.NORM_PRIORITY + 1; priority to be used for main queue processing thread */ 
	@Getter
	private final int mainQueueProcessingThreadPriority = getRawOptions().getIntPositive("mainQueueProcessingThreadPriority", Thread.NORM_PRIORITY + 1);
	
	/** Specifies how many times full cache cycle failures are retried (e.g. when item resync or write fails); this only affects standard implementation of retry checking */
	@Getter
	private final int fullCacheCycleFailureMaxRetryCount = getRawOptions().getIntNonNegative("fullCacheCycleFailureMaxRetryCount");

	
	/** Default: Thread.NORM_PRIORITY + 1; priority to be used for return queue processing thread */ 
	@Getter
	private final int returnQueueProcessingThreadPriority = getRawOptions().getIntPositive("returnQueueProcessingThreadPriority", Thread.NORM_PRIORITY + 1);
	
	/** How many times item can be re-queued in return queue before giving up (affects default implementation of 'write pending' state) */
	@Getter
	private final int returnQueueMaxRequeueCount = getRawOptions().getIntNonNegative("returnQueueMaxRequeueCount");
	
	
	/** Default: 3; how many times cache may attempt/retry to get element by key before giving up due to 'removed from cache' element state; this shouldn't normally happen more than once */
	@Getter
	private final int maxCacheRemovedRetries = getRawOptions().getIntNonNegative("maxCacheRemovedRetries", 3);
	
	/** Default: 500ms; no thread is allowed to sleep/wait for blocking operation longer than this time at a time; this helps with e.g. handling shutdown and/or changing time factor and possibly other issues; you might want to increase this if your read/write methods routinely wait for longer */
	@Getter
	private final long maxSleepTime = getRawOptions().getTimeIntervalPositive("maxSleepTime", "500ms");
	
	/** Default: false; whether cache may accept out-of-order (unexpected) reads -- useful for implementations that may perform additional reads at the times not expected by the default cache implementation; see also {@link WriteBehindResyncInBackgroundCache#spiWriteLockIsAcceptOutOfOrderRead(Object, Object, WBRBCacheEntry, io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)} */ 
	@Getter
	private final boolean acceptOutOfOrderReads = getRawOptions().getBoolean("acceptOutOfOrderReads", false);
	
	
	/** Default: 10s; 'time window' over which log throttling calculates maximum number of allowed messages (per message type) */
	@Getter
	private final long logThrottleTimeInterval = getRawOptions().getTimeIntervalPositive("logThrottleTimeInterval", "10s");

	/** Default: 10; zero value disabled throttling; how many messages of a single type can be logged per log throttling 'time window' */
	@Getter
	private final int logThrottleMaxMessagesOfTypePerTimeInterval = getRawOptions().getIntNonNegative("logThrottleMaxMessagesOfTypePerTimeInterval", 10);
	
	
	/** Default: false (for performance); if enabled, various events will be passed to {@link WriteBehindResyncInBackgroundCache#spiUnknownLock_Event(io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBEvent, Object, io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload, Throwable, Object...)} */
	@Getter
	private final boolean eventNotificationEnabled = getRawOptions().getBoolean("eventNotificationEnabled", false);
	
	/** Default: 1,2,3,4,9 ; a list of exactly 5 int thresholds to be used as thresholds (equal or less) for monitoring 'for how many full cycles items is in the cache' at the end of return queue; values MUST be in ascending order */
	@Getter
	private final List<Integer> monitoringFullCacheCyclesThresholds;
	{
		List<Integer> list = getRawOptions().getIntList("monitoringFullCacheCyclesThresholds", "1,2,3,4,9", OptionConstraint.POSITIVE, OptionConstraint.NON_EMPTY_COLLECTION);
		validateThresholdsListIsAscending(list, "monitoringFullCacheCyclesThresholds");
		
		monitoringFullCacheCyclesThresholds = list;
	}
	
	/** Default: 5s,10s,15s,20s,25s ; a list of exactly 5 time intervals to be used as thresholds (equal or less) for monitoring 'time since last access' at the end of return queue; values MUST be in ascending order */
	@Getter
	private final List<Long> monitoringTimeSinceAccessThresholds;
	{
		List<Long> list = getRawOptions().getTimeIntervalList("monitoringTimeSinceAccessThresholds", "5s,10s,15s,20s,25s", OptionConstraint.NON_NEGATIVE, OptionConstraint.NON_EMPTY_COLLECTION);
		validateThresholdsListIsAscending(list, "monitoringTimeSinceAccessThresholds");
		
		monitoringTimeSinceAccessThresholds = list;
	}

	/**
	 * @param initializeFrom
	 * @throws MissingResourceException
	 * @throws NumberFormatException
	 */
	public WBRBConfig(BaseOptions initializeFrom)
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
	public WBRBConfig(FlatConfiguration configuration)
		throws MissingResourceException,
		NumberFormatException
	{
		super(configuration);
	}

	/**
	 * Validates that the given thresholds list is in ascending order and contains
	 * the 'correct' number of elements (5 in this case).
	 * <p>
	 * Also checks that values are non-negative.
	 */
	protected void validateThresholdsListIsAscending(List<? extends Number> list, String optionName)
	{
		if (list.size() != 5)
			throw new IllegalStateException(optionName + " list must be exactly 5 elements long, got: " + list);
		for (int i = 0; i < list.size() - 1; i++)
		{
			if (list.get(i).longValue() < 0)
				throw new IllegalStateException(optionName + " list must contain non-negative values, got: " + list);
			if (list.get(i).longValue() >= list.get(i + 1).longValue())
				throw new IllegalStateException(optionName + " list must be in ascending order, got: " + list);
		}
	}
}
