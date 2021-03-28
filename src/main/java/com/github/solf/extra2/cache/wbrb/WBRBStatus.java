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

import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheControlState;
import com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * {@link WriteBehindResyncInBackgroundCache} cache status (for e.g. monitoring).
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
@ToString
@AllArgsConstructor
public class WBRBStatus
{
	/**
	 * Indicates when status was created (e.g. for caching purposes).
	 */
	@Getter
	private final long statusCreatedAt;
	
	/**
	 * Whether cache is alive (that is it was started and not stopped yet). 
	 */
	@Getter
	private final boolean cacheAlive;
	
	/**
	 * Whether cache is usable -- that is standard read & write operations
	 * can be performed; this can differ from {@link #isAlive()} value for various
	 * reasons such as cache flush. 
	 */
	@Getter
	private final boolean cacheUsable;
	
	/**
	 * Current cache control state as enum -- NOT_STARTED, RUNNING, SHUTDOWN...
	 */
	@Getter
	private final WBRBCacheControlState cacheControlState;
	
	/**
	 * Current cache control state as String (from WBRBCacheControlState) -- NOT_STARTED, RUNNING, SHUTDOWN...
	 */
	@Getter
	private final String cacheControlStateString;
	
	/**
	 * Whether thread is alive.
	 */
	@Getter
	private final boolean readQueueProcessingThreadAlive;
	
	/**
	 * Whether thread is alive.
	 */
	@Getter
	private final boolean writeQueueProcessingThreadAlive;
	
	/**
	 * Whether thread is alive.
	 */
	@Getter
	private final boolean mainQueueProcessingThreadAlive;
	
	/**
	 * Whether thread is alive.
	 */
	@Getter
	private final boolean returnQueueProcessingThreadAlive;
	
	/**
	 * Whether thread pool is alive (false if there's no pool).
	 */
	@Getter
	private final boolean readThreadPoolAlive;
	
	/**
	 * Whether thread pool is alive (false if there's no pool).
	 */
	@Getter
	private final boolean writeThreadPoolAlive;
	
	/**
	 * Number of currently active threads in the pool; -1 if there's no pool.
	 */
	@Getter
	private final int readThreadPoolActiveThreads;
	
	/**
	 * Number of currently active threads in the pool; -1 if there's no pool.
	 */
	@Getter
	private final int writeThreadPoolActiveThreads;
	
	/**
	 * Whether cache itself AND all the threads & thread pools required for the 
	 * cache operation are still alive; if pools don't exist, they don't affect this value.
	 */
	@Getter
	private final boolean everythingAlive;
	
	
	
	/**
	 * Current cache size.
	 */
	@Getter
	private final long currentCacheSize;
	
	/**
	 * Main processing queue size.
	 */
	@Getter
	private final long mainQueueSize;
	
	/**
	 * Return queue size.
	 */
	@Getter
	private final long returnQueueSize;
	
	/**
	 * Read queue size.
	 */
	@Getter
	private final long readQueueSize;
	
	/**
	 * Write queue size.
	 */
	@Getter
	private final long writeQueueSize;
	
	
	
	/**
	 * Main queue target cache time (in ms) (as configured)
	 */
	@Getter
	private final long configMainQueueCacheTimeMs;
	
	/**
	 * Return queue minimum/target cache time (in ms) (as configured)
	 */
	@Getter
	private final long configReturnQueueCacheTimeMinMs;
	
	/**
	 * Maximum allowed targeted (it's possible to exceed this value) queue size
	 * for the main processing queue (as configured)
	 */
	@Getter
	private final long configMainQueueMaxTargetSize;
	
	/**
	 * Hard limit on the cache size (as configured)
	 */
	@Getter
	private final long configMaxCacheElementsHardLimit;
	
	
	
	/**
	 * How many items were processed out of read queue.
	 */
	@Getter
	private final long storageReadQueueProcessedItems;
	
	/**
	 * How many read attempts (total, initial + refresh) were made.
	 */
	@Getter
	private final long storageReadTotalAttempts;
	
	/**
	 * How many reads (total, initial + refresh) succeeded (via {@link WriteBehindResyncInBackgroundCache#apiStorageReadSuccess(Object, WBRBCacheEntry)}
	 */
	@Getter
	private final long storageReadTotalSuccesses;
	
	/**
	 * How many reads (total, initial + refresh) failed (via {@link WriteBehindResyncInBackgroundCache#apiStorageReadFail(Throwable, WBRBCacheEntry)}
	 */
	@Getter
	private final long storageReadTotalFailures;
	
	/**
	 * How many read (refresh) attempts were made.
	 */
	@Getter
	private final long storageReadRefreshAttempts;
	
	/**
	 * How many reads (refresh) succeeded (via {@link WriteBehindResyncInBackgroundCache#apiStorageReadSuccess(Object, WBRBCacheEntry)}
	 */
	@Getter
	private final long storageReadRefreshSuccesses;
	
	/**
	 * How many reads (refresh) failed (via {@link WriteBehindResyncInBackgroundCache#apiStorageReadFail(Throwable, WBRBCacheEntry)}
	 */
	@Getter
	private final long storageReadRefreshFailures;
	
	/**
	 * How many reads (refresh) arrived too late for a proper resync.
	 */
	@Getter
	private final long storageReadRefreshTooLateCount;
	
	/**
	 * How many reads (refresh) arrived but data was not used (not set/merged) for
	 * whatever reason./
	 */
	@Getter
	private final long storageReadRefreshDataNotUsedCount;
	
	/**
	 * How many read (initial) attempts were made.
	 */
	@Getter
	private final long storageReadInitialAttempts;
	
	/**
	 * How many reads (initial) succeeded (via {@link WriteBehindResyncInBackgroundCache#apiStorageReadSuccess(Object, WBRBCacheEntry)}
	 */
	@Getter
	private final long storageReadInitialSuccesses;
	
	/**
	 * How many reads (initial) failed (via {@link WriteBehindResyncInBackgroundCache#apiStorageReadFail(Throwable, WBRBCacheEntry)}
	 */
	@Getter
	private final long storageReadInitialFailures;
	
	
	
	/**
	 * How many items were processed out of write queue.
	 */
	@Getter
	private final long storageWriteQueueProcessedItems;
	
	/**
	 * How many write attempts were made.
	 */
	@Getter
	private final long storageWriteAttempts;
	
	/**
	 * How many writes succeeded (via {@link WriteBehindResyncInBackgroundCache#apiStorageWriteSuccess(com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBWriteQueueEntry)}
	 */
	@Getter
	private final long storageWriteSuccesses;
	
	/**
	 * How many writes failed (via {@link WriteBehindResyncInBackgroundCache#apiStorageWriteFail(Throwable, com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBWriteQueueEntry)}
	 */
	@Getter
	private final long storageWriteFailures;
	

	
	/**
	 * How many items were processed out of main processing queue.
	 */
	@Getter
	private final long mainQueueProcessedItems;
	
	/**
	 * For the last processed item in the main queue -- how long it actually
	 * was in the queue (in virtual ms).
	 */
	@Getter
	private final long mainQueueLastItemInQueueDurationMs;
	
	/**
	 * How many writes were sent while processing main processing queue.
	 */
	@Getter
	private final long mainQueueSentWrites;
	
	/**
	 * How many items were expired from cache as the result of main queue processing.
	 * <p>
	 * 'expired' action indicates normal processing, no error is logged
	 */
	@Getter
	private final long mainQueueExpiredFromCacheCount;
	
	/**
	 * How many items were removed from cache as the result of main queue processing.
	 * <p>
	 * 'removed' action indicates abnormal processing, an error is logged
	 */
	@Getter
	private final long mainQueueRemovedFromCacheCount;
	
	/**
	 * How many items were requeued back to the main processing queue 
	 * as the result of main queue processing.
	 */
	@Getter
	private final long mainQueueRequeueToMainQueueCount;
	
	/**
	 * How many items weren't marked as 'all ok' as the result of main queue processing.
	 */
	@Getter
	private final long mainQueueNotAllOkCount;
	

	
	/**
	 * How many items were processed out of return queue.
	 */
	@Getter
	private final long returnQueueProcessedItems;
	
	/**
	 * For the last processed item in the return queue -- how long it actually
	 * was in the queue (in virtual ms).
	 */
	@Getter
	private final long returnQueueLastItemInQueueDurationMs;
	
	/**
	 * How many resyncs were scheduled while processing return queue.
	 */
	@Getter
	private final long returnQueueScheduledResyncs;
	
	/**
	 * How many items were processed as 'do nothing' as the result of the return queue processing.
	 * <p>
	 * This is usually not a normal behavior.
	 */
	@Getter
	private final long returnQueueDoNothingCount;
	
	/**
	 * How many items were expired from cache as the result of the return queue processing.
	 */
	@Getter
	private final long returnQueueExpiredFromCacheCount;
	
	/**
	 * How many items were removed from cache (removal is generally an 'error',
	 * non-error is 'expire') as the result of the return queue processing.
	 */
	@Getter
	private final long returnQueueRemovedFromCacheCount;
	
	/**
	 * How many items were requeued back to the return queue 
	 * as the result of the return queue processing.
	 */
	@Getter
	private final long returnQueueRequeueToReturnQueueCount;
	
	
	
	/**
	 * How many check-cache attempts were made -- these are not separated
	 * between preload & read and are not de-dupped -- one external operation
	 * may result in many internal attempts.
	 * <p>
	 * This is basically for debugging/testing purposes.
	 */
	@Getter
	private final long checkCacheAttemptsNoDedup;
	
	/**
	 * How many check-cache (via preload) attempts were made.
	 */
	@Getter
	private final long checkCachePreloadAttempts;

	/**
	 * How many check-cache (via preload) attempts hit the cache.
	 */
	@Getter
	private final long checkCachePreloadCacheHit;

	/**
	 * How many check-cache (via preload) attempts hit the 'cache full' state.
	 */
	@Getter
	private final long checkCachePreloadCacheFullExceptionCount;
	
	/**
	 * How many check-cache (via read) attempts were made.
	 */
	@Getter
	private final long checkCacheReadAttempts;

	/**
	 * How many check-cache (via read) attempts hit the cache.
	 */
	@Getter
	private final long checkCacheReadCacheHit;

	/**
	 * How many check-cache (via read) attempts hit the 'cache full' state.
	 */
	@Getter
	private final long checkCacheReadCacheFullExceptionCount;

	/**
	 * How many check-cache (via read or preload) attempts hit the 'cache full' state.
	 */
	@Getter
	private final long checkCacheTotalCacheFullExceptionCount;

	/**
	 * How many check-cache (via read or preload) attempts hit the 'null key' error.
	 */
	@Getter
	private final long checkCacheNullKeyCount;
	
	
	/**
	 * How many cache read attempts were made.
	 */
	@Getter
	private final long cacheReadAttempts;
	
	/**
	 * How many cache reads timed out (haven't got result in allowed time).
	 */
	@Getter
	private final long cacheReadTimeouts;
	
	/**
	 * How many errors during cache read occurred.
	 */
	@Getter
	private final long cacheReadErrors;
	
	
	/**
	 * How many cache write attempts were made.
	 */
	@Getter
	private final long cacheWriteAttempts;
	
	/**
	 * How many cache writes failed because relevant cache element was not present.
	 */
	@Getter
	private final long cacheWriteElementNotPresentCount;
	
	/**
	 * How many errors during cache write occurred.
	 */
	@Getter
	private final long cacheWriteErrors;
	
	/**
	 * How many times did we encounter {@link WBRBCacheMessage#TOO_MANY_CACHE_ELEMENT_UPDATES}
	 * issue (which potentially leads to data loss).
	 */
	@Getter
	private final long cacheWriteTooManyUpdates;

	
	
	/**
	 * Indicates problem that is probably caused by internal somewhat-known
	 * factors, such as potential concurrency/race conditions (which normally
	 * are not expected to occur).
	 * <p>
	 * These usually should not result in data loss.
	 */
	@Getter
	private final long msgWarnCount;
	
	/**
	 * Indicates an externally-caused warning.
	 * <p>
	 * These messages usually indicate that there was no data loss (yet).
	 */
	@Getter
	private final long msgExternalWarnCount;
	
	/**
	 * Indicates an error probably caused by external factors, such
	 * as underlying storage failing.
	 * <p>
	 * These messages usually indicate that there was no data loss (yet).
	 */
	@Getter
	private final long msgExternalErrorCount;
	
	/**
	 * Indicates an error probably caused by external factors, such
	 * as underlying storage failing.
	 * <p>
	 * This is used when data loss is highly likely, e.g. when cache implementation
	 * gives up on writing piece of data to the underlying storage.
	 */
	@Getter
	private final long msgExternalDataLossCount;
	
	/**
	 * Indicates an error which is likely to be caused by the 
	 * problems and/or unexpected behavior in the cache code itself.
	 * <p>
	 * Data loss is likely although this should not be fatal.
	 */
	@Getter
	private final long msgErrorCount;
	
	/**
	 * Indicates a likely fatal error, meaning cache may well become unusable
	 * after this happens. 
	 */
	@Getter
	private final long msgFatalCount;
	
	/**
	 * Total count of messages with severity 'warn' or higher.
	 */
	@Getter
	private final long msgTotalWarnOrHigherCount;
	
	/**
	 * Total count of messages with severity 'error' or higher.
	 */
	@Getter
	private final long msgTotalErrorOrHigherCount;
	
	
	/**
	 * Collects last message timestamps per each severity in {@link WBRBCacheMessageSeverity}
	 * <p>
	 * It is set to 0 until first matching message happens.
	 * <p>
	 * NOTE: these are tracked even if the message itself is not logged due
	 * to log severity settings or something.
	 * <p>
	 * NOTE2: those are not 'atomic' with {@link #getLastLoggedTextMsgPerSeverityOrdinal()}
	 * there can be discrepancies.
	 */
	@Getter
	private final long[] lastTimestampMsgPerSeverityOrdinal;
	
	/**
	 * Collects last message text per each severity in {@link WBRBCacheMessageSeverity}
	 * <p>
	 * It's {@link AtomicReference} contains null until matching message happens.
	 * <p>
	 * NOTE: these are tracked ONLY if message is actually sent to logging,
	 * i.e. if it passes severity & throttling check.
	 * <p>
	 * NOTE2: those are not 'atomic' with {@link #getLastTimestampMsgPerSeverityOrdinal()}
	 * there can be discrepancies.
	 */
	@Getter
	private final @Nullable String[] lastLoggedTextMsgPerSeverityOrdinal;
	

	/**
	 * Timestamp for the last message (regardless of whether it was logged)
	 * of the WARN-type severity, 0 if no such messages were logged.
	 */
	@Getter
	private final long lastWarnMsgTimestamp;
	
	/**
	 * Last logged message text of the WARN-type severity, null if no
	 * such messages were logged (this does not track messages that were not
	 * logged due to low severity or throttling).
	 */
	@Getter
	private final @Nullable String lastWarnLoggedMsgText;
	
	/**
	 * Timestamp for the last message (regardless of whether it was logged)
	 * of the ERROR-type severity, 0 if no such messages were logged.
	 */
	@Getter
	private final long lastErrorMsgTimestamp;
	
	/**
	 * Last logged message text of the ERROR-type severity, null if no
	 * such messages were logged (this does not track messages that were not
	 * logged due to low severity or throttling).
	 */
	@Getter
	private final @Nullable String lastErrorLoggedMsgText;
	
	/**
	 * Timestamp for the last message (regardless of whether it was logged)
	 * of the FATAL-type severity, 0 if no such messages were logged.
	 */
	@Getter
	private final long lastFatalMsgTimestamp;
	
	/**
	 * Last logged message text of the FATAL-type severity, null if no
	 * such messages were logged (this does not track messages that were not
	 * logged due to low severity or throttling).
	 */
	@Getter
	private final @Nullable String lastFatalLoggedMsgText;
	
}
