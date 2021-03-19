/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.cache.wbrb;

import java.util.MissingResourceException;

import javax.annotation.ParametersAreNonnullByDefault;

import org.javatuples.Pair;

import com.github.solf.extra2.cache.exception.CacheFullException;
import com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry;
import com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBInitialReadFailedFinalDecision;
import com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBMergeDecision;
import com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBResyncFailedFinalDecision;
import com.github.solf.extra2.config.FlatConfiguration;
import com.github.solf.extra2.options.BaseDelegatingOptions;
import com.github.solf.extra2.options.BaseOptions;

import lombok.Getter;

/**
 * Configuration for {@link WriteBehindResyncInBackgroundCache}
 * TODO: probably at least some stuff needs to be re-configurable at runtime?
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class WBRBConfig extends BaseDelegatingOptions
{
	/** Used in thread names and other places to distinguish from other stuff, e.g. something like WBRBCache[cacheName]-read-thread-pool-1 */
	@Getter
	private final String cacheName = getRawOptions().getString("cacheName");
	
	/** Cache maintains an internal 'main' data queue for the data that was cached and may need to be written to the storage; this indicates maximum target size for this queue (i.e. should not exceed the size under normal circumstances); note that this is not the whole size of the cache, there's also 'return' queue TODO more details? */
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
	
	/** Whether cache can merge writes -- specifically if some background write has failed AND there are new updates to the cache value, can cache produce a single write data that merges these values in {@link WriteBehindResyncInBackgroundCache#splitForWrite(Object, Object, com.github.solf.extra2.nullable.NullableOptional)}; if not, then previously failed write can only be re-attempted by itself (thus delaying when further in-memory updates will be written out) */
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
	// FIXME use this!
	@Getter
	private final boolean allowDataReadingAfterResyncFailedFinal = getRawOptions().getBoolean("allowDataReadingAfterResyncFailedFinal");

	/** Default: true; whether it is allowed to keep collecting updates when read/resync has been failing for more than 1 full cycle (allowing it has obvious memory costs) */
	@Getter
	private final boolean allowUpdatesCollectionForMultipleFullCycles = getRawOptions().getBoolean("allowUpdatesCollectionForMultipleFullCycles", true);
	
	
	/** Default: Thread.NORM_PRIORITY + 1; priority to be used for read queue processing thread */ 
	@Getter
	private final int readQueueProcessingThreadPriority = getRawOptions().getIntPositive("readQueueProcessingThreadPriority", Thread.NORM_PRIORITY + 1);
	
	/** Default: 100ms; zero value disables batching functionality; how long read queue processor will wait for the next read item before declaring the batch finished; only useful if batching is used; TODO add info */
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
	
	/** Default: 100ms; zero value disables batching functionality; how long write queue processor will wait for the next write item before declaring the batch finished; only useful if batching is used; TODO add info */
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
	
	/** Default: false; whether cache may accept out-of-order (unexpected) reads -- useful for implementations that may perform additional reads at the times not expected by the default cache implementation; see also {@link WriteBehindResyncInBackgroundCache#spiWriteLockIsAcceptOutOfOrderRead(Object, Object, WBRBCacheEntry, com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)} */ 
	@Getter
	private final boolean acceptOutOfOrderReads = getRawOptions().getBoolean("acceptOutOfOrderReads", false);
	
	
	/** Default: 10s; 'time window' over which log throttling calculates maximum number of allowed messages (per message type) */
	@Getter
	private final long logThrottleTimeInterval = getRawOptions().getTimeIntervalPositive("logThrottleTimeInterval", "10s");

	/** Default: 10; zero value disabled throttling; how many messages of a single type can be logged per log throttling 'time window' */
	@Getter
	private final int logThrottleMaxMessagesOfTypePerTimeInterval = getRawOptions().getIntNonNegative("logThrottleMaxMessagesOfTypePerTimeInterval", 10);
	
	
	/** Default: false (for performance); if enabled, various events will be passed to {@link WriteBehindResyncInBackgroundCache#spiUnknownLock_Event(com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBEvent, Object, com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload, Throwable, Object...)} */
	@Getter
	private final boolean eventNotificationEnabled = getRawOptions().getBoolean("eventNotificationEnabled", false);

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
}
