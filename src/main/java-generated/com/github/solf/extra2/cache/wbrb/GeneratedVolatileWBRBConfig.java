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
package com.github.solf.extra2.cache.wbrb;

import java.util.List;
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
import com.github.solf.extra2.options.OptionConstraint;

import lombok.Getter;

/**
 * Configuration for {@link WriteBehindResyncInBackgroundCache} that allows changing the values by storing everything in volatile fields and providing setters
 *
 * @author Sergey Olefir
 */
@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
public class GeneratedVolatileWBRBConfig extends WBRBConfig
{
	/** Used in thread names and other places to distinguish from other stuff, e.g. something like WBRBCache[cacheName]-read-thread-pool-1 */
	private volatile String v_cacheName = super.getCacheName();
	@Override
	public String getCacheName() {return v_cacheName;}
	public void setCacheName(String newValue) {v_cacheName = newValue;}
	
	/** Cache maintains an internal 'main' data queue for the data that was cached and may need to be written to the storage; this indicates maximum target size for this queue (i.e. should not exceed the size under normal circumstances); note that this is not the whole size of the cache, there's also 'return' queue TO-DO more details? */
	private volatile long v_mainQueueMaxTargetSize = super.getMainQueueMaxTargetSize();
	@Override
	public long getMainQueueMaxTargetSize() {return v_mainQueueMaxTargetSize;}
	public void setMainQueueMaxTargetSize(long newValue) {v_mainQueueMaxTargetSize = newValue;}
	
	/** Default value: 2x {@link #dataQueueMaxTargetSize}; if cache size (including data queue and other internal queue(s) such as 'return' queue) exceeds this value then no further elements can be added and attempts to do so fail with {@link CacheFullException}  */
	private volatile long v_maxCacheElementsHardLimit = super.getMaxCacheElementsHardLimit();
	@Override
	public long getMaxCacheElementsHardLimit() {return v_maxCacheElementsHardLimit;}
	public void setMaxCacheElementsHardLimit(long newValue) {v_maxCacheElementsHardLimit = newValue;}
	
	/** How many updates (per cache item) we are allowed to collect */
	private volatile int v_maxUpdatesToCollect = super.getMaxUpdatesToCollect();
	@Override
	public int getMaxUpdatesToCollect() {return v_maxUpdatesToCollect;}
	public void setMaxUpdatesToCollect(int newValue) {v_maxUpdatesToCollect = newValue;}

	/** Target time for cache elements to spend in the main processing queue; they can be processed faster if cache is exceeding size limits */
	private volatile long v_mainQueueCacheTime = super.getMainQueueCacheTime();
	@Override
	public long getMainQueueCacheTime() {return v_mainQueueCacheTime;}
	public void setMainQueueCacheTime(long newValue) {v_mainQueueCacheTime = newValue;}
	
	/** Default: 1/10 of mainQueueCacheTime; minimum time for cache elements to stay in the main processing queue; if there's not enough cache size to accommodate this value, new elements will be rejected; used to avoid busy loops and similar problems */  
	private volatile long v_mainQueueCacheTimeMin = super.getMainQueueCacheTimeMin();
	@Override
	public long getMainQueueCacheTimeMin() {return v_mainQueueCacheTimeMin;}
	public void setMainQueueCacheTimeMin(long newValue) {v_mainQueueCacheTimeMin = newValue;}

	/** Minimum (also target) time that cache elements spend in the return queue -- this is intended to give time for the write operation to complete before cache eviction is considered */  
	private volatile long v_returnQueueCacheTimeMin = super.getReturnQueueCacheTimeMin();
	@Override
	public long getReturnQueueCacheTimeMin() {return v_returnQueueCacheTimeMin;}
	public void setReturnQueueCacheTimeMin(long newValue) {v_returnQueueCacheTimeMin = newValue;}
	
	/** Zero is the valid value; time that cache item must be 'untouched' before it can be expired in return queue processing; zero means that only items touched while in return queue will be retained; this can still be ignored (and item expired from cache) if current main queue size exceeds target size */
	private volatile long v_untouchedItemCacheExpirationDelay = super.getUntouchedItemCacheExpirationDelay();
	@Override
	public long getUntouchedItemCacheExpirationDelay() {return v_untouchedItemCacheExpirationDelay;}
	public void setUntouchedItemCacheExpirationDelay(long newValue) {v_untouchedItemCacheExpirationDelay = newValue;}
	
	/** Whether cache can merge writes -- specifically if some background write has failed AND there are new updates to the cache value, can cache produce a single write data that merges these values in {@link WriteBehindResyncInBackgroundCache#splitForWrite(Object, Object, com.github.solf.extra2.nullable.NullableOptional)}; if not, then previously failed write can only be re-attempted by itself (thus delaying when further in-memory updates will be written out) */
	private volatile boolean v_canMergeWrites = super.isCanMergeWrites();
	@Override
	public boolean isCanMergeWrites() {return v_canMergeWrites;}
	public void setCanMergeWrites(boolean newValue) {v_canMergeWrites = newValue;}
	
	/** When inital read fails (final fail after possible retries) what action should be taken; options are in WBRBInitialReadFailedFinalDecision: REMOVE_FROM_CACHE, KEEP_AND_THROW_CACHE_READ_EXCEPTIONS */ 
	private volatile WBRBInitialReadFailedFinalDecision v_initialReadFailedFinalAction = super.getInitialReadFailedFinalAction();
	@Override
	public WBRBInitialReadFailedFinalDecision getInitialReadFailedFinalAction() {return v_initialReadFailedFinalAction;}
	public void setInitialReadFailedFinalAction(WBRBInitialReadFailedFinalDecision newValue) {v_initialReadFailedFinalAction = newValue;}
	
	/** When background resync is too late (not all updates are collected) what action should be taken; options are in WBRBMergeDecision: SET_DIRECTLY (use data from storage, ignore in-memory data), MERGE_DATA (even though updates were not properly tracked, CLEAR_READ_PENDING_STATUS (keep current in-memory data), REMOVE_FROM_CACHE, DO_NOTHING (!!! this is dangerous, should not be used in most cases, see CLEAR_READ_PENDING_STATUS instead) */ 
	private volatile WBRBMergeDecision v_resyncTooLateAction = super.getResyncTooLateAction();
	@Override
	public WBRBMergeDecision getResyncTooLateAction() {return v_resyncTooLateAction;}
	public void setResyncTooLateAction(WBRBMergeDecision newValue) {v_resyncTooLateAction = newValue;}
	
	/** When background resync fails (final fail after possible retries) what action should be taken; options are in WBRBResyncFailedFinalDecision: REMOVE_FROM_CACHE, STOP_COLLECTING_UPDATES, KEEP_COLLECTING_UPDATES */ 
	private volatile WBRBResyncFailedFinalDecision v_resyncFailedFinalAction = super.getResyncFailedFinalAction();
	@Override
	public WBRBResyncFailedFinalDecision getResyncFailedFinalAction() {return v_resyncFailedFinalAction;}
	public void setResyncFailedFinalAction(WBRBResyncFailedFinalDecision newValue) {v_resyncFailedFinalAction = newValue;}
	
	/** Whether cache is allowed to write out data that needed a resync when that resync failed and there is no option to attempt resync again; either value may result in data loss */
	private volatile boolean v_allowDataWritingAfterResyncFailedFinal = super.isAllowDataWritingAfterResyncFailedFinal();
	@Override
	public boolean isAllowDataWritingAfterResyncFailedFinal() {return v_allowDataWritingAfterResyncFailedFinal;}
	public void setAllowDataWritingAfterResyncFailedFinal(boolean newValue) {v_allowDataWritingAfterResyncFailedFinal = newValue;}
	
	/** Whether cache is allowed to read (provide to clients) data that needed a resync when that resync failed and there is no option to attempt resync again; either value may result in data loss */
	private volatile boolean v_allowDataReadingAfterResyncFailedFinal = super.isAllowDataReadingAfterResyncFailedFinal();
	@Override
	public boolean isAllowDataReadingAfterResyncFailedFinal() {return v_allowDataReadingAfterResyncFailedFinal;}
	public void setAllowDataReadingAfterResyncFailedFinal(boolean newValue) {v_allowDataReadingAfterResyncFailedFinal = newValue;}

	/** Default: true; whether it is allowed to keep collecting updates when read/resync has been failing for more than 1 full cycle (allowing it has obvious memory costs) */
	private volatile boolean v_allowUpdatesCollectionForMultipleFullCycles = super.isAllowUpdatesCollectionForMultipleFullCycles();
	@Override
	public boolean isAllowUpdatesCollectionForMultipleFullCycles() {return v_allowUpdatesCollectionForMultipleFullCycles;}
	public void setAllowUpdatesCollectionForMultipleFullCycles(boolean newValue) {v_allowUpdatesCollectionForMultipleFullCycles = newValue;}
	
	
	/** Default: Thread.NORM_PRIORITY + 1; priority to be used for read queue processing thread */ 
	private volatile int v_readQueueProcessingThreadPriority = super.getReadQueueProcessingThreadPriority();
	@Override
	public int getReadQueueProcessingThreadPriority() {return v_readQueueProcessingThreadPriority;}
	public void setReadQueueProcessingThreadPriority(int newValue) {v_readQueueProcessingThreadPriority = newValue;}
	
	/** Default: 100ms; zero value disables batching functionality; how long read queue processor will wait for the next read item before declaring the batch finished; only useful if batching is used; TO-DO add info */
	private volatile long v_readQueueBatchingDelay = super.getReadQueueBatchingDelay();
	@Override
	public long getReadQueueBatchingDelay() {return v_readQueueBatchingDelay;}
	public void setReadQueueBatchingDelay(long newValue) {v_readQueueBatchingDelay = newValue;}
	
	/** Min value and max value for read thread pool size (reads are normally executed in separate threads); -1, -1 indicates that there's no read thread pool and instead reads are performed in the read queue processing thread -- in this case you should typically provide your own async processing to e.g. support batching */
	private volatile Pair<Integer, Integer> v_readThreadPoolSize = super.getReadThreadPoolSize();
	@Override
	public Pair<Integer, Integer> getReadThreadPoolSize() {return v_readThreadPoolSize;}
	public void setReadThreadPoolSize(Pair<Integer, Integer> newValue) {v_readThreadPoolSize = newValue;}
	
	/** Default: Thread.NORM_PRIORITY; priority for threads in read pool */
	private volatile int v_readThreadPoolPriority = super.getReadThreadPoolPriority();
	@Override
	public int getReadThreadPoolPriority() {return v_readThreadPoolPriority;}
	public void setReadThreadPoolPriority(int newValue) {v_readThreadPoolPriority = newValue;}
	
	/** Default: 60s; max idle time for threads in read pool (if idle exceeds minimum thread count) */
	private volatile long v_readThreadPoolMaxIdleTime = super.getReadThreadPoolMaxIdleTime();
	@Override
	public long getReadThreadPoolMaxIdleTime() {return v_readThreadPoolMaxIdleTime;}
	public void setReadThreadPoolMaxIdleTime(long newValue) {v_readThreadPoolMaxIdleTime = newValue;}
	
	/** Specifies how many times read failures are retried; this only affects standard implementation of retry checking */
	private volatile int v_readFailureMaxRetryCount = super.getReadFailureMaxRetryCount();
	@Override
	public int getReadFailureMaxRetryCount() {return v_readFailureMaxRetryCount;}
	public void setReadFailureMaxRetryCount(int newValue) {v_readFailureMaxRetryCount = newValue;}


	/** Default: Thread.NORM_PRIORITY + 1; priority to be used for write queue processing thread */ 
	private volatile int v_writeQueueProcessingThreadPriority = super.getWriteQueueProcessingThreadPriority();
	@Override
	public int getWriteQueueProcessingThreadPriority() {return v_writeQueueProcessingThreadPriority;}
	public void setWriteQueueProcessingThreadPriority(int newValue) {v_writeQueueProcessingThreadPriority = newValue;}
	
	/** Default: 100ms; zero value disables batching functionality; how long write queue processor will wait for the next write item before declaring the batch finished; only useful if batching is used; TO-DO add info */
	private volatile long v_writeQueueBatchingDelay = super.getWriteQueueBatchingDelay();
	@Override
	public long getWriteQueueBatchingDelay() {return v_writeQueueBatchingDelay;}
	public void setWriteQueueBatchingDelay(long newValue) {v_writeQueueBatchingDelay = newValue;}
	
	/** Min value and max value for write thread pool size (writes are normally executed in separate threads); -1, -1 indicates that there's no write thread pool and instead writes are performed in the write queue processing thread -- in this case you should typically provide your own async processing to e.g. support batching */
	private volatile Pair<Integer, Integer> v_writeThreadPoolSize = super.getWriteThreadPoolSize();
	@Override
	public Pair<Integer, Integer> getWriteThreadPoolSize() {return v_writeThreadPoolSize;}
	public void setWriteThreadPoolSize(Pair<Integer, Integer> newValue) {v_writeThreadPoolSize = newValue;}
	
	/** Default: Thread.NORM_PRIORITY; priority for threads in write pool */
	private volatile int v_writeThreadPoolPriority = super.getWriteThreadPoolPriority();
	@Override
	public int getWriteThreadPoolPriority() {return v_writeThreadPoolPriority;}
	public void setWriteThreadPoolPriority(int newValue) {v_writeThreadPoolPriority = newValue;}
	
	/** Default: 60s; max idle time for threads in write pool (if idle exceeds minimum thread count) */
	private volatile long v_writeThreadPoolMaxIdleTime = super.getWriteThreadPoolMaxIdleTime();
	@Override
	public long getWriteThreadPoolMaxIdleTime() {return v_writeThreadPoolMaxIdleTime;}
	public void setWriteThreadPoolMaxIdleTime(long newValue) {v_writeThreadPoolMaxIdleTime = newValue;}
	
	/** Specifies how many times write failures are retried; this only affects standard implementation of retry checking */
	private volatile int v_writeFailureMaxRetryCount = super.getWriteFailureMaxRetryCount();
	@Override
	public int getWriteFailureMaxRetryCount() {return v_writeFailureMaxRetryCount;}
	public void setWriteFailureMaxRetryCount(int newValue) {v_writeFailureMaxRetryCount = newValue;}

	
	/** Default: Thread.NORM_PRIORITY + 1; priority to be used for main queue processing thread */ 
	private volatile int v_mainQueueProcessingThreadPriority = super.getMainQueueProcessingThreadPriority();
	@Override
	public int getMainQueueProcessingThreadPriority() {return v_mainQueueProcessingThreadPriority;}
	public void setMainQueueProcessingThreadPriority(int newValue) {v_mainQueueProcessingThreadPriority = newValue;}
	
	/** Specifies how many times full cache cycle failures are retried (e.g. when item resync or write fails); this only affects standard implementation of retry checking */
	private volatile int v_fullCacheCycleFailureMaxRetryCount = super.getFullCacheCycleFailureMaxRetryCount();
	@Override
	public int getFullCacheCycleFailureMaxRetryCount() {return v_fullCacheCycleFailureMaxRetryCount;}
	public void setFullCacheCycleFailureMaxRetryCount(int newValue) {v_fullCacheCycleFailureMaxRetryCount = newValue;}

	
	/** Default: Thread.NORM_PRIORITY + 1; priority to be used for return queue processing thread */ 
	private volatile int v_returnQueueProcessingThreadPriority = super.getReturnQueueProcessingThreadPriority();
	@Override
	public int getReturnQueueProcessingThreadPriority() {return v_returnQueueProcessingThreadPriority;}
	public void setReturnQueueProcessingThreadPriority(int newValue) {v_returnQueueProcessingThreadPriority = newValue;}
	
	/** How many times item can be re-queued in return queue before giving up (affects default implementation of 'write pending' state) */
	private volatile int v_returnQueueMaxRequeueCount = super.getReturnQueueMaxRequeueCount();
	@Override
	public int getReturnQueueMaxRequeueCount() {return v_returnQueueMaxRequeueCount;}
	public void setReturnQueueMaxRequeueCount(int newValue) {v_returnQueueMaxRequeueCount = newValue;}
	
	
	/** Default: 3; how many times cache may attempt/retry to get element by key before giving up due to 'removed from cache' element state; this shouldn't normally happen more than once */
	private volatile int v_maxCacheRemovedRetries = super.getMaxCacheRemovedRetries();
	@Override
	public int getMaxCacheRemovedRetries() {return v_maxCacheRemovedRetries;}
	public void setMaxCacheRemovedRetries(int newValue) {v_maxCacheRemovedRetries = newValue;}
	
	/** Default: 500ms; no thread is allowed to sleep/wait for blocking operation longer than this time at a time; this helps with e.g. handling shutdown and/or changing time factor and possibly other issues; you might want to increase this if your read/write methods routinely wait for longer */
	private volatile long v_maxSleepTime = super.getMaxSleepTime();
	@Override
	public long getMaxSleepTime() {return v_maxSleepTime;}
	public void setMaxSleepTime(long newValue) {v_maxSleepTime = newValue;}
	
	/** Default: false; whether cache may accept out-of-order (unexpected) reads -- useful for implementations that may perform additional reads at the times not expected by the default cache implementation; see also {@link WriteBehindResyncInBackgroundCache#spiWriteLockIsAcceptOutOfOrderRead(Object, Object, WBRBCacheEntry, com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)} */ 
	private volatile boolean v_acceptOutOfOrderReads = super.isAcceptOutOfOrderReads();
	@Override
	public boolean isAcceptOutOfOrderReads() {return v_acceptOutOfOrderReads;}
	public void setAcceptOutOfOrderReads(boolean newValue) {v_acceptOutOfOrderReads = newValue;}
	
	
	/** Default: 10s; 'time window' over which log throttling calculates maximum number of allowed messages (per message type) */
	private volatile long v_logThrottleTimeInterval = super.getLogThrottleTimeInterval();
	@Override
	public long getLogThrottleTimeInterval() {return v_logThrottleTimeInterval;}
	public void setLogThrottleTimeInterval(long newValue) {v_logThrottleTimeInterval = newValue;}

	/** Default: 10; zero value disabled throttling; how many messages of a single type can be logged per log throttling 'time window' */
	private volatile int v_logThrottleMaxMessagesOfTypePerTimeInterval = super.getLogThrottleMaxMessagesOfTypePerTimeInterval();
	@Override
	public int getLogThrottleMaxMessagesOfTypePerTimeInterval() {return v_logThrottleMaxMessagesOfTypePerTimeInterval;}
	public void setLogThrottleMaxMessagesOfTypePerTimeInterval(int newValue) {v_logThrottleMaxMessagesOfTypePerTimeInterval = newValue;}
	
	
	/** Default: false (for performance); if enabled, various events will be passed to {@link WriteBehindResyncInBackgroundCache#spiUnknownLock_Event(com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBEvent, Object, com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload, Throwable, Object...)} */
	private volatile boolean v_eventNotificationEnabled = super.isEventNotificationEnabled();
	@Override
	public boolean isEventNotificationEnabled() {return v_eventNotificationEnabled;}
	public void setEventNotificationEnabled(boolean newValue) {v_eventNotificationEnabled = newValue;}
	
	/** Default: 1,2,3,4,9 ; a list of exactly 5 int thresholds to be used as thresholds (equal or less) for monitoring 'for how many full cycles items is in the cache' at the end of return queue; values MUST be in ascending order */
	private volatile List<Integer> v_monitoringFullCacheCyclesThresholds = super.getMonitoringFullCacheCyclesThresholds();
	@Override
	public List<Integer> getMonitoringFullCacheCyclesThresholds() {return v_monitoringFullCacheCyclesThresholds;}
	public void setMonitoringFullCacheCyclesThresholds(List<Integer> newValue) {v_monitoringFullCacheCyclesThresholds = newValue;}
		
	
	/** Default: 5s,10s,15s,20s,25s ; a list of exactly 5 time intervals to be used as thresholds (equal or less) for monitoring 'time since last access' at the end of return queue; values MUST be in ascending order */
	private volatile List<Long> v_monitoringTimeSinceAccessThresholds = super.getMonitoringTimeSinceAccessThresholds();
	@Override
	public List<Long> getMonitoringTimeSinceAccessThresholds() {return v_monitoringTimeSinceAccessThresholds;}
	public void setMonitoringTimeSinceAccessThresholds(List<Long> newValue) {v_monitoringTimeSinceAccessThresholds = newValue;}
		

	/**
	 * @param initializeFrom
	 * @throws MissingResourceException
	 * @throws NumberFormatException
	 */
	public GeneratedVolatileWBRBConfig(BaseOptions initializeFrom)
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
	public GeneratedVolatileWBRBConfig(FlatConfiguration configuration)
		throws MissingResourceException,
		NumberFormatException
	{
		super(configuration);
	}
}
