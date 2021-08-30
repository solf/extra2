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

import static io.github.solf.extra2.util.NullUtil.fakeNonNull;
import static io.github.solf.extra2.util.NullUtil.nn;
import static io.github.solf.extra2.util.NullUtil.nnChecked;
import static io.github.solf.extra2.util.NullUtil.nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.MissingResourceException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import io.github.solf.extra2.cache.exception.BaseCacheException;
import io.github.solf.extra2.cache.exception.CacheControlStateException;
import io.github.solf.extra2.cache.exception.CacheElementFailedLoadingException;
import io.github.solf.extra2.cache.exception.CacheElementFailedResyncException;
import io.github.solf.extra2.cache.exception.CacheElementHasTooManyUpdates;
import io.github.solf.extra2.cache.exception.CacheElementNotYetLoadedException;
import io.github.solf.extra2.cache.exception.CacheFullException;
import io.github.solf.extra2.cache.exception.CacheIllegalExternalStateException;
import io.github.solf.extra2.cache.exception.CacheIllegalStateException;
import io.github.solf.extra2.cache.exception.CacheInternalException;
import io.github.solf.extra2.cache.wbrb.WBRBStatusBuilder;
import io.github.solf.extra2.concurrent.InterruptableRunnable;
import io.github.solf.extra2.concurrent.InterruptableSupplier;
import io.github.solf.extra2.concurrent.Latch;
import io.github.solf.extra2.concurrent.WAThreadPoolExecutor;
import io.github.solf.extra2.concurrent.exception.WAInterruptedException;
import io.github.solf.extra2.lambda.BooleanObjectWrapper;
import io.github.solf.extra2.lambda.ObjectWrapper;
import io.github.solf.extra2.lambda.SimpleIntCounter;
import io.github.solf.extra2.lambda.SimpleLongCounter;
import io.github.solf.extra2.nullable.NullableOptional;
import io.github.solf.extra2.thread.ExitableThread;
import io.github.solf.extra2.thread.InterruptHandlingExitableThread;
import io.github.solf.extra2.util.TypeUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;

/**
 * TODO -- CCC marks placed where comments are needed, fixme replaced with FIX-ME, todo replaced with TO-DO to hide tasks from tracking for now
 * 
 * FIX-ME -- probably need to add percentile monitoring for the read & write times somehow
 * FIX-ME -- notes on monitoring performance impact? All these volatiles/atomics can't be great
 * FIX-ME -- comments on what happens when abstract methods fail with exception
 * FIX-ME -- cache expiration based on time since last access rather than just boolean flags?
 * FIX-ME -- option to collect updates in main queue and run write & resync when collected updates are more than some value? If doing this, will have to delay processing such items in main queue processing (to give time to finish resync)
 * FIX-ME -- option to handle unexpected resync? e.g. if write is implemented as read + write, we should be able to accept 'unexpected' resync based on this read+write; also skip resync in the return queue?
 * FIX-ME -- limit number of collected updates?
 * FIX-ME -- cache write should have (an option?) to write to element w/o reading it first; this probably means need to adjust merge decision so it can merge updates with just-read-data w/o having previous data
 * FIX-ME -- check returns inside ifPresent/ifException blocks -- they wouldn't do what might be expected
 * FIX-ME -- cache write needs flag or something to decide what to do if item is no longer in the cache; maybe also consider not-removing elements from cache if they are 'recently' accessed?; or maybe this doesn't matter, we do incremental updates anyway?
 * FIX-ME -- needs shutdown procedure
 * FIX-ME -- needs monitoring (relevant threads are alive, queue sizes, etc)
 * FIX-ME -- an option or an override to disable background resync (i.e. cache forever) option?
 * FIX-ME -- make sure not too many write changes are kept in memory in case of cassandra failure
 * FIX-ME -- make sure cassandra writes&reads have some kind of built-in timeout to do not consume thread pool & to do not make code keep change lists too long
 * FIX-ME -- reduce size of change command as much as possible
 * 
 * FIX-ME - class description
 * 
 * TO-DO add option to handle situation where 'no error, but key is not in storage, data is not going to be available'
 * TO-DO check that resync failure actually sets sensible status
 * TO-DO test this actually works with nullable cache values
 * TO-DO make sure all SPI methods indicate lock type
 * TO-DO make sure to test time delay methods
 * TO-DO rename {@link WAInterruptedException} ?
 * TO-DO do CCC comments 
 * TO-DO do CCC comments in interface 
 * TO-DO mechanism to check whether incoming update actually changes anything (i.e. needs write afterwards) or not?
 *
 * @author Sergey Olefir
 * 
 * @param <K> type of the key used in this cache; must be usable as key for
 * 		HashMaps (e.g. proper hashCode() and equals())
 * @param <V> type of the values returned by this cache
 * @param <S> type of the values internally stored by the cache (it doesn't have to
 * 		be of V type)
 * @param <R> type of the values read from the underlying storage; doesn't need
 * 		to be the same as anything else, but methods must be implemented to
 * 		convert/merge it to S 
 * @param <W> type of the values written to the underlying storage; doesn't need
 * 		to be of V or S types
 * @param <UExt> type of the values used to update data in this cache externally
 * 		(in public API)
 * @param <UInt> type of the values used to store & apply updates internally
 * 		(converted from <UExt>)
 */
// Exclude TYPE_ARGUMENT as we will allow null cache values.
@NonNullByDefault({DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE, DefaultLocation.FIELD, DefaultLocation.TYPE_BOUND, DefaultLocation.ARRAY_CONTENTS}) 
public abstract class WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, W, UExt, UInt>
	implements IWriteBehindResyncInBackgroundCacheWithControlMethods<K, V, S, R, W, UExt, UInt>
{
	/**
	 * Config for this cache.
	 */
	protected final WBRBConfig config;
	
	/**
	 * Pre-prepared common naming prefix, e.g. "WBRBCache[cacheName]"
	 */
	protected final String commonNamingPrefix;
	
	/**
	 * Cache control state -- 	 not_started, running, shutdown...
	 * <p>
	 * Used to allow/disallow various operations.
	 */
	protected /*non-final to allow subclasses to override*/ AtomicReference<@Nonnull WBRBCacheControlState> controlState = 
		new AtomicReference<>(WBRBCacheControlState.NOT_STARTED);
	
	/**
	 * Cached status of the cache if previously calculated.
	 */
	@Nullable
	protected volatile WBRBStatus cachedStatus;
	
	/**
	 * Inflight data map -- used to quickly access in-flight values via their keys.
	 * <p>
	 * Note that due to various concurrency issues even if value is stored in
	 * this map -- it might already have become 'outdated' and needs to be re-read.
	 * <p>
	 * ATTENTION: if anything is added to this map, it has to be also added to
	 * {@link #mainQueue} or TO-DO
	 * TO-DO is above about outdated correct? 
	 * TO-DO any reason to make initial size configurable?
	 * TO-DO monitor size
	 * FIX-ME make absolutely sure that there are no stray items in this map (e.g. removed from main or return queue and not removed from here due to an exception)
	 */
	protected final ConcurrentHashMap<K, WBRBCacheEntry> inflightMap = new ConcurrentHashMap<>(1024);
	
	/**
	 * Queue for data in the main processing pipeline.
	 * 
	 * TO-DO is this the best choice for the queue here?
	 */
	protected final LinkedBlockingQueue<WBRBCacheEntry> mainQueue = new LinkedBlockingQueue<>();
	
	/**
	 * Queue for data in the main processing pipeline.
	 * 
	 * TO-DO is this the best choice for the queue here?
	 */
	protected final LinkedBlockingQueue<WBRBCacheEntry> returnQueue = new LinkedBlockingQueue<>();
	
	/**
	 * Queue for data to be read from the storage.
	 * 
	 * TO-DO is this the best choice for the queue here?
	 */
	protected final LinkedBlockingQueue<WBRBCacheEntry> readQueue = new LinkedBlockingQueue<>();
	
	/**
	 * Queue for data to be read from the storage.
	 * 
	 * TO-DO is this the best choice for the queue here?
	 */
	protected final LinkedBlockingQueue<WBRBWriteQueueEntry> writeQueue = new LinkedBlockingQueue<>();
	
	/**
	 * Thread group used for this cache.
	 */
	protected final ThreadGroup threadGroup;
	
	
	/**
	 * Thread for processing read queue.
	 */
	protected final ExitableThread readQueueProcessingThread;
	
	/**
	 * Read pool for performing asynchronous data reads from storage.
	 * <p>
	 * Can be null, in which case reads are performed synchronously in
	 * {@link #readQueueProcessingThread}; this is typically only useful if
	 * there's some other form of making this asynchronous, such as batch processing.
	 * TO-DO expand comment
	 * FIX-ME monitor size
	 */
	@Nullable
	protected final WAThreadPoolExecutor readThreadPool; 
	
	/**
	 * Thread for processing write queue.
	 */
	protected final ExitableThread writeQueueProcessingThread;
	
	/**
	 * Thread for processing return queue.
	 */
	protected final ExitableThread returnQueueProcessingThread;
	
	/**
	 * Write pool for performing asynchronous data writes to storage.
	 * <p>
	 * Can be null, in which case writes are performed synchronously in
	 * {@link #writeQueueProcessingThread}; this is typically only useful if
	 * there's some other form of making this asynchronous, such as batch processing.
	 * TO-DO expand comment
	 * FIX-ME monitor size
	 */
	@Nullable
	protected final WAThreadPoolExecutor writeThreadPool; 
	
	/**
	 * Thread for processing main queue.
	 */
	protected final ExitableThread mainQueueProcessingThread;
	
	/**
	 * Stats for this cache.
	 * 
	 * @deprecated use {@link #getStats()} instead; deprecated only to ensure no
	 * 		accidental access to the field
	 */
	@Deprecated
	protected final WBRBStats internalStatsField = new WBRBStats();
	/**
	 * Gets stats for this cache instance, separated in a method in case
	 * subclasses want to use their own implementation.
	 */
	protected WBRBStats getStats()
	{
		return internalStatsField;
	}
	
	/**
	 * Sets event logger used for debug logging in default implementation of
	 * {@link #spiUnknownLock_Event(WBRBEvent, Object, WBRBCacheEntry, WBRBCachePayload, Throwable, Object...)}
	 * <p>
	 * Events are logged with INFO severity.
	 * <p>
	 * Set to null to disable logging.
	 * <p>
	 * This only has effect if 'eventNotificationEnabled' configuration option
	 * is set to true.
	 * <p>
	 * NOTE: the way the return value is written combined with Eclipse null-analysis
	 * might lead to being unable to override this method in a subclass due to
	 * an apparent Eclipse compiler's failure (both 'doesn't override' and 
	 * 'same erasure' messages at the same time); in which case the workaround
	 * is to implement a method with a different name.
	 */
	@Getter(AccessLevel.PROTECTED)
	@Nullable
	protected volatile Logger debugEventLogger = null;
	protected <C extends IWriteBehindResyncInBackgroundCacheWithControlMethods<K, V, S, R, W, UExt, UInt>> C 
		setDebugEventLogger(@Nullable Logger newDebugEventLogger)
	{
		debugEventLogger = newDebugEventLogger;
		
		return TypeUtil.coerce(this);
	}
	
	/**
	 * Logged used by default implementation in {@link #spiUnknownLockGetLogger(WBRBCacheMessage, Throwable, Object...)}
	 * AND as final fallback if logging fails repeatedly.
	 */
	private static final Logger defaultWBRBlog = LoggerFactory.getLogger(WriteBehindResyncInBackgroundCache.class);		
	
	/**
	 * Stats collected by this cache
	 * <p>
	 * Fields are not final in case there needs to be a subclass of {@link WBRBStats}
	 * that replaces some of the monitors.
	 * <p>
	 * Class and fields are public in order to make it more realistic for subclasses
	 * to override behavior if needed (such as put custom handling on value changes
	 * or something).
	 * <p>
	 * This class is still only intended to be used internally for stats collection.
	 */
	public static class WBRBStats
	{
		/**
		 * Maximum ordinal for the {@link WBRBCacheMessageSeverity}
		 */
		public static final int MAX_SEVERITY_ORDINAL;
		static
		{
			{
				int maxOrdinal = 1;
				for (WBRBCacheMessageSeverity entry : WBRBCacheMessageSeverity.values())
				{
					maxOrdinal = Math.max(maxOrdinal, entry.ordinal());
				}
				
				MAX_SEVERITY_ORDINAL = maxOrdinal;
			}
		}
		
		/**
		 * How many items were processed out of read queue.
		 */
		public AtomicLong storageReadQueueProcessedItems = new AtomicLong(0);
		
		/**
		 * How many read (refresh) attempts were made.
		 */
		public AtomicLong storageReadRefreshAttempts = new AtomicLong(0);
		
		/**
		 * How many reads (refresh) succeeded (via {@link WriteBehindResyncInBackgroundCache#apiStorageReadSuccess(Object, WBRBCacheEntry)}
		 */
		public AtomicLong storageReadRefreshSuccesses = new AtomicLong(0);
		
		/**
		 * How many reads (refresh) failed (via {@link WriteBehindResyncInBackgroundCache#apiStorageReadFail(Throwable, WBRBCacheEntry)}
		 */
		public AtomicLong storageReadRefreshFailures = new AtomicLong(0);
		
		/**
		 * How many reads (refresh) arrived too late for a proper resync.
		 */
		public AtomicLong storageReadRefreshTooLateCount = new AtomicLong(0);
		
		/**
		 * How many reads (refresh) arrived but data was not used (not set/merged) for
		 * whatever reason./
		 */
		public AtomicLong storageReadRefreshDataNotUsedCount = new AtomicLong(0);
		
		/**
		 * How many read (initial) attempts were made.
		 */
		public AtomicLong storageReadInitialAttempts = new AtomicLong(0);
		
		/**
		 * How many reads (initial) succeeded (via {@link WriteBehindResyncInBackgroundCache#apiStorageReadSuccess(Object, WBRBCacheEntry)}
		 */
		public AtomicLong storageReadInitialSuccesses = new AtomicLong(0);
		
		/**
		 * How many reads (initial) failed (via {@link WriteBehindResyncInBackgroundCache#apiStorageReadFail(Throwable, WBRBCacheEntry)}
		 */
		public AtomicLong storageReadInitialFailures = new AtomicLong(0);
		
		
		
		/**
		 * How many items were processed out of write queue.
		 */
		public AtomicLong storageWriteQueueProcessedItems = new AtomicLong(0);
		
		/**
		 * How many write attempts were made.
		 */
		public AtomicLong storageWriteAttempts = new AtomicLong(0);
		
		/**
		 * How many writes succeeded (via {@link WriteBehindResyncInBackgroundCache#apiStorageWriteSuccess(io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBWriteQueueEntry)}
		 */
		public AtomicLong storageWriteSuccesses = new AtomicLong(0);
		
		/**
		 * How many writes failed (via {@link WriteBehindResyncInBackgroundCache#apiStorageWriteFail(Throwable, io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBWriteQueueEntry)}
		 */
		public AtomicLong storageWriteFailures = new AtomicLong(0);
		

		
		/**
		 * How many items were processed out of main processing queue.
		 */
		public AtomicLong mainQueueProcessedItems = new AtomicLong(0);
		
		/**
		 * For the last processed item in the main queue -- how long it actually
		 * was in the queue (in virtual ms).
		 * <p>
		 * Use AtomicLong here instead of e.g. volatile long for the possiblity
		 * to override functionality later.
		 */
		public AtomicLong mainQueueLastItemInQueueDurationMs = new AtomicLong(0);
		
		/**
		 * How many writes were sent while processing main processing queue.
		 */
		public AtomicLong mainQueueSentWrites = new AtomicLong(0);
		
		/**
		 * How many items were expired from cache as the result of main queue processing.
		 * <p>
		 * 'expired' action indicates normal processing, no error is logged
		 */
		public AtomicLong mainQueueExpiredFromCacheCount = new AtomicLong(0);
		
		/**
		 * How many items were removed from cache as the result of main queue processing.
		 * <p>
		 * 'removed' action indicates abnormal processing, an error is logged
		 */
		public AtomicLong mainQueueRemovedFromCacheCount = new AtomicLong(0);
		
		/**
		 * How many items were requeued back to the main processing queue 
		 * as the result of main queue processing.
		 */
		public AtomicLong mainQueueRequeueToMainQueueCount = new AtomicLong(0);
		
		/**
		 * How many items weren't marked as 'all ok' as the result of main queue processing.
		 */
		public AtomicLong mainQueueNotAllOkCount = new AtomicLong(0);
		

		
		/**
		 * How many items were processed out of return queue.
		 */
		public AtomicLong returnQueueProcessedItems = new AtomicLong(0);
		
		/**
		 * For the last processed item in the return queue -- how long it actually
		 * was in the queue (in virtual ms).
		 */
		public AtomicLong returnQueueLastItemInQueueDurationMs = new AtomicLong(0);
		
		/**
		 * How many resyncs were scheduled while processing return queue.
		 */
		public AtomicLong returnQueueScheduledResyncs = new AtomicLong(0);
		
		/**
		 * How many items were processed as 'do nothing' as the result of the return queue processing.
		 * <p>
		 * This is usually not a normal behavior.
		 */
		public AtomicLong returnQueueDoNothingCount = new AtomicLong(0);
		
		/**
		 * How many items were expired from cache as the result of the return queue processing.
		 */
		public AtomicLong returnQueueExpiredFromCacheCount = new AtomicLong(0);
		
		/**
		 * How many items were removed from cache (removal is generally an 'error',
		 * non-error is 'expire') as the result of the return queue processing.
		 */
		public AtomicLong returnQueueRemovedFromCacheCount = new AtomicLong(0);
		
		/**
		 * How many items were requeued back to the return queue 
		 * as the result of the return queue processing.
		 */
		public AtomicLong returnQueueRequeueToReturnQueueCount = new AtomicLong(0);
		
		/**
		 * In return queue processing we calculate time since last access in
		 * order to determine what to do with the cache item; this counts how
		 * many times that resulted in the negative value (this should not
		 * normally happen, but could possibly happen if time is adjusted or
		 * some such).
		 */
		public AtomicLong returnQueueNegativeTimeSinceLastAccessErrorCount = new AtomicLong(0);
		
		/**
		 * In return queue processing there's decision as to whether to keep an
		 * element in the cache; this monitors cases when item is ineligible to
		 * be retained due to main queue size already being at the limit.
		 */
		public AtomicLong returnQueueItemNotRetainedDueToMainQueueSizeCount = new AtomicLong(0);
		
		
		
		/**
		 * How many check-cache attempts were made -- these are not separated
		 * between preload & read and are not de-dupped -- one external operation
		 * may result in many internal attempts.
		 * <p>
		 * This is basically for debugging/testing purposes.
		 */
		public AtomicLong checkCacheAttemptsNoDedup = new AtomicLong(0);
		
		/**
		 * How many check-cache (via preload) attempts were made.
		 */
		public AtomicLong checkCachePreloadAttempts = new AtomicLong(0);

		/**
		 * How many check-cache (via preload) attempts hit the cache.
		 */
		public AtomicLong checkCachePreloadCacheHit = new AtomicLong(0);

		/**
		 * How many check-cache (via preload) attempts hit the 'cache full' state.
		 */
		public AtomicLong checkCachePreloadCacheFullExceptionCount = new AtomicLong(0);
		
		/**
		 * How many check-cache (via read) attempts were made.
		 */
		public AtomicLong checkCacheReadAttempts = new AtomicLong(0);

		/**
		 * How many check-cache (via read) attempts hit the cache.
		 */
		public AtomicLong checkCacheReadCacheHit = new AtomicLong(0);

		/**
		 * How many check-cache (via read) attempts hit the 'cache full' state.
		 */
		public AtomicLong checkCacheReadCacheFullExceptionCount = new AtomicLong(0);

		/**
		 * How many check-cache (via read or preload) attempts hit the 'cache full' state.
		 */
		public AtomicLong checkCacheTotalCacheFullExceptionCount = new AtomicLong(0);
		
		/**
		 * How many check-cache (via read or preload) attempts hit the 'null key' error.
		 */
		public AtomicLong checkCacheNullKeyCount = new AtomicLong(0);
		
		
		/**
		 * How many cache read attempts were made.
		 */
		public AtomicLong cacheReadAttempts = new AtomicLong(0);
		
		/**
		 * How many cache reads timed out (haven't got result in allowed time).
		 */
		public AtomicLong cacheReadTimeouts = new AtomicLong(0);
		
		/**
		 * How many errors during cache read occurred.
		 */
		public AtomicLong cacheReadErrors = new AtomicLong(0);
		
		/**
		 * How many interrupts (external) during cache read occurred.
		 */
		public AtomicLong cacheReadInterrupts = new AtomicLong(0);
		
		
		/**
		 * How many cache write attempts were made.
		 */
		public AtomicLong cacheWriteAttempts = new AtomicLong(0);
		
		/**
		 * How many cache writes failed because relevant cache element was not present.
		 */
		public AtomicLong cacheWriteElementNotPresentCount = new AtomicLong(0);
		
		/**
		 * How many errors during cache write occurred.
		 */
		public AtomicLong cacheWriteErrors = new AtomicLong(0);
		
		/**
		 * How many times did we encounter {@link WBRBCacheMessage#TOO_MANY_CACHE_ELEMENT_UPDATES}
		 * issue (which potentially leads to data loss).
		 */
		public AtomicLong cacheWriteTooManyUpdates = new AtomicLong(0);

		
		
		/**
		 * Indicates problem that is probably caused by internal somewhat-known
		 * factors, such as potential concurrency/race conditions (which normally
		 * are not expected to occur).
		 * <p>
		 * These usually should not result in data loss.
		 */
		public AtomicLong msgWarnCount = new AtomicLong(0);
		
		/**
		 * Indicates an externally-caused warning.
		 * <p>
		 * These messages usually indicate that there was no data loss (yet).
		 */
		public AtomicLong msgExternalWarnCount = new AtomicLong(0);
		
		/**
		 * Indicates an error probably caused by external factors, such
		 * as underlying storage failing.
		 * <p>
		 * These messages usually indicate that there was no data loss (yet).
		 */
		public AtomicLong msgExternalErrorCount = new AtomicLong(0);
		
		/**
		 * Indicates an error probably caused by external factors, such
		 * as underlying storage failing.
		 * <p>
		 * This is used when data loss is highly likely, e.g. when cache implementation
		 * gives up on writing piece of data to the underlying storage.
		 */
		public AtomicLong msgExternalDataLossCount = new AtomicLong(0);
		
		/**
		 * Indicates an error which is likely to be caused by the 
		 * problems and/or unexpected behavior in the cache code itself.
		 * <p>
		 * Data loss is likely although this should not be fatal.
		 */
		public AtomicLong msgErrorCount = new AtomicLong(0);
		
		/**
		 * Indicates a likely fatal error, meaning cache may well become unusable
		 * after this happens. 
		 */
		public AtomicLong msgFatalCount = new AtomicLong(0);
		
		/**
		 * Collects last message timestamps per each severity in {@link WBRBCacheMessageSeverity}
		 * <p>
		 * It is set to 0 until first matching message happens.
		 * <p>
		 * NOTE: these are tracked even if the message itself is not logged due
		 * to log severity settings or something.
		 */
		public AtomicLong[] lastTimestampMsgPerSeverityOrdinal;
		
		/**
		 * Collects last message text per each severity in {@link WBRBCacheMessageSeverity}
		 * <p>
		 * It's {@link AtomicReference} contains null until matching message happens.
		 * <p>
		 * NOTE: these are tracked ONLY if message is actually sent to logging,
		 * i.e. if it passes severity & throttling check.
		 */
		public AtomicReference<@Nullable String>[] lastLoggedTextMsgPerSeverityOrdinal;
		
		/**
		 * 6-item list of 5 'equal or less than' counters (per each threshold) and 
		 * sixth one of 'more than'
		 * 
		 * @see WBRBConfig#getMonitoringFullCacheCyclesThresholds()
		 */
		public AtomicLong[] fullCycleCountThresholdCounters;
		
		/**
		 * 6-item list of 5 'equal or less than' counters (per each threshold) and 
		 * sixth one of 'more than'
		 * 
		 * @see WBRBConfig#getMonitoringTimeSinceAccessThresholds()
		 */
		public AtomicLong[] timeSinceLastAccessThresholdCounters;
		
		/**
		 * Constructor.
		 */
		public WBRBStats()
		{
			{
				lastTimestampMsgPerSeverityOrdinal = TypeUtil.coerce(new AtomicLong[MAX_SEVERITY_ORDINAL + 1]); // must be + 1 for last index to work!
				for (int i = 0; i < lastTimestampMsgPerSeverityOrdinal.length; i++)
					lastTimestampMsgPerSeverityOrdinal[i] = new AtomicLong(0);
			}
			{
				lastLoggedTextMsgPerSeverityOrdinal = TypeUtil.coerce(new AtomicReference[MAX_SEVERITY_ORDINAL + 1]); // must be + 1 for last index to work!
				for (int i = 0; i < lastLoggedTextMsgPerSeverityOrdinal.length; i++)
					lastLoggedTextMsgPerSeverityOrdinal[i] = new AtomicReference<>(null);
			}
			{
				fullCycleCountThresholdCounters = TypeUtil.coerce(new AtomicLong[6]);
				for (int i = 0; i < fullCycleCountThresholdCounters.length; i++)
					fullCycleCountThresholdCounters[i] = new AtomicLong(0);
			}
			{
				timeSinceLastAccessThresholdCounters = TypeUtil.coerce(new AtomicLong[6]);
				for (int i = 0; i < timeSinceLastAccessThresholdCounters.length; i++)
					timeSinceLastAccessThresholdCounters[i] = new AtomicLong(0);
			}
		}
		
	}
	
	/**
	 * Possible cache control statuses (not_started, running, shutdown...)
	 */
	public static enum WBRBCacheControlState
	{
		/**
		 * Cache hasn't been started yet.
		 */
		NOT_STARTED,
		/**
		 * Cache is running (can handle access operations).
		 */
		RUNNING,
		/**
		 * Cache is flushing -- this is mainly useful for tests, tries to flush
		 * all data from cache as soon as possible (similar to shutdown) but does
		 * not shut down cache at the end.
		 * <p>
		 * Standard cache operations (read/write) are NOT allowed during flushing
		 * as it will interfere with flushing itself.
		 */
		FLUSHING,
		/**
		 * Cache shutdown is in progress.
		 */
		SHUTDOWN_IN_PROGRESS,
		/**
		 * Cache has been fully shutdown.
		 */
		SHUTDOWN_COMPLETED,
		;
	}
	
	/**
	 * Possible statuses for cache entry read state.
	 *
	 * @author Sergey Olefir
	 */
	@NonNullByDefault
	protected static enum WBRBCacheEntryReadStatus
	{
		NOT_READ_YET,
		DATA_READY_RESYNC_PENDING,
		DATA_READY,
		/**
		 * Indicates that reading data has failed and no further attempts will
		 * be made.
		 */
		READ_FAILED_FINAL,
		/**
		 * Indicates that reading data for resync has failed and no futher attemps
		 * will be made.
		 */
		DATA_READY_RESYNC_FAILED_FINAL,
		/**
		 * Element was removed from cache already.
		 */
		REMOVED_FROM_CACHE,
		;
	}
	
	/**
	 * Possible statuses for cache entry write state.
	 *
	 * @author Sergey Olefir
	 */
	@NonNullByDefault
	protected static enum WBRBCacheEntryWriteStatus
	{
		NO_WRITE_REQUESTED_YET,
		WRITE_PENDING,
		WRITE_SUCCESS,
		WRITE_FAILED_FINAL,
		/**
		 * Element was removed from cache already.
		 * FIX-ME use it to prevent failed write retries?
		 */
		REMOVED_FROM_CACHE,
		;
	}
	
	/**
	 * Internal class used as entry values in this cache.
	 * <p>
	 * NOTE: this is always accessed through the volatile variable to maintain
	 * access consistency.
	 */
	@ToString(exclude = "parentCacheEntry") // Exclude parent to avoid infinite loop
	protected class WBRBCachePayload
	{
		/**
		 * Parent cache entry.
		 * TO-DO not sure it's a good idea to have it in here, but useful for setStatus unlatching
		 */
		private final WBRBCacheEntry parentCacheEntry;
		
		/**
		 * Track when this entry was last successfully synchronized with storage 
		 * (via initial read or resync read).
		 */
		@Getter
		@Setter
		private long lastSyncedWithStorageTimestamp;
		
		/**
		 * Tracks timestamp of the last read of the data.
		 * <p>
		 * Zero value is not really used; positive values indicate last read
		 * timestamp; negative values are an inversion of timestamp done
		 * e.g. at the end of main queue in order to track whether any new
		 * reads have been made since the inversion. 
		 * <p>
		 * volatile because it needs to be modifiable from read locks
		 */
		@Getter
		@Setter
		private volatile long lastReadTimestamp;
		
		/**
		 * Tracks timestamp of the last write to the data.
		 * <p>
		 * Zero means no writes; positive values indicate last write timestamp
		 * and that the data 'is dirty'; negative values are an inversion of
		 * last write timestamp -- set e.g. at the end of main queue processing --
		 * used to indicate that the data is no longer 'dirty' but to still keep
		 * track of when the last write was done.
		 */
		@Getter
		@Setter
		private long lastWriteTimestamp = 0;
		
		/**
		 * Entry read status.
		 */
		@Getter
		@NonNull // required field for Lombok
		private WBRBCacheEntryReadStatus readStatus;
		
		/**
		 * Entry write status.
		 */
		@Getter
		@NonNull // required field for Lombok
		private WBRBCacheEntryWriteStatus writeStatus;
		
		/**
		 * Actual cached value (may be null if cache supports null values).
		 */
		@Getter
		@Setter
		private S value = fakeNonNull();
		
		/**
		 * In current/previous queue since this time.
		 */
		@Getter
		@Setter
		// FIX-ME make sure it is updated properly
		private long inQueueSince;
		
		/**
		 * Tracks number of consecutive failures since last successful read
		 * operation.
		 */
		@Getter
		private final SimpleIntCounter readFailureCount = new SimpleIntCounter(0);
		
		/**
		 * Tracks number of consecutive writes since last successful read
		 * operation.
		 */
		@Getter
		private final SimpleIntCounter writeFailureCount = new SimpleIntCounter(0);
		
		/**
		 * Keeps track of however many times full cache cycle completed for the
		 * item without a full success (e.g. either read or write has failed);
		 * useful to evict elements that keep failing
		 * TO-DO check this is reset properly 
		 */
		@Getter
		private final SimpleIntCounter fullCacheCycleFailureCount = new SimpleIntCounter(0);
		
		/**
		 * Keeps track of however many times item was re-added to the return
		 * queue to determine when we should give up.
		 * TO-DO check this is reset properly 
		 */
		@Getter
		private final SimpleIntCounter returnQueueRetryCount = new SimpleIntCounter(0);
		
		/**
		 * Keeps track of however many times full cache cycle completed for the
		 * item while it is still in the cache. This is incremented at the end
		 * of return queue (unlike failure count which is handled in main queue
		 * processing).
		 * <p>
		 * Mainly for monitoring cache performance.
		 */
		@Getter
		private final SimpleIntCounter fullCacheCycleCountByReturnQueue = new SimpleIntCounter(0);
		
		/**
		 * Indicates whether updates to this cache entry should be collected;
		 * updates are typically collected after cache write out until cache
		 * value is re-read from storage -- they are used to merge data between
		 * cached value and read-from-storage value.
		 */
		@Getter
		@Setter
		// FIX-ME this shouldn't be used as flag to determine course of operations, because e.g. client code may be doing merge w/o collecting updates
		//       probably best to implement overridable method 'is merge possible' or some such
		private boolean collectUpdates = false;
		
		/**
		 * List of updates executed on this cached value since last write out
		 * (this is useful to be able to merge data between cached value and 
		 * storage-read value).
		 */
		@Nullable
		@Getter
		@Setter
		private List<UInt> collectedUpdates = null;
		
		/**
		 * Is set when write fails with {@link WBRBCacheEntryWriteStatus#WRITE_FAILED_FINAL} status.
		 * <p>
		 * Used in {@link WriteBehindResyncInBackgroundCache#spiWriteLockProcessSplitForWrite(Object, Object, WBRBCacheEntry, WBRBCachePayload)}
		 * <p>
		 * ONLY used if write status is {@link WBRBCacheEntryWriteStatus#WRITE_FAILED_FINAL}
		 */
		@Getter
		@Setter
		private NullableOptional<W> previousFailedWriteData = NullableOptional.empty();
		
		/**
		 * Field that can be used by custom extending code to store whatever it needs extra.
		 * <p>
		 * Field is marked as volatile to provide some cross-threads guarantees.
		 * TO-DO keep? redo?
		 * TO-DO needs copying/reset when cycling through cache?
		 */
		@Getter
		@Setter
		@Nullable
		private volatile Object customDataField = null;

		/**
		 * Constructor.
		 */
		public WBRBCachePayload(
			WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, W, UExt, UInt>.WBRBCacheEntry parentCacheEntry,
			@NonNull WBRBCacheEntryReadStatus readStatus,
			@NonNull WBRBCacheEntryWriteStatus writeStatus,
			long timeNow)
		{
			super();
			this.parentCacheEntry = parentCacheEntry;
			this.readStatus = readStatus;
			this.writeStatus = writeStatus;
			this.lastReadTimestamp = timeNow;
			this.inQueueSince = timeNow;
		}
		
		/**
		 * Custom setter makes sure to unlock the parent cache entry latch
		 * if changing from the NOT_READ_YET status.
		 * <p>
		 * Therefore this is best called after everything else is ready.
		 */
		public void setReadStatus(WBRBCacheEntryReadStatus newReadStatus)
		{
			WBRBCacheEntryReadStatus oldStatus = readStatus;
			
			switch (oldStatus)
			{
				case REMOVED_FROM_CACHE:
					if (newReadStatus != oldStatus)
					{
						Exception e = new Exception("Attempt for element [" + parentCacheEntry.getKey() + "] to change read status from REMOVED_FROM_CACHE to " + newReadStatus);
						logMessage(WBRBCacheMessage.ASSERTION_FAILED, e, e.toString());
					}
					return; // 'removed' status is final and should never be changed
				case NOT_READ_YET:
				case DATA_READY:
				case DATA_READY_RESYNC_PENDING:
				case DATA_READY_RESYNC_FAILED_FINAL:
				case READ_FAILED_FINAL:
					break;
			}
			
			readStatus = newReadStatus;
			
			if (oldStatus != newReadStatus)
			{
				switch (oldStatus)
				{
					case NOT_READ_YET:
						parentCacheEntry.getAccessLatch().open(); // unlock access latch if initial read result is now known
						break;
					case DATA_READY:
					case DATA_READY_RESYNC_PENDING:
					case DATA_READY_RESYNC_FAILED_FINAL:
					case READ_FAILED_FINAL:
						break;
					case REMOVED_FROM_CACHE:
						logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), "code should not be reachable");
						break;
				}
			}
		}
		
		/**
		 * Custom setter -- ensures that {@link WBRBCacheEntryWriteStatus#REMOVED_FROM_CACHE}
		 * is final (never changed to something else).
		 */
		public void setWriteStatus(WBRBCacheEntryWriteStatus newWriteStatus)
		{
			WBRBCacheEntryWriteStatus oldStatus = writeStatus;
			
			switch (oldStatus)
			{
				case REMOVED_FROM_CACHE:
					if (newWriteStatus != oldStatus)
					{
						Exception e = new Exception("Attempt for element [" + parentCacheEntry.getKey() + "] to change write status from REMOVED_FROM_CACHE to " + newWriteStatus);
						logMessage(WBRBCacheMessage.ASSERTION_FAILED, e, e.toString());
					}
					return; // 'removed' status is final and should never be changed
				case NO_WRITE_REQUESTED_YET:
				case WRITE_PENDING:
				case WRITE_SUCCESS:
				case WRITE_FAILED_FINAL:
					break;
			}
			
			writeStatus = newWriteStatus;
		}
	}
	
	/**
	 * Internal class used as entries in this cache.
	 */
	@ToString
	protected class WBRBCacheEntry
	{
		/**
		 * Key for this entry.
		 * TO-DO check if this is actually necessary to keep around
		 */
		@Getter
		private final K key;
		
		/**
		 * Lock used to protect writes to the entry.
		 * TO-DO consider whether fairness should be configurable?
		 */
		@Getter
		private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
		
		/**
		 * Latch is used to block access to data until the data is actually available
		 * (since reads are asynchronous).
		 */
		@Getter
		private final Latch accessLatch = new Latch(false);
		
		/**
		 * Entry payload, contains value and various service fields.
		 * <p>
		 * This field is volatile to provide some guarantee of access 
		 * consistency between threads.
		 */
		@Getter
		@Setter
		private volatile WBRBCachePayload payload;
	
		/**
		 * @param key
		 */
		public WBRBCacheEntry(@Nonnull K key, long timeNow)
		{
			super();
			this.key = key;
			this.payload = new WBRBCachePayload(this, 
				WBRBCacheEntryReadStatus.NOT_READ_YET, WBRBCacheEntryWriteStatus.NO_WRITE_REQUESTED_YET, timeNow);
		}
	}
	
	/**
	 * Entry in the write queue.
	 * <p>
	 * FINAL everything to make in thread-safe.
	 */
	@RequiredArgsConstructor
	@ToString
	protected class WBRBWriteQueueEntry
	{
		/**
		 * Key for this entry.
		 * TO-DO check if this is actually necessary to keep around
		 */
		@Getter
		private final K key;
		
		/**
		 * Related cache entry.
		 * <p>
		 * WARNING -- be careful with access, should always be done under read or
		 * write locks!
		 */
		@Getter
		private final WBRBCacheEntry cacheEntry;
		
		/**
		 * Data to be written out.
		 */
		@Getter
		private final W dataToWrite;
	}
	
	/**
	 * Indicates a 'merge decision' -- what to do with data that was just read
	 * from the underlying storage and ought to be set or merged into an exising
	 * cache entry.
	 */
	protected static enum WBRBMergeDecision
	{
		/**
		 * Set read data directly, no merging required.
		 */
		SET_DIRECTLY,
		/**
		 * Merge data (that is try to merge read data with already existing data).
		 */
		MERGE_DATA,
		/**
		 * Do nothing (ignore the data that has been read (typically ought to also
		 * log a problem)) except clear read pending status (if currently read is
		 * pending), e.g.: 
		 * {@link WBRBCacheEntryReadStatus#DATA_READY_RESYNC_PENDING}
		 * ->
		 * {@link WBRBCacheEntryReadStatus#DATA_READY_RESYNC_FAILED_FINAL};
		 * {@link WBRBCacheEntryReadStatus#NOT_READ_YET} ->
		 * {@link WBRBCacheEntryReadStatus#READ_FAILED_FINAL}
		 * and similar
		 */
		CLEAR_READ_PENDING_STATUS,
		/**
		 * Do nothing, ignore the data that has been read (typically ought to also
		 * log a problem).
		 * <p>
		 * WARNING: may cause issues if used improperly, such as not releasing
		 * access latch.
		 */
		DO_NOTHING,
		/**
		 * Element must be removed from cache.
		 */
		REMOVE_FROM_CACHE,
		;
	}
	
	/**
	 * Indicates a 'retry decision' -- what to do with failed read.
	 */
	protected static enum WBRBRetryDecision
	{
		/**
		 * Retry the failed operation.
		 */
		RETRY,
		/**
		 * No retry, set final failed status for the operation.
		 */
		NO_RETRY_SET_FINAL_FAILED_STATUS,
		/**
		 * Do nothing, ignore the result (typically ought to also log a problem).
		 * <p>
		 * WARNING: may cause issues if used improperly, such as not releasing
		 * access latch.
		 */
		DO_NOTHING,
		/**
		 * Element must be removed from cache.
		 */
		REMOVE_FROM_CACHE,
		;
	}
	
	/**
	 * Indicates a decision for read queue item processing.
	 */
	protected static enum WBRBReadQueueProcessingDecision
	{
		/**
		 * Execute read flagged as 'non-refresh' one.
		 */
		INITIAL_READ,
		/**
		 * Execute read marked as 'refresh read'.
		 */
		REFRESH_READ,
		/**
		 * Does nothing (including not changing any status) -- which might be
		 * dangerous as it wouldn't, e.g., release access latch.
		 */
		DO_NOTHING,
		/**
		 * Indicates that processing code should mark entry as 'read failed (final)'
		 * and not queue it for read.
		 */
		SET_FINAL_FAILED_READ_STATUS,
	}
	
	/**
	 * Indicates a decision for write queue item processing.
	 */
	protected static enum WBRBWriteQueueProcessingDecision
	{
		/**
		 * Do the write.
		 */
		WRITE,
		/**
		 * Does nothing (including not changing any status) -- which might be
		 * dangerous as it wouldn't do the 'expected' stuff.
		 */
		DO_NOTHING,
		/**
		 * Indicates that processing code should mark referenced cache entry 
		 * as 'write failed (final)' and not queue it for the write.
		 */
		SET_FINAL_FAILED_WRITE_STATUS,
	}

	
	
	/**
	 * Indicates a decision for whether main queue item being processed
	 * shall be retained in cache.
	 * FIX-ME probably needs a DO_NOTHING option for e.g. already-removed elements
	 */
	@RequiredArgsConstructor
	protected static enum WBRBMainQueueItemCacheRetainDecision
	{
		/**
		 * This is a 'normal' expected behavior -- item is sent to the return
		 * queue after leaving main queue. 
		 */
		RETURN_QUEUE(10),
		/**
		 * Do the 'normal' processing as in {@link #RETURN_QUEUE} but keep
		 * full cycle processing failure count (for failures where we want
		 * to run standard processing but possibly abort at some point in the
		 * future if it keeps failing).
		 */
		RETURN_QUEUE_KEEP_FULL_CYCLE_FAILURE_COUNT(20),
		/**
		 * Put item in the return queue (as per normal), but DO NOT send anything
		 * to write (this can be used to e.g. force new attempt at data resync).
		 * <p>
		 * This also 'keeps' (doesn't reset, increments) full cache cycle failure count
		 */
		RETURN_QUEUE_NO_WRITE(30),
		/**
		 * The item is re-added to the main queue -- this can be useful e.g. if
		 * previous pending write hasn't completed yet and we do not want to risk
		 * queueing a new write yet.
		 * <p>
		 * WRITE IS NOT PERFORMED.
		 */
		MAIN_QUEUE(40),
		/**
		 * Item is removed from cache, useful e.g. if item is not in an useful
		 * state, such as initial read has failed.
		 * <p>
		 * This is 'normal' item expiration, it is not logged as error.
		 * <p>
		 * WRITE IS NOT PERFORMED.
		 */
		EXPIRE_FROM_CACHE(50),
		/**
		 * Item is removed from cache, useful e.g. if item is not in an useful
		 * state, such as initial read has failed.
		 * <p>
		 * This is 'abnormal' item removal, it is logged as error.
		 * <p>
		 * WRITE IS NOT PERFORMED.
		 */
		REMOVE_FROM_CACHE(60),
		;
		
		/**
		 * How high is 'failure rating' for this item; the higher, the 'worse'
		 * is the failure.
		 * <p>
		 * Used to determine between multiple values which one is the 'worst'.
		 */
		@Getter
		private final int failureRating;
	}
	
	
	/**
	 * Decision on how to proceed when initial read has failed (final status).
	 */
	protected static enum WBRBInitialReadFailedFinalDecision
	{
		/**
		 * Element is removed from cache (this results in the loss of all
		 * currently in-memory updates).
		 */
		REMOVE_FROM_CACHE,
		/**
		 * Element is kept in the cache and any read (and maybe write) operations
		 * will throw exception.
		 */
		KEEP_AND_THROW_CACHE_READ_EXCEPTIONS,
	}
	
	
	/**
	 * Decision on how to proceed when background resync has failed (final status).
	 */
	protected static enum WBRBResyncFailedFinalDecision
	{
		/**
		 * Element is removed from cache (this results in the loss of all
		 * currently in-memory updates).
		 */
		REMOVE_FROM_CACHE,
		/**
		 * Element is retained in cache, but updates are no longed collected
		 * (this means that element cannot be resynced at a later date).
		 */
		STOP_COLLECTING_UPDATES,
		/**
		 * Element is retained in cache and updates are still collected -- this
		 * allows for an option to resync at a later date but at the cost of
		 * memory usage for collecting all the updates.
		 */
		KEEP_COLLECTING_UPDATES,
	}

	
	/**
	 * Indicates a decision for how the item in return queue should be processed.
	 */
	@RequiredArgsConstructor
	protected static enum WBRBReturnQueueItemProcessingDecisionAction
	{
		/**
		 * This is a 'normal' expected behavior -- item is sent to the main queue
		 * and resync in initiated
		 */
		MAIN_QUEUE_PLUS_RESYNC(10),
		/**
		 * Put item back into main queue but do not initiate a resync.
		 */
		MAIN_QUEUE_NO_RESYNC(20),
		/**
		 * Expire element from the cache (item is no longer needed).
		 * <p>
		 * This is 'normal' item expiration, it is not logged as error.
		 * 
		 * @see #REMOVE_FROM_CACHE
		 */
		EXPIRE_FROM_CACHE(30),
		/**
		 * Item goes back to the return queue (basically a 'wait some more' 
		 * action).
		 */
		RETURN_QUEUE(40),
		/**
		 * Item is removed from cache, useful e.g. if item is not in an useful
		 * state, such as initial read has failed.
		 * <p>
		 * This is 'abnormal' item removal, it is logged as error.
		 * 
		 * @see #EXPIRE_FROM_CACHE
		 */
		REMOVE_FROM_CACHE(50),
		/**
		 * Do nothing -- e.g. if element was already removed from cache, then
		 * it doesn't need to be re-added anywhere.
		 */
		DO_NOTHING(60),
		;
		
		/**
		 * How high is 'failure rating' for this item; the higher, the 'worse'
		 * is the failure.
		 * <p>
		 * Used to determine between multiple values which one is the 'worst'.
		 */
		@Getter
		//TO-DO probably is not needed
		private final int failureRating;
	}
	// CCC
	@RequiredArgsConstructor
	@ToString
	protected static class WBRBReturnQueueItemProcessingDecision
	{
		/**
		 * Constant for when we want to remove item from cache (so no need to
		 * create new ones all the time).
		 */
		public static final WBRBReturnQueueItemProcessingDecision REMOVE_FROM_CACHE = 
			new WBRBReturnQueueItemProcessingDecision(WBRBReturnQueueItemProcessingDecisionAction.REMOVE_FROM_CACHE, true);
		
		/**
		 * A 'do nothing' option that can be used instead of creating new instances.
		 */
		public static final WBRBReturnQueueItemProcessingDecision DO_NOTHING = 
			new WBRBReturnQueueItemProcessingDecision(WBRBReturnQueueItemProcessingDecisionAction.DO_NOTHING, false);
		
		/**
		 * An 'expire from cache' option that can be used instead of creating new instances.
		 */
		public static final WBRBReturnQueueItemProcessingDecision EXPIRE_FROM_CACHE = 
			new WBRBReturnQueueItemProcessingDecision(WBRBReturnQueueItemProcessingDecisionAction.EXPIRE_FROM_CACHE, true);
		
		// CCC
		@Getter
		@NonNull // force Lombok to do null-check
		private final WBRBReturnQueueItemProcessingDecisionAction action;
		
		// CCC
		@Getter
		private final boolean stopCollectingUpdates;
	}
	
	/**
	 * Action when deciding whether to access item from cache.
	 */
	protected static enum WBRBCacheAccessDecisionAction
	{
		/**
		 * Value successfully read & returned
		 */
		VALUE_RETURNED,
		/**
		 * Exception returned that needs to be thrown.
		 * 
		 * @see #RETURN_EXCEPTION
		 */
		THROW_EXCEPTION,
		/**
		 * Exception that indicates a problem, but is not necessarily thrown
		 * (e.g. can be returned as part of {@link NullableOptional})
		 * 
		 * @see #THROW_EXCEPTION
		 */
		RETURN_EXCEPTION,
		/**
		 * Value is not read from the storage yet, wait for latch.
		 */
		WAIT_FOR_LATCH,
		/**
		 * This value has been removed from cache, need to attempt a re-read.
		 */
		REMOVED_FROM_CACHE,
		;
	}
	
	/**
	 * Decision whether to access an item from cache.
	 * 
	 * @see WriteBehindResyncInBackgroundCache#WBRBCacheAccessDecision_WAIT_FOR_LATCH
	 * @see WriteBehindResyncInBackgroundCache#WBRBCacheAccessDecision_REMOVED_FROM_CACHE
	 * @see WriteBehindResyncInBackgroundCache#WBRBCacheAccessDecision_WRITE_ALLOWED
	 */
	@ToString(doNotUseGetters = true)
	protected class WBRBCacheAccessDecision
	{
		/**
		 * Action associated with this decision.
		 */
		@Getter
		private final WBRBCacheAccessDecisionAction action;
		
		/**
		 * Value read in this decision (only available for appropriate {@link #action})
		 */
		private final S readValue;
		
		/**
		 * Exception to be thrown in this decision (only available for appropriate
		 * {@link #action})
		 */
		private final CacheIllegalStateException exception;
		
		/**
		 * Constructor.
		 */
		private WBRBCacheAccessDecision(WBRBCacheAccessDecisionAction action, S readValue, 
			CacheIllegalStateException exception)
		{
			this.action = action;
			this.readValue = readValue;
			this.exception = exception;
		}
		
		/**
		 * Constructor for the case when we return value.
		 */
		public WBRBCacheAccessDecision(S readValue)
		{
			this(WBRBCacheAccessDecisionAction.VALUE_RETURNED, readValue, fakeNonNull());
		}
		
		/**
		 * Constructor for the case when we return exception to be thrown.
		 * 
		 * @param throwException true for {@link WBRBCacheAccessDecisionAction#THROW_EXCEPTION}
		 * 		action, false for {@link WBRBCacheAccessDecisionAction#RETURN_EXCEPTION}
		 */
		public WBRBCacheAccessDecision(boolean throwException, CacheIllegalStateException exception)
		{
			this(throwException ? 
					WBRBCacheAccessDecisionAction.THROW_EXCEPTION
				:	WBRBCacheAccessDecisionAction.RETURN_EXCEPTION
				, fakeNonNull(), exception);
		}
		
		public S getReadValue() throws CacheInternalException
		{
			WBRBCacheAccessDecisionAction act = getAction();
			switch (act)
			{
				case REMOVED_FROM_CACHE:
				case THROW_EXCEPTION:
				case RETURN_EXCEPTION:
				case WAIT_FOR_LATCH:
					logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), "code should not be reachable (method should not be called in this action)");
					throw new CacheInternalException("Illegal action for requesting readValue(): " + act);
				case VALUE_RETURNED:
					return readValue;
			}
			
			logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), "code should not be reachable");
			throw new CacheInternalException("assertion failed -- code should not be reachable");
		}
		
		public CacheIllegalStateException getException() throws CacheInternalException
		{
			WBRBCacheAccessDecisionAction act = getAction();
			switch (act)
			{
				case REMOVED_FROM_CACHE:
				case VALUE_RETURNED:
				case WAIT_FOR_LATCH:
					logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), "code should not be reachable (method should not be called in this action)");
					throw new CacheInternalException("Illegal action for requesting getException(): " + act);
				case RETURN_EXCEPTION:
				case THROW_EXCEPTION:
					return exception;
			}
			
			logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), "code should not be reachable");
			throw new CacheInternalException("assertion failed -- code should not be reachable");
		}

		// TO-DO remove cuz have annotation?
//		@Override
//		public String toString()
//		{
//			return getClass().getSimpleName() + "[" + action + ": " + readValue + " / exc: " + exception + "]";
//		}
	}
	/**
	 * Constant for indicating that read operation should wait for latch
	 */
	protected final WBRBCacheAccessDecision WBRBCacheAccessDecision_WAIT_FOR_LATCH = new WBRBCacheAccessDecision(
		WBRBCacheAccessDecisionAction.WAIT_FOR_LATCH, fakeNonNull(), fakeNonNull());
	/**
	 * Constant for indicating that this entry was removed from cache and read 
	 * operation should retry.
	 */
	protected final WBRBCacheAccessDecision WBRBCacheAccessDecision_REMOVED_FROM_CACHE = new WBRBCacheAccessDecision(
		WBRBCacheAccessDecisionAction.REMOVED_FROM_CACHE, fakeNonNull(), fakeNonNull());
	/**
	 * Constant for indicating decision that allows write (write decision doesn't
	 * require returning any actual value).
	 */
	@SuppressWarnings("unchecked")
	protected final WBRBCacheAccessDecision WBRBCacheAccessDecision_WRITE_ALLOWED =
		new WBRBCacheAccessDecision((S)new Object());
	
	
	/**
	 * Split data for when cached value needs to be written out, e.g. in
	 * {@link #splitForWrite(Object, Object)}
	 */
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	@ToString
	protected class WriteSplit
	{
		/**
		 * New cache data to be stored instead of the old one (may be the same
		 * instance as the old one).
		 */
		@Getter
		@NonNull // for Lombok Non-Null checking
		private final S newCacheData;
		
		/**
		 * Data to send for writing.
		 */
		@Getter
		@NonNull // for Lombok Non-Null checking
		private final NullableOptional<W> writeData;
		
		/**
		 * Constructor for the case when there's no write data (i.e. write
		 * is not required).
		 */
		public WriteSplit(S newCacheData)
		{
			this(newCacheData, NullableOptional.empty());
		}
		
		/**
		 * Constructor for when there's both new cache data and write data (i.e.
		 * write will be executed)
		 */
		public WriteSplit(S newCacheData, W dataToWrite)
		{
			this(newCacheData, NullableOptional.of(dataToWrite));
		}
	}
	
	/**
	 * A {@link WriteSplit} instance with additional flags.
	 */
	@RequiredArgsConstructor
	@ToString
	protected class WBRBWriteSplitWithFlags
	{
		/**
		 * Actual {@link WriteSplit}
		 */
		@Getter
		@NonNull // for Lombok Non-Null checking
		private final WriteSplit writeSplit;
		
		/**
		 * Whether write data (in {@link #writeSplit}) contains all pending
		 * updates (in case write merge is not supported, it is possible that
		 * we have some write data that needs to be stored plus further updates
		 * that aren't included in that write data -- in this case this flag
		 * is false). 
		 */
		@Getter
		private final boolean writeDataContainsAllPendingUpdates;
	}
	
	/**
	 * Decision when processing {@link #mainQueue}
	 */
	@RequiredArgsConstructor
	@ToString
	protected class WBRBMainQueueProcessingDecision
	{
		@Getter
		@NonNull // for Lombok Non-Null checking
		private final WBRBMainQueueItemCacheRetainDecision decision;

		@Getter
		@NonNull // for Lombok Non-Null checking
		private final WBRBWriteSplitWithFlags writeSplitWithFlags;
	}
	
	
	/**
	 * Possible event types in {@link WriteBehindResyncInBackgroundCache#spiUnknownLock_Event(WBRBEvent, Object, WBRBCacheEntry, WBRBCachePayload, Throwable, Object...)}
	 */
	protected static enum WBRBEvent
	{
		/**
		 * Internal debug event -- used when necessary to debug something internally
		 * <p> 
		 * Arguments: whatever is appropriate, typically description should be 
		 * included.
		 */
		DEBUG_EVENT_INTERNAL,
		
		/**
		 * Item from the read queue processed
		 * <p>
		 * Arguments: {@link WBRBReturnQueueItemProcessingDecision}
		 */
		READ_QUEUE_ITEM_PROCESSED,
		/**
		 * Item from the write queue processed
		 * <p>
		 * Arguments: no cache entry, no payload, {@link WBRBWriteQueueProcessingDecision}, {@link WBRBWriteQueueEntry}
		 */
		WRITE_QUEUE_ITEM_PROCESSED,
		/**
		 * Item from the main queue was processed
		 * <p>
		 * Arguments: {@link WBRBMainQueueProcessingDecision}
		 */
		MAIN_QUEUE_ITEM_PROCESSED,
		/**
		 * Item from the return queue was processed
		 * <p>
		 * Arguments: {@link WBRBReturnQueueItemProcessingDecision}
		 */
		RETURN_QUEUE_ITEM_PROCESSED,
		/**
		 * Item was read successfully and merge decision was taken
		 * <p>
		 * Arguments: {@link WBRBMergeDecision}
		 */
		READ_SUCCESS_DECISION,
		/**
		 * Item failed read, the retry decision and potentially final-fail 
		 * decision were taken
		 * <p>
		 * Arguments: {@link WBRBRetryDecision}, {@link NullableOptional} of 
		 * {@link WBRBInitialReadFailedFinalDecision} or {@link WBRBResyncFailedFinalDecision} 
		 */
		READ_FAIL_DECISION,
		/**
		 * Item was written successfully
		 * <p>
		 * Arguments: {@link WBRBWriteQueueEntry}
		 */
		WRITE_SUCCESS,
		/**
		 * Item failed to write, the retry decision was taken
		 * <p>
		 * Arguments: {@link WBRBRetryDecision}, {@link WBRBWriteQueueEntry}
		 */
		WRITE_FAIL_DECISION,
		/**
		 * Item was added to cache
		 * <p>
		 * Arguments: no payload
		 */
		CACHE_ADD,
		/**
		 * Item was removed from cache
		 * <p>
		 * Arguments: no payload
		 */
		CACHE_REMOVE,
		/**
		 * Successful write event for the cache item (although still can fail
		 * later on while collecting update)
		 * <p>
		 * Arguments: update value (ATTN: can be null if cache accepts those; not
		 * wrapped into {@link NullableOptional} for performance reasons)
		 */
		CACHE_WRITE,
		/**
		 * Successful read event for the cache item
		 * <p>
		 * Arguments: {@link NullableOptional} of {@link R} (value returned from cache)
		 */
		CACHE_READ,
		;
	}

	/**
	 * Indicates reason as to why {@link WriteBehindResyncInBackgroundCache#spiWriteLockUpdates_reset(boolean, Object, io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * is called (lets implementations to make their own decisions as to whether
	 * to collect updates).
	 *
	 * @author Sergey Olefir
	 */
	protected enum WBRBUpdatesResetReason
	{
		/**
		 * New entry has been created for the cache.
		 * <p>
		 * NOTE: in this case NO LOCK is held, however it is executed in a
		 * thread-safe manner (e.g. before object reference is shared) so
		 * the typical implementation should not care about the difference.
		 * <p>
		 * NOTE2: it is possible under contention that newly created entry will
		 * be immediatelly discarded without adding to the cache; implementations
		 * might want to keep this in mind.
		 */
		NO_WRITE_LOCK_NEW_CACHE_ENTRY_CREATED,
		/**
		 * Entry has been removed from cache, this is memory cleanup.
		 */
		REMOVED_FROM_CACHE,
		/**
		 * Data from storage was just merged successfully (i.e. initial read or
		 * refresh read were processed). 
		 */
		STORAGE_DATA_MERGED,
		/**
		 * Read failed (final, no more retries) -- either initial or refresh -- and
		 * handling code decision (e.g. in {@link WriteBehindResyncInBackgroundCache#spiWriteLockMakeResyncFailedFinalDecision(Object, WBRBCacheEntry, WBRBCachePayload)} ) 
		 * is that reset is needed. 
		 */
		READ_FAILED_FINAL_DECISION,
		/**
		 * Exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockUpdates_isMergePossible(Object, WBRBCacheEntry, WBRBCachePayload, NullableOptional)}
		 */
		IS_MERGE_POSSIBLE_EXCEPTION,
		/**
		 * In main queue processing all pending data was sent to write queue.
		 */
		FULL_WRITE_SENT,
		/**
		 * Decision in return queue processing in {@link WriteBehindResyncInBackgroundCache#spiWriteLockMakeReturnQueueProcessingDecision(Object, WBRBCacheEntry, WBRBCachePayload)}
		 */
		RETURN_QUEUE_DECISION,
		/**
		 * Exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockUpdates_collect(Object, WBRBCacheEntry, WBRBCachePayload, Object)}
		 * (such as too many updates already collected).
		 */
		UPDATE_COLLECT_EXCEPTION,
		;
	}
	
	
	/**
	 * Constructor.
	 * 
	 * @throws RuntimeException if something is not right with config
	 */
	public WriteBehindResyncInBackgroundCache(WBRBConfig config) 
		throws IllegalArgumentException, IllegalStateException, MissingResourceException, NumberFormatException
	{
		this.config = config;
		this.commonNamingPrefix = "WBRBCache[" + config.getCacheName() + "]";
		
		// Initialize logging stuff
		{
			int maxOrdinal = 1;
			for (WBRBCacheMessage entry : WBRBCacheMessage.values())
			{
				maxOrdinal = Math.max(maxOrdinal, entry.ordinal());
			}
			
			messageTypeCountersArray = TypeUtil.coerce(new AtomicReference[maxOrdinal + 1]); // must be + 1 for last index to work!
			for (int i = 0; i < messageTypeCountersArray.length; i++)
				messageTypeCountersArray[i] = new AtomicReference<>(new WBRBMessageTypeLoggingCounter(0));
		}
		
		this.threadGroup = new ThreadGroup(commonNamingPrefix + " Thread Group");
		
		this.readQueueProcessingThread = createReadQueueProcessor();
		
		// readThreadPool
		{
			final WAThreadPoolExecutor pool;
			
			Pair<@Nonnull Integer, @Nonnull Integer> poolSize = config.getReadThreadPoolSize();
			int minSize = poolSize.getValue0();
			int maxSize = poolSize.getValue1();
			
			if ((minSize == -1) && (maxSize == -1))
			{
				pool = null;
			}
			else if ((minSize == -1) || (maxSize == -1))
			{
				throw new IllegalStateException("min/max pool size for readThreadPoolSize can only be -1 if they both are, got: " + poolSize);
			}
			else
			{
				if (maxSize < minSize)
					throw new IllegalStateException("max pool size is less than min for readThreadPoolSize: " + poolSize);
				
				pool = new WAThreadPoolExecutor(maxSize, commonNamingPrefix + "-readPool", true, config.getReadThreadPoolPriority(), threadGroup);
				pool.setCorePoolSize(minSize);
				pool.setKeepAliveTime(config.getReadThreadPoolMaxIdleTime(), TimeUnit.MILLISECONDS);
			}
			
			readThreadPool = pool;
		}
		
		
		this.writeQueueProcessingThread = createWriteQueueProcessor();
		
		// writeThreadPool
		{
			final WAThreadPoolExecutor pool;
			
			Pair<@Nonnull Integer, @Nonnull Integer> poolSize = config.getWriteThreadPoolSize();
			int minSize = poolSize.getValue0();
			int maxSize = poolSize.getValue1();
			
			if ((minSize == -1) && (maxSize == -1))
			{
				pool = null;
			}
			else if ((minSize == -1) || (maxSize == -1))
			{
				throw new IllegalStateException("min/max pool size for writeThreadPoolSize can only be -1 if they both are, got: " + poolSize);
			}
			else
			{
				if (maxSize < minSize)
					throw new IllegalStateException("max pool size is less than min for writeThreadPoolSize: " + poolSize);
				
				pool = new WAThreadPoolExecutor(maxSize, commonNamingPrefix + "-writePool", true, config.getWriteThreadPoolPriority(), threadGroup);
				pool.setCorePoolSize(minSize);
				pool.setKeepAliveTime(config.getWriteThreadPoolMaxIdleTime(), TimeUnit.MILLISECONDS);
			}
			
			writeThreadPool = pool;
		}
		
		this.mainQueueProcessingThread = createMainQueueProcessor();
		this.returnQueueProcessingThread = createReturnQueueProcessor();
	}
	
	// FIX-ME review
	// FIX-ME update class/constructor javadocs to indicate that it needs to be started
	@Override
	public <C extends IWriteBehindResyncInBackgroundCacheWithControlMethods<K, V, S, R, W, UExt, UInt>> C start()
		throws CacheControlStateException
	{
		switch (controlState.get())
		{
			case NOT_STARTED:
				break;
			case FLUSHING:
			case RUNNING:
				throw new CacheControlStateException(commonNamingPrefix, "cache cannot be started because it has been started already.");
			case SHUTDOWN_IN_PROGRESS:
			case SHUTDOWN_COMPLETED:
				throw new CacheControlStateException(commonNamingPrefix, "cache cannot be started because it has been shutdown already.");
		}
		
		// Start all the threads
		this.readQueueProcessingThread.start();
		this.writeQueueProcessingThread.start();
		this.mainQueueProcessingThread.start();
		this.returnQueueProcessingThread.start();
		
		if (!controlState.compareAndSet(WBRBCacheControlState.NOT_STARTED, WBRBCacheControlState.RUNNING))
			throw new CacheControlStateException(commonNamingPrefix, "cache cannot be set to running state because its control state was changed concurrently (e.g. via shutdown).");
		
		logMessage(WBRBCacheMessage.STARTED, null);
		
		return TypeUtil.coerce(this);
	}
	
	
	/**
	 * CRITICAL marker -- used in preference to FATAL because FATAL originally
	 * meant application shutdown (which is not applicable in the case of library).
	 */
	protected static final Marker criticalMarker = MarkerFactory.getMarker("CRITICAL");
	
	/**
	 * Returns logger used by this instance for this particular event logging
	 * <p>
	 * Default implementation just returns {@link #defaultWBRBlog} which is
	 * based of our own class name. 
	 */
	@SuppressWarnings("unused")
	protected Logger spiUnknownLockGetLogger(WBRBCacheMessage msg, @Nullable Throwable exception, Object... args)
		throws InterruptedException
	{
		return defaultWBRBlog;
	}

	/**
	 * Extendable implementation of logging of events.
	 * <p>
	 * Default implementation simply sends them to throttled logging in 
	 * {@link #spiUnknownLockLogMessage_Throttled(WBRBCacheMessage, Throwable, Object...)}
	 */
	protected void spiUnknownLockLogMessage(WBRBCacheMessage msg, @Nullable Throwable exception, Object... args)
		throws InterruptedException
	{
		spiUnknownLockLogMessage_Throttled(msg, exception, args);
	}
	
	/**
	 * Implementation of event logging that just logs the event to the logger
	 * without any additional processing or checking.
	 * <p>
	 * This method uses logger retrieved from {@link #spiUnknownLockGetLogger(WBRBCacheMessage, Throwable, Object...)}
	 */
	protected void spiUnknownLockLogMessage_Plain(WBRBCacheMessage msg, @Nullable Throwable exception, Object... args)
		throws InterruptedException
	{
		Logger log = spiUnknownLockGetLogger(msg, exception, args);
		
		spiUnknownLockLogMessage_Plain(log, msg, exception, args);
	}
	
	/**
	 * Implementation of event logging that just logs the event to the given logger
	 * without any additional processing or checking.
	 */
	protected void spiUnknownLockLogMessage_Plain(Logger log, WBRBCacheMessage msg, @Nullable Throwable exception, Object... args)
		throws InterruptedException
	{
		final WBRBCacheMessageSeverity severity = msg.getSeverity(); 
		
		switch(msg.getSeverity())
		{
//			case TRACE:
//				if (!log.isTraceEnabled())
//					return;
//				break;
			case DEBUG:
				if (!log.isDebugEnabled())
					return;
				break;
			case INFO:
			case EXTERNAL_INFO:
				if (!log.isInfoEnabled())
					return;
				break;
			case WARN:
			case EXTERNAL_WARN:
				if (!log.isWarnEnabled())
					return;
				break;
			case ERROR:
			case EXTERNAL_DATA_LOSS:
			case EXTERNAL_ERROR:
			case FATAL:
				if (!log.isErrorEnabled())
					return;
				break;
		}
		
		String formattedMsg = spiUnknownLockLogMessage_FormatAndTrackMessage(log, msg, exception, args);
		
		switch(severity)
		{
//			case TRACE:
//				if (e != null)
//					log.trace(formattedMsg, e);
//				else
//					log.trace(formattedMsg);
//				break;
			case DEBUG:
				if (exception != null)
					log.debug(formattedMsg, exception);
				else
					log.debug(formattedMsg);
				break;
			case INFO:
			case EXTERNAL_INFO:
				if (exception != null)
					log.info(formattedMsg, exception);
				else
					log.info(formattedMsg);
				break;
			case WARN:
			case EXTERNAL_WARN:
				if (exception != null)
					log.warn(formattedMsg, exception);
				else
					log.warn(formattedMsg);
				break;
			case ERROR:
			case EXTERNAL_DATA_LOSS:
			case EXTERNAL_ERROR:
				if (exception != null)
					log.error(formattedMsg, exception);
				else
					log.error(formattedMsg);
				break;
			case FATAL:
				if (exception != null)
					log.error(criticalMarker, formattedMsg, exception);
				else
					log.error(criticalMarker, formattedMsg);
				break;
		}
	}
	
	/**
	 * Formats a given message for the output
	 */
	@SuppressWarnings("unused")
	protected String spiUnknownLockLogMessage_FormatMessage(Logger log, WBRBCacheMessage msg, @Nullable Throwable exception, Object... args)
		throws InterruptedException
	{
		StringBuilder sb = new StringBuilder(100);
		sb.append(commonNamingPrefix);
		sb.append(' ');
		sb.append(msg.toString());
		
		final String formattedMsg;
		if (args.length > 0)
		{
			sb.append(' ');
			sb.append(Arrays.toString(args));
		}
		
		return sb.toString();
	}
	
	/**
	 * Formats a given message for the output & tracks last logged message
	 */
	protected String spiUnknownLockLogMessage_FormatAndTrackMessage(Logger log, WBRBCacheMessage msg, @Nullable Throwable exception, Object... args)
		throws InterruptedException
	{
		String msgText = spiUnknownLockLogMessage_FormatMessage(log, msg, exception, args);
		
		getStats().lastLoggedTextMsgPerSeverityOrdinal[msg.getSeverity().ordinal()].set(msgText);
		
		return msgText;
	}
	
	
	/**
	 * Helper method to format event information (from {@link #spiUnknownLock_Event(WBRBEvent, Object, WBRBCacheEntry, WBRBCachePayload, Throwable, Object...)}})
	 * into data ready for sending to slf4j logging (pattern + arguments array).
	 */
	protected Pair<@Nonnull String, @Nullable Object @Nonnull[]> debugUnknownLock_FormatEventForSlf4jLogging(
		WBRBEvent event, @Nonnull K key,
		@Nullable WBRBCacheEntry cacheEntry, @Nullable WBRBCachePayload payload, 
		@Nullable Throwable exception, Object... additionalArgs)
	{
		String currentCachedValue = null;
		{
			WBRBCachePayload p = payload;
			if (p == null)
				if (cacheEntry != null)
					p = cacheEntry.getPayload();
			if (p != null)
			{
				@Nullable S cached = nullable(p.getValue()); // could be null if not read yet or cache contains null values
				if (cached != null)
					currentCachedValue = cached.toString();
			}
		}
		
		String writtenValue = null;
		{
			WBRBWriteQueueEntry wEntry = null;
			switch (event)
			{
				case DEBUG_EVENT_INTERNAL:
				case MAIN_QUEUE_ITEM_PROCESSED:
				case READ_FAIL_DECISION:
				case READ_QUEUE_ITEM_PROCESSED:
				case READ_SUCCESS_DECISION:
				case RETURN_QUEUE_ITEM_PROCESSED:
				case CACHE_ADD:
				case CACHE_REMOVE:
				case CACHE_READ:
				case CACHE_WRITE:
					break;
				case WRITE_SUCCESS:
					wEntry = TypeUtil.coerce(additionalArgs[0]);
					break;
				case WRITE_QUEUE_ITEM_PROCESSED:
					currentCachedValue = "<<UNKNOWN>>"; // this event doesn't provide easy access to payload
					//$FALL-THROUGH$
				case WRITE_FAIL_DECISION:
					wEntry = TypeUtil.coerce(additionalArgs[1]);
					break;
			}
			if (wEntry != null)
			{
				W wData = wEntry.getDataToWrite();
				if (wData != null)
					writtenValue = wData.toString();
			}
		}
		
		if (exception == null)
			return new Pair<>("[event] {} [{}] cached[{}] written[{}] {} {}", 
				new @Nullable Object[] {event, key, currentCachedValue, writtenValue, Arrays.toString(additionalArgs), commonNamingPrefix});
		else
		{
			// last 'unused' exception will be printed out as stack trace by slf4j
			return new Pair<>("[event] {} [{}] cached[{}] written[{}] {} {}", 
				new @Nullable Object[] {event, key, currentCachedValue, writtenValue, Arrays.toString(additionalArgs), commonNamingPrefix, exception});
		}
	}
	
	/**
	 * Used to track how many times specific message type has been logged already
	 * in order to decide when to throttle.
	 */
	@RequiredArgsConstructor
	@ToString
	protected static class WBRBMessageTypeLoggingCounter
	{
		/**
		 * Track when this message counting period started.
		 */
		@Getter
		private final long periodStartTime;

		/**
		 * Message counter.
		 */
		@Getter
		private final AtomicInteger messageCounter = new AtomicInteger(0);
	}
	
	/**
	 * Used to track message stats via {@link WBRBCacheMessage#ordinal()}
	 */
	protected final AtomicReference<WBRBMessageTypeLoggingCounter>[] messageTypeCountersArray;
	
	/**
	 * Used to track message stats via classificators in {@link #logNonStandardMessage(WBRBCacheMessageSeverity, String, Throwable, Object...)}
	 */
	protected final ConcurrentHashMap<String, AtomicReference<WBRBMessageTypeLoggingCounter>> messageClassificatorCountersMap = 
		new ConcurrentHashMap<String, AtomicReference<WBRBMessageTypeLoggingCounter>>();
	
	/**
	 * Implementation of event logging that only logs messages so long as there
	 * weren't 'too many' messages of a particular kind logged already.
	 * <p>
	 * If message is not throttled, it is sent to {@link #spiUnknownLockLogMessage_Plain(WBRBCacheMessage, Throwable, Object...)}
	 * for actual logging.
	 */
	protected void spiUnknownLockLogMessage_Throttled(WBRBCacheMessage msg, @Nullable Throwable exception, Object... args)
		throws InterruptedException
	{
		boolean logMessage = false;
		boolean logNextMessagesMayBeSkipped = false;
		
		final int msgLimit = config.getLogThrottleMaxMessagesOfTypePerTimeInterval();
		long gap = -1; // this is used to log message below, so 'global' variable
		long throttleIntervalDuration = -1; // this is used to log message below, so 'global' variable
		final String msgId;
		if (msgLimit < 1)
		{
			// Always log if throttling value is zero
			logMessage = true;
			msgId = msg.toString();
		}
		else if (spiUnknownLockLogMessage_IsThrottlingMessage(msg, exception, args))
		{
			// No throttling for throttling messages to avoid weirdness
			logMessage = true;
			msgId = msg.toString();
		}
		else
		{
			final AtomicReference<WBRBMessageTypeLoggingCounter> reference;
			if (!msg.isNonStandard())
			{
				reference = messageTypeCountersArray[msg.ordinal()];
				msgId = msg.toString();
			}
			else
			{	
				String classificator;
				try
				{
					classificator = nnChecked((String)args[0]);
				} catch (Throwable e)
				{
					throw new CacheInternalException("Non-standard message type [" + msg + "] must provide String classificator as first argument, got: " + Arrays.toString(args), e);
				}
				
				msgId = classificator + '_' + msg.getSeverity(); // append severity in case same classificator is used with different severities
				reference = messageClassificatorCountersMap.computeIfAbsent(msgId, k -> new AtomicReference<>(new WBRBMessageTypeLoggingCounter(0)));
			}
			
			WBRBMessageTypeLoggingCounter throttleData = reference.get();
			
			long now = timeNow();
			
			gap = timeGapVirtual(throttleData.getPeriodStartTime(), now);
			
			throttleIntervalDuration = config.getLogThrottleTimeInterval();
			
			if (gap > throttleIntervalDuration)
			{
				// Need to create a new counter data.
				WBRBMessageTypeLoggingCounter newThrottleData = new WBRBMessageTypeLoggingCounter(now);
				
				if (reference.compareAndSet(throttleData, newThrottleData))
				{
					// using our instance
					int oldCount = throttleData.getMessageCounter().get();
					if (oldCount > msgLimit)
					{
						// Log approximately how many messages were skipped
						logMessage(WBRBCacheMessage.LOG_MESSAGE_TYPE_PREVIOUS_MESSAGES_SKIPPED, null, msgId, (oldCount - msgLimit));
					}
					throttleData = newThrottleData; 
				}
				else
					throttleData = reference.get(); // someone else set it, use that one
				
				gap = timeGapVirtual(throttleData.getPeriodStartTime(), now); // recalculate gap since throttle data was replaced
			}
			
			int messageCount = throttleData.getMessageCounter().incrementAndGet();
			if (messageCount <= msgLimit)
			{
				logMessage = true;
				if (messageCount == msgLimit)
					logNextMessagesMayBeSkipped = true;
					
			}
		}
		
		if (logMessage)
		{
			// Send to actual logging
			spiUnknownLockLogMessage_Plain(msg, exception, args);
		}
		if (logNextMessagesMayBeSkipped)
		{
			// Log that further messages may be skipped for X ms
			logMessage(WBRBCacheMessage.LOG_MESSAGE_TYPE_MESSAGES_MAY_BE_SKIPPED_FOR, null, msgId, (throttleIntervalDuration - gap));
		}
	}

	
	/**
	 * Determines whether the given message is a 'throttling message'.
	 * Throttling messages are handled differently, e.g. they don't participate
	 * in throttling and don't affect message tracking.
	 */
	@SuppressWarnings({"deprecation", "unused"})
	protected boolean spiUnknownLockLogMessage_IsThrottlingMessage(WBRBCacheMessage msg, @Nullable Throwable exception, Object... args)
	{
		switch (msg)
		{
			case LOG_MESSAGE_TYPE_MESSAGES_MAY_BE_SKIPPED_FOR:
			case LOG_MESSAGE_TYPE_PREVIOUS_MESSAGES_SKIPPED:
				return true;
				
			case APPLY_UPDATE_FAIL:
			case ASSERTION_FAILED:
			case CONVERT_FROM_CACHE_FORMAT_TO_RETURN_VALUE_FAIL:
			case CONVERT_TO_CACHE_FORMAT_FROM_STORAGE_DATA_FAIL:
			case CONVERT_TO_INTERNAL_UPDATE_FORMAT_FROM_EXTERNAL_UPDATE_FAIL:
			case FLUSH_REQUESTED:
			case FLUSH_SPOOLDOWN_NOT_ACHIEVED:
			case FLUSH_SUCCESFULLY_COMPLETED:
			case MAIN_QUEUE_NON_STANDARD_OUTCOME:
			case MAIN_QUEUE_PROCESSOR_UNEXPECTED_INTERRUPT:
			case MERGE_CACHE_AND_STORAGE_DATA_FAIL:
			case NON_STANDARD_DEBUG:
			case NON_STANDARD_ERROR:
			case NON_STANDARD_EXTERNAL_DATA_LOSS:
			case NON_STANDARD_EXTERNAL_ERROR:
			case NON_STANDARD_EXTERNAL_INFO:
			case NON_STANDARD_EXTERNAL_WARN:
			case NON_STANDARD_FATAL:
			case NON_STANDARD_INFO:
			case NON_STANDARD_WARN:
			case NOT_PRESENT_ELEMENT_REMOVAL_ATTEMPT:
			case READ_QUEUE_PROCESSOR_UNEXPECTED_INTERRUPT:
			case RESYNC_FAILED_FINAL_DATA_DISCARDED:
			case RESYNC_FAILED_FINAL_STORAGE_DATA_OVERWRITE:
			case RESYNC_IS_TOO_LATE:
			case RETURN_QUEUE_NON_STANDARD_OUTCOME:
			case RETURN_QUEUE_PROCESSOR_UNEXPECTED_INTERRUPT:
			case RETURN_QUEUE_ITEM_NOT_RETAINED_DUE_TO_MAIN_QUEUE_SIZE:
			case RETURN_QUEUE_NEGATIVE_TIME_SINCE_TOUCHED:
			case SHUTDOWN_COMPLETED:
			case SHUTDOWN_REQUESTED:
			case SHUTDOWN_SPOOLDOWN_NOT_ACHIEVED:
			case SPI_EXCEPTION_Event:
			case SPI_EXCEPTION_GetReadQueueProcessorLock:
			case SPI_EXCEPTION_LogMessage:
			case SPI_EXCEPTION_MakeCacheReadDecision:
			case SPI_EXCEPTION_MakeCacheWriteDecision:
			case SPI_EXCEPTION_MakeInitialReadFailedFinalDecision:
			case SPI_EXCEPTION_MakeMainQueueProcessingDecision:
			case SPI_EXCEPTION_MakeMainQueueProcessingDecision_ResyncFailedFinal:
			case SPI_EXCEPTION_MakeMainQueueProcessingDecision_ResyncPending:
			case SPI_EXCEPTION_MakeMainQueueProcessingDecision_WriteFailedFinal:
			case SPI_EXCEPTION_MakeMainQueueProcessingDecision_WritePending:
			case SPI_EXCEPTION_MakeMainQueueProcessingDecision_isResetFailureCounts:
			case SPI_EXCEPTION_MakeMainQueueProcessingDecision_logNonStandardOutcome:
			case SPI_EXCEPTION_MakeMergeDecision:
			case SPI_EXCEPTION_MakeReadQueueProcessingDecision:
			case SPI_EXCEPTION_MakeReadRetryDecision:
			case SPI_EXCEPTION_MakeResyncFailedFinalDecision:
			case SPI_EXCEPTION_MakeReturnQueueProcessingDecision:
			case SPI_EXCEPTION_MakeReturnQueueProcessingDecision_WriteFailedFinal:
			case SPI_EXCEPTION_MakeReturnQueueProcessingDecision_WriteOk:
			case SPI_EXCEPTION_MakeReturnQueueProcessingDecision_WritePending:
			case SPI_EXCEPTION_MakeReturnQueueProcessingDecision_logNonStandardOutcome:
			case SPI_EXCEPTION_MakeWriteQueueProcessingDecision:
			case SPI_EXCEPTION_MakeWriteRetryDecision:
			case SPI_EXCEPTION_ReadBatchDelayExpired:
			case SPI_EXCEPTION_ReadFromStorage:
			case SPI_EXCEPTION_Updates_collect:
			case SPI_EXCEPTION_Updates_isMergePossible:
			case SPI_EXCEPTION_Updates_reset:
			case SPI_EXCEPTION_WriteBatchDelayExpired:
			case SPI_EXCEPTION_WriteToStorage:
			case SPLIT_FOR_WRITE_FAIL:
			case STARTED:
			case STORAGE_READ_FAIL:
			case STORAGE_READ_FAIL_FINAL:
			case STORAGE_READ_RETRY_ISSUED:
			case STORAGE_WRITE_FAIL:
			case STORAGE_WRITE_FAIL_FINAL:
			case STORAGE_WRITE_RETRY_ISSUED:
			case TEST_WARN:
			case TOO_MANY_CACHE_ELEMENT_UPDATES:
			case TOO_MANY_REMOVED_FROM_CACHE_STATE_RETRIES:
			case UNEXPECTED_CACHE_REMOVAL_IN_ADD_ENTRY:
			case UNEXPECTED_CACHE_REMOVAL_IN_MAIN_QUEUE_PROCESSING:
			case UNEXPECTED_CACHE_REMOVAL_IN_RETURN_QUEUE_PROCESSING:
			case CACHE_ADD_MAIN_QUEUE_SIZE_WARNING:
			case CACHE_ADD_FAIL_CACHE_SIZE_LIMIT_EXCEEDED:
			case UNEXPECTED_CACHE_STATE_FOR_READ_FAIL:
			case UNEXPECTED_CACHE_STATE_FOR_READ_MERGE:
			case UNEXPECTED_CACHE_STATE_FOR_READ_QUEUE_PROCESSING:
			case UNEXPECTED_CACHE_STATE_FOR_WRITE_FAIL:
			case UNEXPECTED_CACHE_STATE_FOR_WRITE_QUEUE_PROCESSING:
			case UNEXPECTED_CACHE_STATE_FOR_WRITE_SUCCESS:
			case WRITE_FAILED_FINAL_DATA_DISCARDED:
			case WRITE_QUEUE_PROCESSOR_UNEXPECTED_INTERRUPT:
				return false;
		}
		
		// It ought to be an assertion error, but we'll be lenient here because
		// it's not very important and there are many message types.
		return false;
	}
	
	/**
	 * Invoked when there's message to log by the cache.
	 * <p>
	 * This method is used to provide a 'guard' against failures around the
	 * {@link #spiUnknownLockLogMessage(WBRBCacheMessage, Throwable, Object...)}
	 * method, so usually SHOULD NOT be overridden.
	 * <p>
	 * WARNING: this can (and should be able to) throw {@link InterruptedException},
	 * however since dealing with it in most places is not required (it should
	 * just propagate upwards to indicate potential thread termination), it uses
	 * {@link SneakyThrows}
	 */
	@SneakyThrows(InterruptedException.class) // see comment above
	protected void logMessage(@NonNull WBRBCacheMessage msg, @Nullable Throwable exception, Object... args)
	{
		try
		{
			if (!spiUnknownLockLogMessage_IsThrottlingMessage(msg, exception, args))
			{
				// Record message timestamp
				getStats().lastTimestampMsgPerSeverityOrdinal[msg.getSeverity().ordinal()].set(timeNow());
				
				switch (msg.getSeverity())
				{
					case DEBUG:
					case INFO:
					case EXTERNAL_INFO:
						break;
					case WARN:
						getStats().msgWarnCount.incrementAndGet();
						break;
					case EXTERNAL_WARN:
						getStats().msgExternalWarnCount.incrementAndGet();
						break;
					case EXTERNAL_ERROR:
						getStats().msgExternalErrorCount.incrementAndGet();
						break;
					case EXTERNAL_DATA_LOSS:
						getStats().msgExternalDataLossCount.incrementAndGet();
						break;
					case ERROR:
						getStats().msgErrorCount.incrementAndGet();
						break;
					case FATAL:
						getStats().msgFatalCount.incrementAndGet();
						break;
				}
			}
			
			spiUnknownLockLogMessage(msg, exception, args);
		} catch (Throwable loggingException)
		{
			if (loggingException instanceof ThreadDeath)
				throw loggingException;
			if (loggingException instanceof InterruptedException) // this may be used to indicate that thread should exit 
				throw loggingException;
			
			getStats().msgErrorCount.incrementAndGet();

			// Logging failed, try to log that fact, but it may well fail itself
			// TO-DO monitor
			try
			{
				spiUnknownLockLogMessage(WBRBCacheMessage.SPI_EXCEPTION_LogMessage, loggingException);
			} catch (Exception e2)
			{
				// This is a problem, we can't use standard logging mechanism here because it just failed
				// so just log it directly
				getStats().msgErrorCount.incrementAndGet();
				try
				{
					defaultWBRBlog.error("LOGGING FAILED for: " + msg + ": " + loggingException, loggingException);
				} catch (Exception e3)
				{
					// ignore this
				}
			}
		}
	}
	
	
	/**
	 * Invoked when there's message to log by the cache that doesn't fit any
	 * of the 'standard' message categories -- to be used in e.g. subclasses.
	 * 
	 * @param classifier non-null string used for message classification, e.g.
	 * 		for message throttling
	 */
	@SuppressWarnings("deprecation") // deprecated is used to mark WBRBCacheMessage instances that are not supposed to be used directly by clients
	protected void logNonStandardMessage(WBRBCacheMessageSeverity severity, @NonNull String classifier, @Nullable Throwable exception, Object... args)
	{
		WBRBCacheMessage msg = fakeNonNull(); // to make compiler happy
		switch (severity)
		{
			case DEBUG:
				msg = WBRBCacheMessage.NON_STANDARD_DEBUG;
				break;
			case ERROR:
				msg = WBRBCacheMessage.NON_STANDARD_ERROR;
				break;
			case EXTERNAL_DATA_LOSS:
				msg = WBRBCacheMessage.NON_STANDARD_EXTERNAL_DATA_LOSS;
				break;
			case EXTERNAL_ERROR:
				msg = WBRBCacheMessage.NON_STANDARD_EXTERNAL_ERROR;
				break;
			case EXTERNAL_INFO:
				msg = WBRBCacheMessage.NON_STANDARD_EXTERNAL_INFO;
				break;
			case EXTERNAL_WARN:
				msg = WBRBCacheMessage.NON_STANDARD_EXTERNAL_WARN;
				break;
			case FATAL:
				msg = WBRBCacheMessage.NON_STANDARD_FATAL;
				break;
			case INFO:
				msg = WBRBCacheMessage.NON_STANDARD_INFO;
				break;
			case WARN:
				msg = WBRBCacheMessage.NON_STANDARD_WARN;
				break;
		}
		
		Object[] newArgs = new @Nonnull Object[args.length + 1];
		newArgs[0] = classifier;
		if (args.length > 0)
			System.arraycopy(args, 0, newArgs, 1, args.length);
		
		logMessage(msg, exception, newArgs);
	}
	
	
	// TO-DO preload tests?
	@Override
	public void preloadCache(K key) throws IllegalArgumentException, CacheFullException,
		CacheIllegalStateException, CacheInternalException, WAInterruptedException
	{
		try
		{
			haveNoLock_CheckCache(key, true, true, Boolean.TRUE);
		} catch (InterruptedException e)
		{
			throw new WAInterruptedException("Cache preload interrupted for [" + key + "]: " + e, e);
		}
	}
	
	/**
	 * Checks cache for the element presence; if addIsMissing is set, will add
	 * an empty cache element if not currently present
	 * 
	 * @param addIfMissing if true, element will be added to cache if missing
	 * @param returnNullIfAdded if true and element was just added by this method,
	 * 		return null instead of cache entry (otherwise return cache entry)
	 * @param isPreload whether this is for preloading data; this is currently
	 * 		used only for monitoring purposes; null means that most counters
	 * 		should not be incremented (e.g. because this is a retry)
	 * 
	 * @return cache element for the key or null if it's not present and addIsMissing
	 * 		flag is not set
	 * 
	 * @throws IllegalArgumentException if key is null
	 * @throws CacheFullException if element is not currently cached and cache
	 * 		is full (so no additional element may be added)
	 */
	@Nullable
	protected WBRBCacheEntry haveNoLock_CheckCache(K key, 
		boolean addIfMissing, boolean returnNullIfAdded, final @Nullable Boolean isPreload) 
		throws IllegalArgumentException, CacheFullException, InterruptedException
	{
		checkStandardCacheOperationsAllowed();
		
		getStats().checkCacheAttemptsNoDedup.incrementAndGet(); // always increment
		
		if (nullable(key) == null)
		{
			getStats().checkCacheNullKeyCount.incrementAndGet();
			throw new IllegalArgumentException("Key must not be null.");
		}
		
		if (isPreload != null)
		{
			if (isPreload)
				getStats().checkCachePreloadAttempts.incrementAndGet();
			else
				getStats().checkCacheReadAttempts.incrementAndGet();
		}
		
		WBRBCacheEntry value = inflightMap.get(key);
		if (value != null)
		{
			if (isPreload != null)
			{
				if (isPreload)
					getStats().checkCachePreloadCacheHit.incrementAndGet();
				else
					getStats().checkCacheReadCacheHit.incrementAndGet();
			}
			
			return value; // already have it cache, although it's not guaranteed to be available
		}
		
		if (!addIfMissing)
			return null;
		
		// TO-DO not clear whether concurrent map mappingCount() performance is 'good enough'
		long cacheSize = inflightMap.mappingCount();
		
		if (cacheSize >= config.getMaxCacheElementsHardLimit())
		{
			if (isPreload != null)
			{
				if (isPreload)
					getStats().checkCachePreloadCacheFullExceptionCount.incrementAndGet();
				else
					getStats().checkCacheReadCacheFullExceptionCount.incrementAndGet();
			}
			
			logMessage(WBRBCacheMessage.CACHE_ADD_FAIL_CACHE_SIZE_LIMIT_EXCEEDED, null, key, cacheSize);
			throw new CacheFullException(config.getCacheName(), cacheSize, config.getMaxCacheElementsHardLimit());
		}
		
		long mainQueueSize = mainQueue.size();
		if (mainQueueSize > config.getMainQueueMaxTargetSize())
			logMessage(WBRBCacheMessage.CACHE_ADD_MAIN_QUEUE_SIZE_WARNING, null, key, mainQueueSize);
		
		WBRBCacheEntry entry = new WBRBCacheEntry(key, timeNow());
		WBRBCachePayload payload = entry.getPayload();
		wrappedSpiWriteLockUpdates_reset(WBRBUpdatesResetReason.NO_WRITE_LOCK_NEW_CACHE_ENTRY_CREATED, false, key, entry, payload);
		
		boolean removeEntry = true; // this is to make sure we don't leave orphans in inflight map
		WBRBCacheEntry prev = inflightMap.putIfAbsent(key, entry);
		if (prev != null)
		{
			if (isPreload != null)
			{
				if (isPreload)
					getStats().checkCachePreloadCacheHit.incrementAndGet();
				else
					getStats().checkCacheReadCacheHit.incrementAndGet();
			}
			
			return prev; // someone else added entry before us, return that one
		}
		
		try
		{
			// 'in queue since' is already set above
			mainQueue.add(entry);
			removeEntry = false; // keep inflight map entry
		} finally
		{
			if (removeEntry)
			{
				haveNoLock_RemoveFromCache(entry);
				logMessage(WBRBCacheMessage.UNEXPECTED_CACHE_REMOVAL_IN_ADD_ENTRY, null, key);
			}
		}
		
		// We added new entry, need to schedule it for reading.
		readQueue.add(entry);
		
		// Event notification
		wrappedSpiUnknownLock_Event(WBRBEvent.CACHE_ADD, key, entry, null, null);								
		
		if (returnNullIfAdded)
			return null;
		else
			return entry;
	}
	
	/**
	 * Removes an element from cache -- more specifically removes it from
	 * in-flight map and updates status.
	 * <p>
	 * This must be called WITHOUT HOLDING A READ LOCK (or better yet -- no lock)
	 * because READ LOCK CANNOT BE UPGRADED TO WRITE LOCK.
	 */
	protected void haveNoLock_RemoveFromCache(WBRBCacheEntry cacheEntry)
		throws InterruptedException
	{
		// Thread we're executing in might have interrupted flag, so do the
		// critical, non-interruptible stuff first
		internal_lockDoesntMatter_RemoveEntryFromInflightMap(cacheEntry, true);
		
		// Now attempt to get a write lock and mark entry as removed... 
		withWriteLock(cacheEntry, () -> {
			internal_haveWriteLock_MarkEntryAsRemovedFromCache(cacheEntry);
		});
	}
	
	/**
	 * Removes an element from cache -- more specifically removes it from
	 * in-flight map and updates status.
	 * <p>
	 * This must be called WHILE ALREADY HOLDING THE WRITE LOCK!
	 */
	protected void haveWriteLock_RemoveFromCache(WBRBCacheEntry cacheEntry)
		throws InterruptedException
	{
		internal_haveWriteLock_RemoveFromCache(cacheEntry, true);
	}
	
	/**
	 * Removes an element from cache -- more specifically removes it from
	 * in-flight map and updates status.
	 * <p>
	 * If element is already marked with {@link WBRBCacheEntryReadStatus#REMOVED_FROM_CACHE},
	 * then warning message will not be produced about 'element already removed'
	 * when trying to remove from in-flight map.
	 * <p>
	 * This must be called WHILE ALREADY HOLDING THE WRITE LOCK!
	 */
	protected void haveWriteLock_RemoveFromCache_NoMessageIfAlreadyRemoved(WBRBCacheEntry cacheEntry)
		throws InterruptedException
	{
		internal_haveWriteLock_RemoveFromCache(cacheEntry, 
			cacheEntry.getPayload().getReadStatus() != WBRBCacheEntryReadStatus.REMOVED_FROM_CACHE);
	}
	
	/**
	 * Removes an element from cache -- more specifically removes it from
	 * in-flight map and updates status.
	 * <p>
	 * This must be called WHILE ALREADY HOLDING THE WRITE LOCK!
	 */
	protected void internal_haveWriteLock_RemoveFromCache(WBRBCacheEntry cacheEntry, boolean logIfElementNotPresent)
		throws InterruptedException
	{
		// Thread we're executing in might have interrupted flag, so do the
		// critical, non-interruptible stuff first
		internal_lockDoesntMatter_RemoveEntryFromInflightMap(cacheEntry, logIfElementNotPresent);
		
		// Now attempt to get a write lock and mark entry as removed... 
		internal_haveWriteLock_MarkEntryAsRemovedFromCache(cacheEntry);
	}

	// CCC
	protected void internal_lockDoesntMatter_RemoveEntryFromInflightMap(WBRBCacheEntry cacheEntry, boolean logIfElementNotPresent)
		throws InterruptedException
	{
		@Nonnull K key = cacheEntry.getKey();

		if (inflightMap.remove(key, cacheEntry)) // only removes if key maps to the value being removed
		{
			// Event notification
			wrappedSpiUnknownLock_Event(WBRBEvent.CACHE_REMOVE, key, cacheEntry, null, null);								
		}
		else
		{
			if (logIfElementNotPresent)
				logMessage(WBRBCacheMessage.NOT_PRESENT_ELEMENT_REMOVAL_ATTEMPT, new Exception(WBRBCacheMessage.NOT_PRESENT_ELEMENT_REMOVAL_ATTEMPT.toString() + " stack trace"), key);
		}
	}
	
	// CCC
	protected void internal_haveWriteLock_MarkEntryAsRemovedFromCache(WBRBCacheEntry cacheEntry)
		throws InterruptedException
	{
		@Nonnull K key = cacheEntry.getKey();
		
		WBRBCachePayload payload = cacheEntry.getPayload();
		
		// TO-DO should it set the write status too?
		cacheEntry.getPayload().setReadStatus(WBRBCacheEntryReadStatus.REMOVED_FROM_CACHE);
		
		// Clear memory used by any collected updates
		wrappedSpiWriteLockUpdates_reset(WBRBUpdatesResetReason.REMOVED_FROM_CACHE, false, key, cacheEntry, payload);
	}
	
	/**
	 * Creates read queue processor thread.
	 */
	protected ExitableThread createReadQueueProcessor()
	{
		InterruptHandlingExitableThread thread = new InterruptHandlingExitableThread(threadGroup, commonNamingPrefix + " Read Queue Processor")
		{
			@Override
			protected void run1(boolean reentry)
				throws InterruptedException
			{
				runnableReadQueueProcessor();
			}

			@Override
			protected boolean handleUnexpectedInterruptedException(
				InterruptedException e)
			{
				logMessage(WBRBCacheMessage.READ_QUEUE_PROCESSOR_UNEXPECTED_INTERRUPT, e);
				
				return true; // don't exit, restart processing
			}
			
		};
		
		thread.setPriority(config.getReadQueueProcessingThreadPriority());
		
		return thread;
	}
	
	/**
	 * Code executed by {@link #readQueueProcessingThread}
	 */
	protected void runnableReadQueueProcessor() throws InterruptedException
	{
		WAThreadPoolExecutor pool = readThreadPool;
		
		SimpleLongCounter batchCount = new SimpleLongCounter(0);
		while(true)
		{
			final WBRBCacheEntry cacheEntry;
			
			if ((batchCount.get() > 0) && (config.getReadQueueBatchingDelay() > 0))
			{
				// Here we are during batch processing (i.e. some elements were already read)
				cacheEntry = pollQueue(readQueue, config.getReadQueueBatchingDelay());
				if (cacheEntry == null)
				{
					// batch is over.
					guardedInvocation(() -> spiNoLockReadBatchDelayExpired(), WBRBCacheMessage.SPI_EXCEPTION_ReadBatchDelayExpired);
					batchCount.reset();
					continue;
				}
			}
			else
			{
				// no batching or zero elements in current batch
				batchCount.reset();
				cacheEntry = readQueue.take(); // wait indefinitely for the next one
			}
			
			getStats().storageReadQueueProcessedItems.incrementAndGet();
			
			K key = cacheEntry.getKey();
			
			BooleanObjectWrapper setFailedStatus = BooleanObjectWrapper.of(false);
			guardedInvocationNonNull(() -> spiNoLockGetReadQueueProcessorLock(cacheEntry), WBRBCacheMessage.SPI_EXCEPTION_GetReadQueueProcessorLock, key)
				.ifPresentInterruptibly(lock -> {
					BooleanObjectWrapper isRefreshRead = BooleanObjectWrapper.of(false);
					BooleanObjectWrapper doInlineReadOutsideLock = BooleanObjectWrapper.of(false);
					withLock(lock, cacheEntry, () -> { // we use lock because we need to read status from the payload
						// Here we have proper lock and can do standard processing.
						WBRBCachePayload payload = cacheEntry.getPayload();
						WBRBCacheEntryReadStatus status = payload.getReadStatus();
						
						guardedInvocationNonNull(() -> spiSomeLockMakeReadQueueProcessingDecision(key, status, cacheEntry, payload), WBRBCacheMessage.SPI_EXCEPTION_MakeReadQueueProcessingDecision, key)
							.ifPresentInterruptibly(decision -> {
								// Here we have decision and can process it
								switch (decision)
								{
									case DO_NOTHING:
										break;
									case SET_FINAL_FAILED_READ_STATUS:
										setFailedStatus.setTrue();
										break;
									case REFRESH_READ:
										isRefreshRead.setTrue();
										//$FALL-THROUGH$
									case INITIAL_READ:
										// ok, execute the read
										if (isRefreshRead.isTrue())
											getStats().storageReadRefreshAttempts.incrementAndGet();
										else
											getStats().storageReadInitialAttempts.incrementAndGet();
										if (pool == null)
										{
											// Execute in our own thread
											// CANNOT do it here since we hold some lock, do it later!
											doInlineReadOutsideLock.setTrue();
										}
										else
										{
											// Execute in the pool.
											pool.waExecute(() -> {
												// can't use guarded invocation as those throw InterruptedException
												try
												{
													spiNoLockProcessReadFromStorage(key, isRefreshRead.isTrue(), cacheEntry);
												} catch (Exception e)
												{
													// This shouldn't happen unless spi method is broken or execution thread is interrupted for w/e reason
													logMessage(WBRBCacheMessage.SPI_EXCEPTION_ReadFromStorage, e, key);
													try
													{
														withWriteLock(cacheEntry, () -> {
															payload.setReadStatus(WBRBCacheEntryReadStatus.READ_FAILED_FINAL);
														});
													} catch (InterruptedException e2)
													{
														// nothing else to do here.
													}
												}
											});
											batchCount.incrementAndGet();
										}
										break;
								}
								
								// Event notification
								wrappedSpiUnknownLock_Event(WBRBEvent.READ_QUEUE_ITEM_PROCESSED, key, cacheEntry, payload, null, decision);								
							})
							// Exception in spiSomeLockMakeReadQueueProcessingDecision
							.ifExceptionInterruptibly(e -> {
								setFailedStatus.setTrue();
							});
					}); // end lock
					
					if (doInlineReadOutsideLock.isTrue())
					{
						guardedInvocation(() -> spiNoLockProcessReadFromStorage(key, isRefreshRead.isTrue(), cacheEntry), WBRBCacheMessage.SPI_EXCEPTION_ReadFromStorage, key)
							.ifPresentInterruptibly(o -> {
								batchCount.incrementAndGet();
							})
							.ifExceptionInterruptibly(e -> {
								setFailedStatus.setTrue();
							});
					}
					
				}) // end of spiNoLockGetReadQueueProcessorLock normal processing 
				// Exception in spiNoLockGetReadQueueProcessorLock
				.ifExceptionInterruptibly(e -> {
					setFailedStatus.setTrue();
				});
			
			if (setFailedStatus.isTrue())
			{
				// Have to do it in a new lock, since the above lock could've been read and it cannot be upgraded
				withWriteLock(cacheEntry, () -> {
					cacheEntry.getPayload().setReadStatus(WBRBCacheEntryReadStatus.READ_FAILED_FINAL);
				});
			}
		}
	}
	
	/**
	 * This is invoked to 'finish' a read batch when there were some reads and 
	 * no additional reads queued for config.readQueueBatchingDelay time.
	 * <p>
	 * Default implementation does nothing.
	 * <p>
	 * This is a useful method if you're implementing batched reads.
	 * <p>
	 * WARNING: this is ALWAYS executed in read queue processor thread, so it
	 * must return VERY QUICKLY.
	 */
	@SuppressWarnings("unused")
	protected void spiNoLockReadBatchDelayExpired()
		throws InterruptedException
	{
		// nothing in default implementation
	}
	
	/**
	 * Returns lock to be used by read queue processor; normally (and by default)
	 * this is read lock; however if you're doing something special, you can
	 * change that here.
	 * TO-DO: reference read queue decision method here
	 */
	@SuppressWarnings("unused")
	protected Lock spiNoLockGetReadQueueProcessorLock(WBRBCacheEntry cacheEntry)
		throws InterruptedException
	{
		return cacheEntry.getLock().readLock();
	}
	
	/**
	 * This method is executed while holding *some* lock, as defined in
	 * {@link #spiNoLockGetReadQueueProcessorLock(WBRBCacheEntry)}; by default
	 * this is a read lock.
	 * <p>
	 * It must make a decision as to what read queue processor should do with
	 * the given item.
	 * <p>
	 * Default implementation looks at read status and bases decision on that;
	 * it will also log message if status is unexpected.
	 */
	@SuppressWarnings("unused")
	protected WBRBReadQueueProcessingDecision spiSomeLockMakeReadQueueProcessingDecision(
		K key, WBRBCacheEntryReadStatus status, 
		WBRBCacheEntry cacheEntry, 
		WBRBCachePayload payload)
			throws InterruptedException
	{
		switch (status)
		{
			case DATA_READY:
			case DATA_READY_RESYNC_FAILED_FINAL:
			case READ_FAILED_FINAL:
			case REMOVED_FROM_CACHE: // TO-DO not sure this status should produce message
				logMessage(WBRBCacheMessage.UNEXPECTED_CACHE_STATE_FOR_READ_QUEUE_PROCESSING, null, key, status);
				return WBRBReadQueueProcessingDecision.DO_NOTHING;
			case NOT_READ_YET:
				return WBRBReadQueueProcessingDecision.INITIAL_READ;
			case DATA_READY_RESYNC_PENDING:
				return WBRBReadQueueProcessingDecision.REFRESH_READ;
		}
		
		logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), "code should not be reachable");
		return WBRBReadQueueProcessingDecision.DO_NOTHING;
	}

	/**
	 * Overriding this method allows to change how cache processes time internally.
	 * <p>
	 * Time 'passes' at the speed of actual time * this factor (i.e. numbers over
	 * 1 'speed up' the time, numbers under 1 'slow' it down).
	 * <p>
	 * This is probably mostly useful for testing.
	 * <p>
	 * Default implementation returns {@link Float#NaN}
	 * 
	 * @return time factor or {@link Float#NaN} to indicate that time flow should
	 * 		be 'standard'
	 */
	protected float timeFactor()
	{
		return Float.NaN;
	}
	
	/**
	 * Lets custom implementations override what current time is; this is likely
	 * to break something unless done very-very carefully.
	 * <p>
	 * Default implementation returns {@link System#currentTimeMillis()}
	 * TO-DO make sure there are no other references to system.currentime
	 */
	protected long timeNow()
	{
		return System.currentTimeMillis();
	}
	
	/**
	 * Calculates time gap in virtual milliseconds (taking into account {@link #timeFactor()})
	 * between two 'real-world' timestamps.
	 * <p>
	 * So if e.g. real-world timestamps are 4000 and 5000; and time factor is 2;
	 * then the result will be (5000-4000) * 2 = 2000 virtual milliseconds
	 * <p>
	 * This never returns 0 unless both arguments are exactly equal.
	 * <p>
	 * The 'inverse' of this method is {@link #timeAddVirtualIntervalToRealWorldTime(long, long)}
	 */
	protected long timeGapVirtual(long realWorldStartTime, long realWorldEndTime)
	{
		if (realWorldEndTime == realWorldStartTime)
			return 0;
		
		float timeFactor = timeFactor();
		if (Float.isNaN(timeFactor))
			return realWorldEndTime - realWorldStartTime;
		
		double result = Math.ceil( (realWorldEndTime - realWorldStartTime) * timeFactor );
		
		if (result > 0)
			return (long)result;
		
		// Math.ceil returns 'higher' number for negative values including e.g. negative zero;
		// so subtract one for proper result
		return (long)(result - 1);
	}
	
	/**
	 * Adds a given virtual milliseconds interval (taking into account {@link #timeFactor()})
	 * to the given real-world timestamp.
	 * <p>
	 * So if real-world timestamp is 4000, virtual interval is 2000 and time
	 * factor is 2, then the result will be 4000 + (2000 / 2) = 5000
	 * <p>
	 * It's useful to calculate to e.g. how long to sleep.
	 * <p>
	 * This never returns the same real-world timestamp unless interval is zero.
	 * <p>
	 * The 'inverse' of this method is {@link #timeGapVirtual(long, long)}	 
	 */
	protected long timeAddVirtualIntervalToRealWorldTime(long realWorldTime, long virtualInterval)
	{
		float timeFactor = timeFactor();
		if (Float.isNaN(timeFactor))
			return realWorldTime + virtualInterval;
		
		double delta = Math.ceil(virtualInterval / timeFactor);
		// Math.ceil returns 'higher' number for negative values including e.g. negative zero;
		// so subtract one to get non-zero result in all cases
		if (delta < 1)
			delta = delta - 1;
		
		return realWorldTime + (long)delta;
	}
	
	/**
	 * Calculates real-world interval from the given virtual interval (taking 
	 * into account {@link #timeFactor()})
	 */
	protected long timeRealWorldInterval(long virtualInterval)
	{
		long now = timeNow();
		long targetTime = timeAddVirtualIntervalToRealWorldTime(now, virtualInterval);
		
		return targetTime - now;
	}
	
	/**
	 * Polls given BlockingQueue for the specified amount of time; takes care
	 * of respecting {@link #timeFactor()}
	 */
	@Nullable
	protected <E> E pollQueue(BlockingQueue<E> queue, long timeMillis) throws InterruptedException
	{
		return queue.poll(timeRealWorldInterval(timeMillis), TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Should implement reading from the storage based on the specified key.
	 * <p>
	 * These are typically called in the separate read threads, but it is still
	 * very important to produce results very quickly as updates to cached objects
	 * are collected while they wait for the background read & writes (and thus memory
	 * can be consumed significantly).
	 * <p>
	 * If your storage works better with batched reads, this can be implemented
	 * by overriding {@link #spiNoLockProcessReadFromStorage(Object, boolean, WBRBCacheEntry)}
	 * method (which allows to produce result later instead of 'right now') and
	 * {@link #spiNoLockReadBatchDelayExpired()} to detect 'batch end' events.
	 * <p>
	 * For batched reads you also might want to disable readThreadPool so that
	 * these methods are not called in separate threads unnecessarily.
	 * 
	 * @param key key to read
	 * @param isRefreshRead true if this read is intended to refresh value
	 * 		already in memory (i.e. there's expected to be a subsequent merge)
	 */
	protected abstract R readFromStorage(K key, boolean isRefreshRead)
		throws InterruptedException;
	
	
	/**
	 * A version of {@link #readFromStorage(Object, boolean)} that provides more
	 * access to the internal data (so the implementations may override this
	 * to get more access). 
	 * <p>
	 * Default implementation simply calls {@link #readFromStorage(Object, boolean)}
	 * and returns the result.
	 */
	protected R spiNoLockReadFromStorage(K key, boolean isRefreshRead, @SuppressWarnings("unused") WBRBCacheEntry cacheEntry) 
		throws InterruptedException
	{
		return readFromStorage(key, isRefreshRead);
	}
	
	
	/**
	 * Converts data from external update format (the update type used in public
	 * API) to the internal update format (used to store & apply updates internally).
	 * <p>
	 * This is executed without holding any locks, but it is executed on each
	 * write operation, so performance is important.
	 */
	protected abstract UInt convertToInternalUpdateFormatFromExternalUpdate(K key, UExt externalUpdate);
	
	/**
	 * Converts data read from the storage into format used by the cache internally;
	 * this is used in case of initial read, when there's nothing to merge.
	 * <p>
	 * WARNING: this is executed while holding write lock on the cache entry,
	 * therefore it should be VERY FAST.
	 * TO-DO desc
	 */
	protected abstract S convertToCacheFormatFromStorageData(K key, R storageData);
	
	/**
	 * Converts data read from the internal cache format to the return value type.
	 * This is used to produce results during the reads.
	 * <p>
	 * This is executed while holding read lock on the cache entry.
	 * TO-DO desc
	 */
	protected abstract V convertFromCacheFormatToReturnValue(K key, S cachedData);
	
	/**
	 * Applies given update to the cached state and returns updated cached state
	 * (can be the same updated instance or a new one)
	 * <p>
	 * This is done under write lock, so should complete ASAP. 
	 */
	protected abstract S applyUpdate(S cacheData, UInt update);
	
	/**
	 * @param previousFailedWriteData can be non-empty ONLY if {@link WBRBConfig#isCanMergeWrites()}
	 * 		is true; if it is true and previous write has failed with final status,
	 * 		then it is passed to the method here -- and it should be used to
	 * 		produce combined write data (combining previous failed write with
	 * 		further in-memory updates (if any)); NOTE: if this is non-empty,
	 * 		then it is possible that there were no further updates to cacheData
	 * 		(i.e. it may be unmodified since previous split)
	 */
	// CCC comment
	protected abstract WriteSplit splitForWrite(K key, S cacheData, NullableOptional<W> previousFailedWriteData);
	
	// CCC
	// NOTE: is NOT called if there are no pending writes UNLESS 'can merge failed writes' and previous write failed
	@SuppressWarnings("unused")
	protected WriteSplit spiWriteLockSplitForWrite(
		K key, S cacheData, NullableOptional<W> previousFailedWriteData, 
		WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		return splitForWrite(key, cacheData, previousFailedWriteData);
	}
	
	// CCC comment
	protected WBRBWriteSplitWithFlags spiWriteLockProcessSplitForWrite(
		K key, S cacheData, WBRBCacheEntry cacheEntry, 
		WBRBCachePayload payload)
			throws InterruptedException
	{
		NullableOptional<W> previousFailedWriteData = null;
		switch (payload.getWriteStatus())
		{
			case NO_WRITE_REQUESTED_YET:
			case REMOVED_FROM_CACHE:
			case WRITE_PENDING:
			case WRITE_SUCCESS:
				break;
			case WRITE_FAILED_FINAL:
				previousFailedWriteData = payload.getPreviousFailedWriteData();
				if (!previousFailedWriteData.isPresent())
				{
					CacheInternalException exception = new CacheInternalException("When write status is WRITE_FAILED_FINAL, previous failed write data should be present (got empty) in: " + cacheEntry);
					logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), "code should not be reachable");
					throw exception;
				}
				break;
		}
		
		payload.setPreviousFailedWriteData(NullableOptional.empty()); // reset previous failure if any
		
		if (previousFailedWriteData != null)
		{
			if (spiWriteLockIsCanMergeWrites(key, cacheData, previousFailedWriteData, cacheEntry, payload))
			{
				// Write merges are supported, so create a merged write
				return new WBRBWriteSplitWithFlags(spiWriteLockSplitForWrite(key, cacheData, previousFailedWriteData, cacheEntry, payload), true/*contains all updates*/);
			}
			else
			{
				// Write merges are not supported, so have to re-issue previous failed write
				boolean haveAllUpdates = payload.getLastWriteTimestamp() <= 0;
				return new WBRBWriteSplitWithFlags(new WriteSplit(payload.getValue(), previousFailedWriteData), haveAllUpdates/*flag: whether contains all updates*/);
			}
		}
		else
		{
			// No previous write failure, so normal operation
			if (payload.getLastWriteTimestamp() > 0)
				return new WBRBWriteSplitWithFlags(spiWriteLockSplitForWrite(key, cacheData, NullableOptional.empty(), cacheEntry, payload), true/*contains all updates*/);
			else
				return new WBRBWriteSplitWithFlags(new WriteSplit(payload.getValue()), true/*contains all updates*/);
		}
	}
	
	/**
	 * Determines whether writes can be merged for the given item.
	 * <p>
	 * Default implementation simply returns {@link WBRBConfig#isCanMergeWrites()} 
	 */
	@SuppressWarnings("unused")
	protected boolean spiWriteLockIsCanMergeWrites(K key, S cacheData, NullableOptional<W> previousFailedWriteData, 
		WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		return config.isCanMergeWrites(); 
	}
	
	/**
	 * Reads data from the underlying storage; does not need to be synchronous,
	 * completion is indicated at some point by invoking either of {@link #apiStorageReadSuccess(Object, WBRBCacheEntry)}
	 * or {@link #apiStorageReadFail(WBRBCacheEntry)} methods.
	 * <p>
	 * Default implementation is synchronous -- it invokes {@link #spiNoLockReadFromStorage(Object, boolean, WBRBCacheEntry)}
	 * and records the success result or failure (exception in {@link #spiNoLockReadFromStorage(Object, boolean, WBRBCacheEntry)} 
	 * indicates failed read).
	 */
	protected void spiNoLockProcessReadFromStorage(K key, boolean isRefreshRead, WBRBCacheEntry cacheEntry) 
			throws InterruptedException
	{
		guardedInvocation(() -> spiNoLockReadFromStorage(key, isRefreshRead, cacheEntry) , WBRBCacheMessage.STORAGE_READ_FAIL, key)
			.ifPresentInterruptibly(data -> apiStorageReadSuccess(data, cacheEntry))
			.ifExceptionInterruptibly(e -> apiStorageReadFail(e, cacheEntry));
	}
	
	/**
	 * Merges data between currently cached data and just-read storage data.
	 * <p>
	 * Default implementation converts data read from storage to cache format and
	 * then applies collected updates (if any) one by one.
	 */
	protected S spiWriteLockMergeCacheAndStorageData(K key, R storageData, 
		WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		S newCacheData = convertToCacheFormatFromStorageData(key, storageData);
		
		List<UInt> collectedUpdates = spiWriteLockUpdates_getCollectedUpdates(key, cacheEntry, payload);
		if (collectedUpdates != null)
		{
			for (UInt update : collectedUpdates)
			{
				newCacheData = applyUpdate(newCacheData, update);
			}
		}
		
		return newCacheData;
	}
	
	/**
	 * For an incoming successful storage read must make a decision whether to
	 * set or merge incoming data into the cache entry.
	 * <p>
	 * Default implementation sets/merges depending on cache payload status &
	 * also potentially logs message {@link WBRBCacheMessage#UNEXPECTED_CACHE_STATE_FOR_READ_MERGE}
	 * if read was unexpected.
	 * <p>
	 * Executed under write lock, so must finish ASAP.
	 * <p>
	 * NOTE: DO_NOTHING result will NOT release access latch (if it not released
	 * already), so be careful with that.
	 */
	protected WBRBMergeDecision spiWriteLockMakeMergeDecision(
		K key, R storageData, 
		WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		switch (payload.getReadStatus())
		{
			case NOT_READ_YET:
				return WBRBMergeDecision.SET_DIRECTLY;
			case DATA_READY:
			case DATA_READY_RESYNC_FAILED_FINAL:
				if (!spiWriteLockIsAcceptOutOfOrderRead(key, storageData, cacheEntry, payload))
				{
					logMessage(WBRBCacheMessage.UNEXPECTED_CACHE_STATE_FOR_READ_MERGE, null, key, payload.getReadStatus());
					return WBRBMergeDecision.DO_NOTHING;
				}
				//$FALL-THROUGH$
			case DATA_READY_RESYNC_PENDING:
				if (wrappedSpiWriteLockUpdates_isMergePossible(key, cacheEntry, payload, NullableOptional.of(storageData)))
					return WBRBMergeDecision.MERGE_DATA;
				else
				{
					getStats().storageReadRefreshTooLateCount.incrementAndGet(); // FUTURE: this is not a great place to do monitoring because it can be overridden 'relatively easily'
					return spiWriteLockMakeMergeDecision_ResyncTooLate(key, storageData, cacheEntry, payload);
				}
			case READ_FAILED_FINAL:
			case REMOVED_FROM_CACHE: // TO-DO not sure this status should produce message
				logMessage(WBRBCacheMessage.UNEXPECTED_CACHE_STATE_FOR_READ_MERGE, null, key, payload.getReadStatus());
				return WBRBMergeDecision.DO_NOTHING;
		}
		
		logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), "code should not be reachable");
		return WBRBMergeDecision.DO_NOTHING;
	}

	/**
	 * When out-of-order (unexpected) read arrives (e.g. via {@link #apiStorageReadSuccess(Object, WBRBCacheEntry)})
	 * decides whether to accept this read or reject it.
	 * <p>
	 * Default implementation returns {@link WBRBConfig#isAcceptOutOfOrderReads()}
	 */
	@SuppressWarnings("unused")
	protected boolean spiWriteLockIsAcceptOutOfOrderRead(K key, R storageData, 
		WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		return config.isAcceptOutOfOrderReads();
	}
	
	// CCC
	@SuppressWarnings("unused")
	protected WBRBMergeDecision spiWriteLockMakeMergeDecision_ResyncTooLate(
		K key, R storageData, 
		WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		WBRBMergeDecision decision = config.getResyncTooLateAction();
		logMessage(WBRBCacheMessage.RESYNC_IS_TOO_LATE, null, key, decision);
		return decision;
	}

	/**
	 * SPI: default implementation simply invokes {@link #convertToCacheFormatFromStorageData(Object, Object)}
	 * <p>
	 * WARNING: this is executed while holding write lock on the cache entry,
	 * therefore it should be VERY FAST.
	 * @param payload 
	 */
	@SuppressWarnings("unused")
	protected S spiWriteLockConvertToCacheFormat(K key, R storageData, 
		WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		return convertToCacheFormatFromStorageData(key, storageData);
	}

	/**
	 * SPI: default implementation simply invokes {@link #convertFromCacheFormatToReturnValue(Object, Object)}
	 * <p>
	 * This is executed while holding read OR write lock (depending on usage,
	 * this can be called e.g. from the write operation to obtain updated
	 * value) on the cache entry.
	 * 
	 * @param payload 
	 */
	@SuppressWarnings("unused")
	protected V spiSomeLockConvertFromCacheFormat(K key, S cachedData, 
		WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		return convertFromCacheFormatToReturnValue(key, cachedData);
	}
	
	/**
	 * For an incoming FAILED storage read must make a decision whether to
	 * go for a read retry ot not.
	 * <p>
	 * Default implementation first check that payload status is 'as expected'
	 * (if state is not expected, message is logged and {@link WBRBRetryDecision#DO_NOTHING}
	 * is returned);
	 * <p>
	 * If state is expeted, then decision depends on the already-failed read
	 * count and {@link WBRBConfig#getReadFailureMaxRetryCount()}
	 * <p>
	 * Executed under write lock, so must finish ASAP.
	 */
	@SuppressWarnings("unused")
	protected WBRBRetryDecision spiWriteLockMakeReadRetryDecision(
		@Nullable Throwable exception, K key, 
		WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		switch (payload.getReadStatus())
		{
			case NOT_READ_YET:
			case DATA_READY_RESYNC_PENDING:
				// standard processing
				if (payload.getReadFailureCount().get() > config.getReadFailureMaxRetryCount())
					return WBRBRetryDecision.NO_RETRY_SET_FINAL_FAILED_STATUS;
				else
					return WBRBRetryDecision.RETRY;
				
			case DATA_READY:
			case DATA_READY_RESYNC_FAILED_FINAL:
			case READ_FAILED_FINAL:
				logMessage(WBRBCacheMessage.UNEXPECTED_CACHE_STATE_FOR_READ_FAIL, null, payload.getReadStatus());
				return WBRBRetryDecision.DO_NOTHING;
			case REMOVED_FROM_CACHE: // TO-DO not sure this status should produce message
				return WBRBRetryDecision.DO_NOTHING;
		}
		
		logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), "code should not be reachable");
		return WBRBRetryDecision.DO_NOTHING;
	}
	
	// TO-DO comment
	protected void apiStorageReadSuccess(R readResult, WBRBCacheEntry cacheEntry)
		throws InterruptedException
	{
		withWriteLock(cacheEntry, () -> {
			WBRBCachePayload payload = cacheEntry.getPayload();
			
			switch (payload.getReadStatus())
			{
				case DATA_READY:
				case DATA_READY_RESYNC_FAILED_FINAL:
				case DATA_READY_RESYNC_PENDING:
				case REMOVED_FROM_CACHE: // this is a somewhat random choice
					getStats().storageReadRefreshSuccesses.incrementAndGet();
					break;
				case NOT_READ_YET:
				case READ_FAILED_FINAL:
					getStats().storageReadInitialSuccesses.incrementAndGet();
					break;
			}
			
			K key = cacheEntry.getKey();
			
			BooleanObjectWrapper doStuff = BooleanObjectWrapper.of(true);
			ObjectWrapper<NullableOptional<S>> result = ObjectWrapper.of(NullableOptional.empty());
			guardedInvocationNonNull(() -> spiWriteLockMakeMergeDecision(key, readResult, cacheEntry, payload), WBRBCacheMessage.SPI_EXCEPTION_MakeMergeDecision, key)
				.ifPresentInterruptibly(decision -> {
					boolean dataUsed = false;
					switch(decision)
					{
						case CLEAR_READ_PENDING_STATUS:
							doStuff.setFalse(); // no unlatching, etc.
							switch (payload.getReadStatus())
							{
								case DATA_READY:
								case DATA_READY_RESYNC_FAILED_FINAL:
								case READ_FAILED_FINAL:
								case REMOVED_FROM_CACHE:
									break;
								case DATA_READY_RESYNC_PENDING:
									payload.setReadStatus(WBRBCacheEntryReadStatus.DATA_READY_RESYNC_FAILED_FINAL);
									break;
								case NOT_READ_YET:
									payload.setReadStatus(WBRBCacheEntryReadStatus.READ_FAILED_FINAL);
									break;
							}
							break;
						case DO_NOTHING:
							doStuff.setFalse(); // no unlatching, etc.
							break;
						case SET_DIRECTLY:
							result.set( guardedInvocation(() -> spiWriteLockConvertToCacheFormat(key, readResult, cacheEntry, payload), 
								WBRBCacheMessage.CONVERT_TO_CACHE_FORMAT_FROM_STORAGE_DATA_FAIL, key) );
							dataUsed = true;
							break;
						case MERGE_DATA:
							result.set( guardedInvocation(() -> spiWriteLockMergeCacheAndStorageData(key, readResult, cacheEntry, payload),
								WBRBCacheMessage.MERGE_CACHE_AND_STORAGE_DATA_FAIL, key) );
							dataUsed = true;
							break;
						case REMOVE_FROM_CACHE:
							doStuff.setFalse(); // Nothing to do after cache removal
							haveWriteLock_RemoveFromCache_NoMessageIfAlreadyRemoved(cacheEntry);
							break;
					}
					
					if (!dataUsed)
						getStats().storageReadRefreshDataNotUsedCount.incrementAndGet();
					
					// Event notification
					wrappedSpiUnknownLock_Event(WBRBEvent.READ_SUCCESS_DECISION, key, cacheEntry, payload, null, decision, NullableOptional.of(readResult), result);								
				})
				.ifExceptionInterruptibly(e -> {
					result.set( NullableOptional.emptyWithException(e) );
				});
			
			if (doStuff.isTrue())
			{
				result.get().ifPresentInterruptibly(data -> {
					payload.setValue(data);
					payload.getReadFailureCount().reset();
					payload.setLastSyncedWithStorageTimestamp(timeNow());
					payload.setReadStatus(WBRBCacheEntryReadStatus.DATA_READY); // should also open latch if needed
					
					wrappedSpiWriteLockUpdates_reset(WBRBUpdatesResetReason.STORAGE_DATA_MERGED, false, key, cacheEntry, payload);
				})
				.ifExceptionInterruptibly(e -> {
					payload.getReadFailureCount().incrementAndGet();
					payload.setReadStatus(WBRBCacheEntryReadStatus.READ_FAILED_FINAL); // should also open latch if needed
				});
			}
		}); // end write lock
	}
	
	// TO-DO comment
	/**
	 * WARNING: will NOT log error/exception information if going for retry
	 * because normally it is already logged in {@link #spiNoLockProcessReadFromStorage(Object, boolean, WBRBCacheEntry)}
	 * If you're doing your own implementation, make sure to log the problem as needed.
	 * 
	 */
	protected void apiStorageReadFail(@Nullable Throwable exception, WBRBCacheEntry cacheEntry)
		throws InterruptedException
	{
		withWriteLock(cacheEntry, () -> {
			WBRBCachePayload payload = cacheEntry.getPayload();
			
			switch (payload.getReadStatus())
			{
				case DATA_READY:
				case DATA_READY_RESYNC_FAILED_FINAL:
				case DATA_READY_RESYNC_PENDING:
				case REMOVED_FROM_CACHE: // this is a somewhat random choice
					getStats().storageReadRefreshFailures.incrementAndGet();
					break;
				case NOT_READ_YET:
				case READ_FAILED_FINAL:
					getStats().storageReadInitialFailures.incrementAndGet();
					break;
			}
			
			K key = cacheEntry.getKey();

			payload.getReadFailureCount().incrementAndGet(); // increment fail counter
			
			ObjectWrapper<@Nonnull WBRBRetryDecision> retryDecision = ObjectWrapper.of(fakeNonNull());
			ObjectWrapper<Object> finalFailDecision = ObjectWrapper.of(null);
			BooleanObjectWrapper setReadFailedStatus = BooleanObjectWrapper.of(false);
			guardedInvocationNonNull(() -> spiWriteLockMakeReadRetryDecision(exception, key, cacheEntry, payload), WBRBCacheMessage.SPI_EXCEPTION_MakeReadRetryDecision, key)
				.ifPresentInterruptibly(decision -> {
					switch(decision)
					{
						case DO_NOTHING:
							break;
						case REMOVE_FROM_CACHE:
							haveWriteLock_RemoveFromCache(cacheEntry);
							break;
						case NO_RETRY_SET_FINAL_FAILED_STATUS:
							setReadFailedStatus.setTrue();
							logMessage(WBRBCacheMessage.STORAGE_READ_FAIL_FINAL, exception, key);
							break;
						case RETRY:
							// Need to re-queue read.
							readQueue.add(cacheEntry);
							logMessage(WBRBCacheMessage.STORAGE_READ_RETRY_ISSUED, exception, key);
							break;
					}
					
					retryDecision.set(decision);
				})
				.ifExceptionInterruptibly(e -> {
					setReadFailedStatus.setTrue();
				});
			
			if (setReadFailedStatus.isTrue())
			{
				WBRBCacheEntryReadStatus status = payload.getReadStatus();
				BooleanObjectWrapper stopCollectUpdates = BooleanObjectWrapper.of(true);
				switch(status)
				{
					case NOT_READ_YET:
						payload.setReadStatus(WBRBCacheEntryReadStatus.READ_FAILED_FINAL);

						// When initial read fails (final), need to make a decision on what to do now
						guardedInvocationNonNull(() -> spiWriteLockMakeInitialReadFailedFinalDecision(key, cacheEntry, payload), WBRBCacheMessage.SPI_EXCEPTION_MakeInitialReadFailedFinalDecision, key)
							.ifPresentInterruptibly(decision -> {
								switch (decision)
								{
									case REMOVE_FROM_CACHE:
										haveWriteLock_RemoveFromCache_NoMessageIfAlreadyRemoved(cacheEntry);
										break;
									case KEEP_AND_THROW_CACHE_READ_EXCEPTIONS:
										break; // Keep it, will throw access exceptions instead
								}
								
								finalFailDecision.set(decision);
							})
							.ifExceptionInterruptibly(e -> {
								haveWriteLock_RemoveFromCache(cacheEntry);
							});
						
						break;
					case DATA_READY_RESYNC_PENDING:
						payload.setReadStatus(WBRBCacheEntryReadStatus.DATA_READY_RESYNC_FAILED_FINAL);
						
						// When resync fails (final), need to make a decision on what to do now
						guardedInvocationNonNull(() -> spiWriteLockMakeResyncFailedFinalDecision(key, cacheEntry, payload), WBRBCacheMessage.SPI_EXCEPTION_MakeResyncFailedFinalDecision, key)
							.ifPresentInterruptibly(decision -> {
								switch (decision)
								{
									case REMOVE_FROM_CACHE:
										haveWriteLock_RemoveFromCache_NoMessageIfAlreadyRemoved(cacheEntry);
										break;
									case KEEP_COLLECTING_UPDATES:
										stopCollectUpdates.setFalse();
										break;
									case STOP_COLLECTING_UPDATES:
										break;
								}
								
								finalFailDecision.set(decision);
							})
							.ifExceptionInterruptibly(e -> {
								haveWriteLock_RemoveFromCache(cacheEntry);
							});
						
						break;
					case DATA_READY:
					case DATA_READY_RESYNC_FAILED_FINAL:
					case READ_FAILED_FINAL:
					case REMOVED_FROM_CACHE:
						logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), "code should not be reachable");
						break;
				}
				
				if (stopCollectUpdates.isTrue())
				{
					// Stop collecting updates if this is the decision
					wrappedSpiWriteLockUpdates_reset(WBRBUpdatesResetReason.READ_FAILED_FINAL_DECISION, false, key, cacheEntry, payload);
				}
			}
			
			// Event notification
			wrappedSpiUnknownLock_Event(WBRBEvent.READ_FAIL_DECISION, key, cacheEntry, payload, null, 
				retryDecision.get(), NullableOptional.of(finalFailDecision.get()));
		});
	}

	
	// CCC comment
	@SuppressWarnings("unused")
	protected WBRBInitialReadFailedFinalDecision spiWriteLockMakeInitialReadFailedFinalDecision(
		@Nonnull K key,
		WBRBCacheEntry cacheEntry,
		WBRBCachePayload payload)
			throws InterruptedException
	{
		return config.getInitialReadFailedFinalAction();
	}
	
	// CCC comment
	@SuppressWarnings("unused")
	protected WBRBResyncFailedFinalDecision spiWriteLockMakeResyncFailedFinalDecision(
		@Nonnull K key,
		WBRBCacheEntry cacheEntry,
		WBRBCachePayload payload)
			throws InterruptedException
	{
		return config.getResyncFailedFinalAction();
	}
	
	/**
	 * Part of the 'collect updates' subsystem.
	 * <p>
	 * 'Collects' an update -- if returned value is non-null, then it is reported
	 * and {@link #spiWriteLockUpdates_reset(boolean, Object, WBRBCacheEntry, WBRBCachePayload)}
	 * is invoked with 'false' (i.e. failure to collect an update leads to updates
	 * reset and no plans to collect further updates).
	 * <p>
	 * Default implementation checks whether updates collection is enabled (for
	 * disabled collection it does nothing, just returns null) and if enabled -- simply 
	 * adds them to updates list unless capacity is exceeded (in which case an
	 * exception is returned).
	 * 
	 * @return null if update has been collected and we can continue collecting
	 * 		updates; exception instance of update could not be collected for some
	 * 		reason -- such as limit on number of updates (further updates will 
	 * 		NOT be collected)
	 */
	@SuppressWarnings("unused")
	@Nullable
	protected CacheIllegalStateException spiWriteLockUpdates_collect(@Nonnull K key,
		WBRBCacheEntry cacheEntry,
		WBRBCachePayload payload, UInt update)
			throws InterruptedException
	{
		if (!payload.isCollectUpdates())
			return null;
		
		int limit = config.getMaxUpdatesToCollect();
		if (limit < 1)
			return new CacheElementHasTooManyUpdates(commonNamingPrefix, key, limit);
		
		List<UInt> collected = payload.getCollectedUpdates();
		if (collected == null)
		{
			collected = new ArrayList<>();
			payload.setCollectedUpdates(collected);
		}
		
		if (collected.size() >= limit)
			return new CacheElementHasTooManyUpdates(commonNamingPrefix, key, limit);
		
		collected.add(update);
		
		return null;
	}
	
	/**
	 * Returns list of collected updates so far (or null if none were collected).
	 * <p>
	 * This is used in {@link #spiWriteLockMergeCacheAndStorageData(Object, Object, WBRBCacheEntry, WBRBCachePayload)};
	 * if you need more customization on how updates are merged, override that method.
	 * <p>
	 * Default implementation simply returns {@link WBRBCachePayload#getCollectedUpdates()}
	 */
	@SuppressWarnings("unused")
	protected @Nullable List<UInt> spiWriteLockUpdates_getCollectedUpdates(@Nonnull K key,
		WBRBCacheEntry cacheEntry,
		WBRBCachePayload payload)
			throws InterruptedException
	{
		return payload.getCollectedUpdates();
	}
	
	/**
	 * Part of the 'collect updates' subsystem.
	 * <p>
	 * Determines whether a merge is possible for the given data.
	 * <p> 
	 * Default implementation simply returns {@link WBRBCachePayload#isCollectUpdates()}
	 * which is set according to the {@link #spiWriteLockUpdates_reset(boolean, Object, WBRBCacheEntry, WBRBCachePayload)}
	 * flag.
	 * 
	 * @param incomingStorageData non-empty if this is a decision for actual
	 * 		incoming data; empty if it is a decision for a future incoming data
	 * 		(such as when deciding whether it's reasonable to keep collecting
	 * 		updates)
	 */
	@SuppressWarnings("unused")
	protected boolean spiWriteLockUpdates_isMergePossible(@Nonnull K key,
		WBRBCacheEntry cacheEntry, WBRBCachePayload payload,
		NullableOptional<R> incomingStorageData)
			throws InterruptedException
	{
		return payload.isCollectUpdates();
	}
	
	/**
	 * Part of the 'collect updates' subsystem.
	 * <p>
	 * {@link #spiWriteLockUpdates_isMergePossible(Object, WBRBCacheEntry, WBRBCachePayload)}
	 * wrapped into error handler; logs message, returns false and resets 
	 * collected updates (with 'false' further collection flag) in case of an exception.
	 */
	protected boolean wrappedSpiWriteLockUpdates_isMergePossible(@Nonnull K key,
		WBRBCacheEntry cacheEntry, WBRBCachePayload payload,
		NullableOptional<R> incomingStorageData)
			throws InterruptedException
	{
		NullableOptional<@Nonnull Boolean> result = guardedInvocationNonNull(() -> spiWriteLockUpdates_isMergePossible(key, cacheEntry, payload, incomingStorageData), WBRBCacheMessage.SPI_EXCEPTION_Updates_isMergePossible, key);
		
		if (result.hasException())
		{
			wrappedSpiWriteLockUpdates_reset(WBRBUpdatesResetReason.IS_MERGE_POSSIBLE_EXCEPTION, false, key, cacheEntry, payload);
			return false;
		}
		
		return result.get();
	}
	
	/**
	 * Part of the 'collect updates' subsystem.
	 * <p>
	 * NOTE: this method is called when either holding the write lock or when
	 * it is otherwise thread-safe (such as when new cache entry instance is
	 * created and is not yet shared among threads).
	 * <p>
	 * Resets collected updates because the processing code believes they will
	 * no longer be needed (e.g. because they've been applied or there was some
	 * error that prevents their usefulness in the future).
	 * <p>
	 * Flag indicates whether subsystem should collect the following updates or
	 * not (e.g. if we expect valid resync, we should collect updates). 
	 * <p>
	 * Default implementation simply nulls collected updates list & sets 
	 * {@link WBRBCachePayload#setCollectUpdates(boolean)} with the given flag.
	 * 
	 * @param reason the reason the reset method was called; this lets custom
	 * 		implementations to make their own decisions as to whether to 
	 * 		continue collecting updates
	 * @param collectUpdatesAfter 'opinion' of the calling code as to whether 
	 * 		to collect updates after the reset; custom implementations may
	 * 		disregard this value if desired
	 */
	@SuppressWarnings("unused")
	protected void spiWriteLockUpdates_reset(
		WBRBUpdatesResetReason reason, boolean collectUpdatesAfter,
		@Nonnull K key,
		WBRBCacheEntry cacheEntry,
		WBRBCachePayload payload)
			throws InterruptedException
	{
		payload.setCollectedUpdates(null);
		payload.setCollectUpdates(collectUpdatesAfter);
	}

	/**
	 * {@link #spiWriteLockUpdates_reset(Object, WBRBCacheEntry, WBRBCachePayload)}
	 * wrapped in error handler.
	 */
	protected void wrappedSpiWriteLockUpdates_reset( 
		WBRBUpdatesResetReason reason, boolean collectUpdatesAfter,
		@Nonnull K key,
		WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		guardedInvocation(() -> spiWriteLockUpdates_reset(reason, collectUpdatesAfter, key, cacheEntry, payload), WBRBCacheMessage.SPI_EXCEPTION_Updates_reset, key);		
	}

	/**
	 * Invokes given code that produces result and returns {@link NullableOptional}
	 * with the result.
	 * <p>
	 * In case of an exception (except note below), returned {@link NullableOptional} has no value,
	 * but has an exception; also exceptionMessage plus actual exception plus 
	 * exceptionArgs are passed to {@link #logMessage(WBRBCacheMessage, Throwable, Object...)}
	 * <p>
	 * NOTE: {@link InterruptedException} and {@link ThreadDeath} subclasses
	 * are not logged and are re-thrown as those can be used to stop thread
	 */
	protected <RV> NullableOptional<RV> guardedInvocation(InterruptableSupplier<RV> callable, 
		WBRBCacheMessage exceptionMessage, Object... exceptionArgs) 
			throws InterruptedException
	{
		try
		{
			return NullableOptional.of(callable.get());
		} catch (Throwable e)
		{
			if (e instanceof ThreadDeath)
				throw e;
			if (e instanceof InterruptedException) // this may be used to indicate that thread should exit 
				throw e;
			
			logMessage(exceptionMessage, e, exceptionArgs);
			
			return NullableOptional.emptyWithException(e);
		}
	}
	
	
	/**
	 * Invokes given code that produces result and returns {@link NullableOptional}
	 * with the result.
	 * <p>
	 * If code being invoked returns NULL, then it is treated as NullPointerException
	 * within the code (and is therefore logged and the resulting {@link NullableOptional}
	 * is empty).
	 * <p>
	 * In case of an exception (except note below), returned {@link NullableOptional} has no value,
	 * but has an exception; also exceptionMessage plus actual exception plus 
	 * exceptionArgs are passed to {@link #logMessage(WBRBCacheMessage, Throwable, Object...)}
	 * <p>
	 * NOTE: {@link InterruptedException} and {@link ThreadDeath} subclasses
	 * are not logged and are re-thrown as those can be used to stop thread
	 */
	protected <@Nonnull RV> NullableOptional<RV> guardedInvocationNonNull(InterruptableSupplier<RV> callable, 
		WBRBCacheMessage exceptionMessage, Object... exceptionArgs) 
			throws InterruptedException
	{
		return TypeUtil.coerce(guardedInvocation(() -> nnChecked(callable.get()), exceptionMessage, exceptionArgs));
	}
	

	/** Used in {@link #guardedInvocation(InterruptableRunnable, WBRBCacheMessage, Object...)} */
	private static final Object someObject = new Object();
	
	/**
	 * Invokes given code that produces no result and returns {@link NullableOptional} --
	 * if there's no exception, this optional contains meaningless value.
	 * <p>
	 * In case of an exception (except note below), returned {@link NullableOptional} has no value,
	 * but has an exception; also exceptionMessage plus actual exception plus 
	 * exceptionArgs are passed to {@link #logMessage(WBRBCacheMessage, Throwable, Object...)}
	 * <p>
	 * NOTE: {@link InterruptedException} and {@link ThreadDeath} subclasses
	 * are not logged and are re-thrown as those can be used to stop thread
	 */
	protected NullableOptional<Object> guardedInvocation(InterruptableRunnable runnable,
		WBRBCacheMessage exceptionMessage, Object... exceptionArgs)
			throws InterruptedException
	{
		return guardedInvocation(() -> {runnable.run(); return someObject;}, exceptionMessage, exceptionArgs);
	}
	
	// CCC comment
	protected <RV> RV withWriteLock(WBRBCacheEntry cacheEntry, InterruptableSupplier<RV> callable)
		throws InterruptedException
	{
		return withLock(cacheEntry.getLock().writeLock(), cacheEntry, callable);
	}

	// CCC comment
	protected void withWriteLock(WBRBCacheEntry cacheEntry, InterruptableRunnable runnable)
		throws InterruptedException
	{
		withLock(cacheEntry.getLock().writeLock(), cacheEntry, runnable);
	}
	
	// CCC comment
	protected <RV> RV withReadLock(WBRBCacheEntry cacheEntry, InterruptableSupplier<RV> callable)
		throws InterruptedException
	{
		return withLock(cacheEntry.getLock().readLock(), cacheEntry, callable);
	}

	// CCC comment
	protected void withReadLock(WBRBCacheEntry cacheEntry, InterruptableRunnable runnable)
		throws InterruptedException
	{
		withLock(cacheEntry.getLock().readLock(), cacheEntry, runnable);
	}
	
	// CCC comment or remove
	/**
	 * @deprecated use {@link #withLock(Lock, WBRBCacheEntry, InterruptableSupplier)}
	 */
	@Deprecated
	protected <RV> RV withLock(final Lock lock, InterruptableSupplier<RV> callable)
		throws InterruptedException
	{
		lock.lockInterruptibly(); // need read lock before doing any changes
		try
		{
			return callable.get();
		} finally
		{
			lock.unlock();
		}
	}
	
	// CCC comment or remove
	/**
	 * @deprecated use {@link #withLock(Lock, WBRBCacheEntry, InterruptableRunnable)}
	 */
	@Deprecated
	protected void withLock(final Lock lock, InterruptableRunnable runnable)
		throws InterruptedException
	{
		withLock(lock, () -> {runnable.run(); return someObject;});
	}
	
	
	// CCC comment
	protected void withLock(final Lock lock, WBRBCacheEntry cacheEntry, InterruptableRunnable runnable)
		throws InterruptedException
	{
		withLock(lock, cacheEntry, () -> {runnable.run(); return someObject;});
	}
	
	
	// CCC comment
	// CCC note that these throw exceptions that happen inside
	protected <RV> RV withLock(final Lock lock, WBRBCacheEntry cacheEntry, InterruptableSupplier<RV> callable)
		throws InterruptedException
	{
		lock.lockInterruptibly(); // need read lock before doing any changes
		try
		{
			RV result = callable.get();
			
			if (lock instanceof WriteLock)
			{
				// TO-DO is this a proper way to do memory barrier?
				WBRBCachePayload payload = cacheEntry.getPayload();
				cacheEntry.setPayload(payload);
			}
			
			return result;
		} finally
		{
			lock.unlock();
		}
	}
	
	
	/**
	 * Creates write queue processor thread.
	 */
	protected ExitableThread createWriteQueueProcessor()
	{
		InterruptHandlingExitableThread thread = new InterruptHandlingExitableThread(threadGroup, commonNamingPrefix + " Write Queue Processor")
		{
			@Override
			protected void run1(boolean reentry)
				throws InterruptedException
			{
				runnableWriteQueueProcessor();
			}

			@Override
			protected boolean handleUnexpectedInterruptedException(
				InterruptedException e)
			{
				logMessage(WBRBCacheMessage.WRITE_QUEUE_PROCESSOR_UNEXPECTED_INTERRUPT, e);
				
				return true; // don't exit, restart processing
			}
			
		};
		
		thread.setPriority(config.getWriteQueueProcessingThreadPriority());
		
		return thread;
	}
	
	/**
	 * Code executed by {@link #writeQueueProcessingThread}
	 */
	protected void runnableWriteQueueProcessor() throws InterruptedException
	{
		WAThreadPoolExecutor pool = writeThreadPool;
		
		SimpleLongCounter batchCount = new SimpleLongCounter(0);
		while(true)
		{
			final WBRBWriteQueueEntry writeEntry;
			
			if ((batchCount.get() > 0) && (config.getWriteQueueBatchingDelay() > 0))
			{
				// Here we are during batch processing (i.e. some elements were already written)
				writeEntry = pollQueue(writeQueue, config.getWriteQueueBatchingDelay());
				if (writeEntry == null)
				{
					// batch is over.
					guardedInvocation(() -> spiNoLockWriteBatchDelayExpired(), WBRBCacheMessage.SPI_EXCEPTION_WriteBatchDelayExpired);
					batchCount.reset();
					continue;
				}
			}
			else
			{
				// no batching or zero elements in current batch
				batchCount.reset();
				writeEntry = writeQueue.take(); // wait indefinitely for the next one
			}
			
			getStats().storageWriteQueueProcessedItems.incrementAndGet();
			
			K key = writeEntry.getKey();
			
			BooleanObjectWrapper setFailedStatus = BooleanObjectWrapper.of(false);
			
			guardedInvocationNonNull(() -> spiNoLockMakeWriteQueueProcessingDecision(key, writeEntry), WBRBCacheMessage.SPI_EXCEPTION_MakeWriteQueueProcessingDecision, key)
				.ifPresentInterruptibly(decision -> {
					switch (decision)
					{
						case DO_NOTHING:
							break;
						case SET_FINAL_FAILED_WRITE_STATUS:
							setFailedStatus.setTrue();
							break;
						case WRITE:
							// ok, execute write
							getStats().storageWriteAttempts.incrementAndGet();
							if (pool == null)
							{
								// Execute in our own thread
								guardedInvocation(() -> spiNoLockProcessWriteToStorage(key, writeEntry), WBRBCacheMessage.SPI_EXCEPTION_WriteToStorage, key)
									.ifPresentInterruptibly(o -> {
										batchCount.incrementAndGet();
									})
									.ifExceptionInterruptibly(e -> {
										setFailedStatus.setTrue();
									});
							}
							else
							{
								// Execute in the pool.
								pool.waExecute(() -> {
									// can't use guarded invocation as those throw InterruptedException
									try
									{
										spiNoLockProcessWriteToStorage(key, writeEntry);
									} catch (Exception e)
									{
										// This shouldn't happen unless spi method is broken or execution thread is interrupted for w/e reason
										logMessage(WBRBCacheMessage.SPI_EXCEPTION_WriteToStorage, e, key);
										try
										{
											WBRBCacheEntry cacheEntry = writeEntry.getCacheEntry();
											withWriteLock(cacheEntry, () -> { // cache entry access needs lock
												WBRBCachePayload payload = cacheEntry.getPayload();
												payload.setWriteStatus(WBRBCacheEntryWriteStatus.WRITE_FAILED_FINAL);
												payload.setPreviousFailedWriteData(NullableOptional.of(writeEntry.getDataToWrite()));
											});
										} catch (InterruptedException e2)
										{
											// nothing else to do here.
										}
									}
								});
								batchCount.incrementAndGet();
							}
							break;
					}
					
					// Event notification
					wrappedSpiUnknownLock_Event(WBRBEvent.WRITE_QUEUE_ITEM_PROCESSED, key, null, null, null, decision, writeEntry);								
				})
				// Exception in spiNoLockMakeWriteQueueProcessingDecision
				.ifExceptionInterruptibly(e -> {
					setFailedStatus.setTrue();
				});
			
			if (setFailedStatus.isTrue())
			{
				// Need to get write lock on cache entry before can update that
				WBRBCacheEntry cacheEntry = writeEntry.getCacheEntry();
				withWriteLock(cacheEntry, () -> {
					WBRBCachePayload payload = cacheEntry.getPayload();
					payload.setWriteStatus(WBRBCacheEntryWriteStatus.WRITE_FAILED_FINAL);
					payload.setPreviousFailedWriteData(NullableOptional.of(writeEntry.getDataToWrite()));
				});
			}
		}
	}

	
	/**
	 * This is invoked to 'finish' a write batch when there were some writes and 
	 * no additional writes queued for config.writeQueueBatchingDelay time.
	 * <p>
	 * Default implementation does nothing.
	 * <p>
	 * This is a useful method if you're implementing batched writes.
	 * <p>
	 * WARNING: this is ALWAYS executed in write queue processor thread, so it
	 * must return VERY QUICKLY.
	 */
	@SuppressWarnings("unused")
	protected void spiNoLockWriteBatchDelayExpired()
		throws InterruptedException
	{
		// nothing in default implementation
	}

	
	/**
	 * Must make a decision as to what write queue processor should do with
	 * the given item.
	 * <p>
	 * Default implementation always responds with {@link WBRBWriteQueueProcessingDecision#WRITE}
	 */
	@SuppressWarnings("unused")
	protected WBRBWriteQueueProcessingDecision spiNoLockMakeWriteQueueProcessingDecision(
		K key, WBRBWriteQueueEntry writeEntry) 
			throws InterruptedException
	{
		return WBRBWriteQueueProcessingDecision.WRITE;
	}
	
	
	/**
	 * Should implement write to the storage based on the specified key & data.
	 * <p>
	 * These are typically called in the separate write threads, but it is still
	 * very important to produce results very quickly as updates to cached objects
	 * are collected while they wait for the background read & writes (and thus memory
	 * can be consumed significantly).
	 * <p>
	 * If your storage works better with batched writes, this can be implemented
	 * by overriding {@link #spiNoLockProcessWriteToStorage(Object, WBRBWriteQueueEntry)}
	 * method (which allows to produce result later instead of 'right now') and
	 * {@link #spiNoLockWriteBatchDelayExpired()} to detect 'batch end' events.
	 * <p>
	 * For batched writes you also might want to disable writeThreadPool so that
	 * these methods are not called in separate threads unnecessarily.
	 * 
	 * @param key key to write
	 * @param dataToWrite data to be written
	 */
	protected abstract void writeToStorage(K key, W dataToWrite)
		throws InterruptedException;

	
	/**
	 * Extension point for subclasses that need more access to the internals
	 * than {@link #writeToStorage(Object, Object)} but at the same time do
	 * not want to deal with re-doing logic in {@link #spiNoLockProcessWriteToStorage(Object, WBRBWriteQueueEntry)}
	 */
	protected void spiNoLockWriteToStorage(K key, WBRBWriteQueueEntry writeEntry) 
			throws InterruptedException
	{
		writeToStorage(key, writeEntry.getDataToWrite());
	}
	
	/**
	 * Writes data to the underlying storage; does not need to be synchronous,
	 * completion is indicated at some point by invoking either of {@link #apiStorageWriteSuccess(WBRBWriteQueueEntry)}
	 * or {@link #apiStorageWriteFail(Throwable, WBRBWriteQueueEntry)}
	 * <p>
	 * Default implementation is synchronous -- it invokes {@link #writeToStorage(Object, Object)}
	 * and records the success result or failure (exception in {@link #writeToStorage(Object, Object)} 
	 * indicates failed write).
	 */
	protected void spiNoLockProcessWriteToStorage(K key, WBRBWriteQueueEntry writeEntry) 
			throws InterruptedException
	{
		guardedInvocation(() -> spiNoLockWriteToStorage(key, writeEntry) , WBRBCacheMessage.STORAGE_WRITE_FAIL, key)
			.ifPresentInterruptibly(o -> apiStorageWriteSuccess(writeEntry))
			.ifExceptionInterruptibly(e -> apiStorageWriteFail(e, writeEntry));
	}
	
	
	/**
	 * Notifies system about successful write.
	 * <p>
	 * Default implementation basically just checks that the cache entry
	 * status is as expected and sets write status to 'success'; if status isn't
	 * as expected, error message is produced and status is not updated. 
	 */
	protected void apiStorageWriteSuccess(WBRBWriteQueueEntry writeEntry)
		throws InterruptedException
	{
		getStats().storageWriteSuccesses.incrementAndGet();
		
		K key = writeEntry.getKey();
		WBRBCacheEntry cacheEntry = writeEntry.getCacheEntry();
		
		withWriteLock(cacheEntry, () -> {
			WBRBCachePayload payload = cacheEntry.getPayload();
			
			switch (payload.getWriteStatus())
			{
				case NO_WRITE_REQUESTED_YET:
				case WRITE_FAILED_FINAL:
				case WRITE_SUCCESS:
					logMessage(WBRBCacheMessage.UNEXPECTED_CACHE_STATE_FOR_WRITE_SUCCESS, null, key, payload.getWriteStatus());
					return; // Not sure what else to do in this case
				case REMOVED_FROM_CACHE:
					return; // too late now
				case WRITE_PENDING:
					break; // normal processing
			}
			
			cacheEntry.getPayload().setWriteStatus(WBRBCacheEntryWriteStatus.WRITE_SUCCESS);
			
			// Event notification
			wrappedSpiUnknownLock_Event(WBRBEvent.WRITE_SUCCESS, key, cacheEntry, payload, null, writeEntry);								
		});
	}
	
	// CCC comment
	/**
	 * WARNING: will NOT log error/exception information if going for retry
	 * because normally it is already logged in {@link #spiNoLockProcessWriteToStorage(Object, WBRBWriteQueueEntry)}
	 * If you're doing your own implementation, make sure to log the problem as needed.
	 * 
	 */
	protected void apiStorageWriteFail(@Nullable Throwable exception, WBRBWriteQueueEntry writeEntry)
		throws InterruptedException
	{
		getStats().storageWriteFailures.incrementAndGet();
		
		WBRBCacheEntry cacheEntry = writeEntry.getCacheEntry();
		withWriteLock(cacheEntry, () -> {
			WBRBCachePayload payload = cacheEntry.getPayload();
			
			K key = writeEntry.getKey();
			
			payload.getWriteFailureCount().incrementAndGet(); // increment fail counter
			
			BooleanObjectWrapper setFinalWriteFailedStatus = BooleanObjectWrapper.of(false);
			guardedInvocationNonNull(() -> spiWriteLockMakeWriteRetryDecision(exception, key, writeEntry, cacheEntry, payload), WBRBCacheMessage.SPI_EXCEPTION_MakeWriteRetryDecision, key)
				.ifPresentInterruptibly(decision -> {
					switch(decision)
					{
						case DO_NOTHING:
							break;
						case REMOVE_FROM_CACHE:
							haveWriteLock_RemoveFromCache(cacheEntry);
							break;
						case NO_RETRY_SET_FINAL_FAILED_STATUS:
							setFinalWriteFailedStatus.setTrue();
							logMessage(WBRBCacheMessage.STORAGE_WRITE_FAIL_FINAL, exception, key);
							break;
						case RETRY:
							// Need to re-queue write.
							writeQueue.add(writeEntry);
							logMessage(WBRBCacheMessage.STORAGE_WRITE_RETRY_ISSUED, exception, key);
							break;
					}
					
					// Event notification
					wrappedSpiUnknownLock_Event(WBRBEvent.WRITE_FAIL_DECISION, key, cacheEntry, payload, null, decision, writeEntry);								
				})
				.ifExceptionInterruptibly(e -> {
					setFinalWriteFailedStatus.setTrue();
				});
			
			if (setFinalWriteFailedStatus.isTrue())
			{
				payload.setWriteStatus(WBRBCacheEntryWriteStatus.WRITE_FAILED_FINAL);
				payload.setPreviousFailedWriteData(NullableOptional.of(writeEntry.getDataToWrite()));
			}
		});
	}
	
	/**
	 * For an incoming FAILED storage write must make a decision whether to
	 * go for a write retry ot not.
	 * <p>
	 * Default implementation first check that payload status is 'as expected'
	 * (if state is not expected, message is logged and {@link WBRBRetryDecision#DO_NOTHING}
	 * is returned);
	 * <p>
	 * If state is expeted, then decision depends on the already-failed write
	 * count and {@link WBRBConfig#getWriteFailureMaxRetryCount()}
	 * <p>
	 * Executed under write lock, so must finish ASAP.
	 */
	@SuppressWarnings("unused")
	protected WBRBRetryDecision spiWriteLockMakeWriteRetryDecision(
		@Nullable Throwable exception, K key,
		WBRBWriteQueueEntry writeEntry,
		WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		switch (payload.getWriteStatus())
		{
			case NO_WRITE_REQUESTED_YET:
			case WRITE_FAILED_FINAL:
			case WRITE_SUCCESS:
				logMessage(WBRBCacheMessage.UNEXPECTED_CACHE_STATE_FOR_WRITE_FAIL, null, key, payload.getWriteStatus());
				return WBRBRetryDecision.DO_NOTHING; // Not sure what else to do in this case
			case REMOVED_FROM_CACHE:
				return WBRBRetryDecision.DO_NOTHING; // too late now
			case WRITE_PENDING:
				// normal processing
				if (payload.getWriteFailureCount().get() > config.getWriteFailureMaxRetryCount())
					return WBRBRetryDecision.NO_RETRY_SET_FINAL_FAILED_STATUS;
				else
					return WBRBRetryDecision.RETRY;
		}
		
		logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), "code should not be reachable");
		return WBRBRetryDecision.DO_NOTHING;
	}
	
	
	/**
	 * Creates main queue processor thread.
	 */
	protected ExitableThread createMainQueueProcessor()
	{
		InterruptHandlingExitableThread thread = new InterruptHandlingExitableThread(threadGroup, commonNamingPrefix + " Main Queue Processor")
		{
			@Override
			protected void run1(boolean reentry)
				throws InterruptedException
			{
				runnableMainQueueProcessor();
			}

			@Override
			protected boolean handleUnexpectedInterruptedException(
				InterruptedException e)
			{
				logMessage(WBRBCacheMessage.MAIN_QUEUE_PROCESSOR_UNEXPECTED_INTERRUPT, e);
				
				return true; // don't exit, restart processing
			}
			
		};
		
		thread.setPriority(config.getMainQueueProcessingThreadPriority());
		
		return thread;
	}

	/**
	 * Value used for 'expire from cache' state (i.e. non-error unlike {@link #REMOVE_FROM_CACHE_MAIN_QUEUE_DECISION}).
	 */
	private final WBRBMainQueueProcessingDecision EXPIRE_FROM_CACHE_MAIN_QUEUE_DECISION =
		new WBRBMainQueueProcessingDecision(WBRBMainQueueItemCacheRetainDecision.EXPIRE_FROM_CACHE, 
			new WBRBWriteSplitWithFlags(
				new WriteSplit(TypeUtil.coerce(someObject)/*use some random object because it is not used anyway*/), 
				false/*does NOT contain all updates*/)); 

	/**
	 * Value used for 'remove from cache' state (often used for e.g. errors).
	 */
	private final WBRBMainQueueProcessingDecision REMOVE_FROM_CACHE_MAIN_QUEUE_DECISION =
		new WBRBMainQueueProcessingDecision(WBRBMainQueueItemCacheRetainDecision.REMOVE_FROM_CACHE, 
			new WBRBWriteSplitWithFlags(
				new WriteSplit(TypeUtil.coerce(someObject)/*use some random object because it is not used anyway*/), 
				false/*does NOT contain all updates*/)); 
	
	/**
	 * Code executed by {@link #mainQueueProcessingThread}
	 */
	protected void runnableMainQueueProcessor() throws InterruptedException
	{
		WBRBCacheEntry currentCacheEntry = null;
		try
		{
			SimpleLongCounter sleepDelayInsteadOfProcessing = new SimpleLongCounter(-1);
			while(true)
			{
				if (currentCacheEntry == null) // We could have element that hasn't finished processing yet
					currentCacheEntry = mainQueue.take(); // get next element
				
				// This is set to positive value in order to delay processing of the current element.
				sleepDelayInsteadOfProcessing.set(-1); // reset sleep flag
				WBRBCacheEntry cacheEntry = currentCacheEntry; // process this, don't touch 'current' variable unnecessarily
				
				withWriteLock(cacheEntry, () -> {				
					K key = cacheEntry.getKey();
					WBRBCachePayload payload = cacheEntry.getPayload();

					// Check if we should process or sleep.
					final long now = timeNow();
					long sleepFor = -1;
					{
						long cacheUntil = timeAddVirtualIntervalToRealWorldTime(
							payload.getInQueueSince(), config.getMainQueueCacheTime());
						
						if (isFlushing()) 
						{
							// during shutdown we go into 'process immediately' mode
							// however if there's read or write operation executing
							// currently -- we still wait, there's no reason not to wait for it
							boolean skipWait = true;
							switch (payload.getReadStatus())
							{
								case DATA_READY:
								case DATA_READY_RESYNC_FAILED_FINAL:
								case READ_FAILED_FINAL:
								case REMOVED_FROM_CACHE:
									break;
								case DATA_READY_RESYNC_PENDING:
								case NOT_READ_YET:
									skipWait = false;
									break;
							}
							switch (payload.getWriteStatus())
							{
								case NO_WRITE_REQUESTED_YET:
								case REMOVED_FROM_CACHE:
								case WRITE_FAILED_FINAL:
								case WRITE_SUCCESS:
									break;
								case WRITE_PENDING:
									skipWait = false;
									break;
							}
							
							if (skipWait)
								cacheUntil = 0;
						}
						
						if (cacheUntil > now)
						{
							// maybe need to wait
							boolean haveToWait = true;
							// maybe we don't need to wait due to too many items in the queue
							if (mainQueue.size() > config.getMainQueueMaxTargetSize())
							{
								long minCacheUntil = timeAddVirtualIntervalToRealWorldTime(
									payload.getInQueueSince(), config.getMainQueueCacheTimeMin());
								if (minCacheUntil > now)
								{
									// have to wait due to minimum cache time restriction
								}
								else
								{
									// no waiting, too many cache elements and item is over minimum cache time
									haveToWait = false;
								}
							}
							
							if (haveToWait)
							{
								long maxWaitUntil = timeAddVirtualIntervalToRealWorldTime(
									now, config.getMaxSleepTime()); // never wait more than max sleep time at a time to avoid issues due to shutdown or changing time factor
								
								long waitUntil = Math.min(maxWaitUntil, cacheUntil);
								
								sleepFor = waitUntil - now;
							}
						}
					}
					
					if (sleepFor > 0)
					{
						// Sleep instead of processing.
						sleepDelayInsteadOfProcessing.set(sleepFor);
					}
					else
					{
						// Process item.
						getStats().mainQueueProcessedItems.incrementAndGet();
						getStats().mainQueueLastItemInQueueDurationMs.set(
							timeGapVirtual(payload.getInQueueSince(), now)
						);
						
						// FIX-ME probably needs early bail out if item is already marked as REMOVE_FROM_CACHE because we don't want to try removing something that already WAS removed
						// FIX-ME redo triplet into real class with null-checking via Lombok
						NullableOptional<WBRBMainQueueProcessingDecision> result = guardedInvocationNonNull(
							() -> spiWriteLockMakeMainQueueProcessingDecision(key, cacheEntry, payload), WBRBCacheMessage.SPI_EXCEPTION_MakeMainQueueProcessingDecision, key);
						
						// Handle exception
						WBRBMainQueueProcessingDecision decision = 
							result.hasException() ?
								REMOVE_FROM_CACHE_MAIN_QUEUE_DECISION
								: result.get();
						
						// Execute decision
						boolean mayWrite = true;
						boolean resetFailureCounts = true;
						boolean logNonStandardOutcome = false;
						WBRBMainQueueItemCacheRetainDecision decisionOutcome = decision.getDecision();
						switch(decisionOutcome)
						{
							case EXPIRE_FROM_CACHE:
								haveWriteLock_RemoveFromCache_NoMessageIfAlreadyRemoved(cacheEntry);
								getStats().mainQueueExpiredFromCacheCount.incrementAndGet();
								break;
							
							case REMOVE_FROM_CACHE:
								haveWriteLock_RemoveFromCache_NoMessageIfAlreadyRemoved(cacheEntry);
								resetFailureCounts = false; // 'expire' is an 'ok' option
								logNonStandardOutcome = true; // for logging non-standard outcomes
								getStats().mainQueueRemovedFromCacheCount.incrementAndGet();
								break;
								
							case MAIN_QUEUE:
								payload.setInQueueSince(timeNow());
								mainQueue.add(cacheEntry); // re-queue to main queue
								resetFailureCounts = false;
								logNonStandardOutcome = true; // for logging non-standard outcomes
								getStats().mainQueueRequeueToMainQueueCount.incrementAndGet();
								break;
								
							case RETURN_QUEUE_NO_WRITE: // e.g. resync failed (but still have collected updates) or previous write didn't complete yet, so a delaying tactic
								mayWrite = false;
								//$FALL-THROUGH$
							case RETURN_QUEUE_KEEP_FULL_CYCLE_FAILURE_COUNT: // e.g. prev write completely failed, try again
								resetFailureCounts = false;
								//$FALL-THROUGH$
							case RETURN_QUEUE:
								WBRBWriteSplitWithFlags writeSplitWithFlags = decision.getWriteSplitWithFlags();
								WriteSplit writeSplit = writeSplitWithFlags.getWriteSplit();
								
								if (mayWrite)
								{
									if (writeSplitWithFlags.isWriteDataContainsAllPendingUpdates())
									{
										// only if there are no other pending updates -- 
										// reset 'dirty' flag by forcing timestamp to non-positive value
										payload.setLastWriteTimestamp(-Math.abs(payload.getLastWriteTimestamp()));
									}
									payload.getWriteFailureCount().reset(); // new write, so reset counter
									
									NullableOptional<W> writeData = writeSplit.getWriteData();
									if (writeData.isPresent())
									{
										// Queue actual write
										writeQueue.add(new WBRBWriteQueueEntry(key, cacheEntry, writeData.get()));
										payload.setWriteStatus(WBRBCacheEntryWriteStatus.WRITE_PENDING);
										getStats().mainQueueSentWrites.incrementAndGet();
									}
									else
									{
										// Write is allowed, but there's nothing to write.
										payload.setWriteStatus(WBRBCacheEntryWriteStatus.NO_WRITE_REQUESTED_YET);
									}
									
									if (writeSplitWithFlags.isWriteDataContainsAllPendingUpdates())
									{
										// If all pending updates sent to write -- restart collecting updates
										wrappedSpiWriteLockUpdates_reset(WBRBUpdatesResetReason.FULL_WRITE_SENT, true, key, cacheEntry, payload);
									}
								}
								
								// Overwrite existing cache value with whatever was the result of the decision.
								payload.setValue(writeSplit.getNewCacheData());
								
								payload.setInQueueSince(timeNow());
								returnQueue.add(cacheEntry);
								
								if (decisionOutcome != WBRBMainQueueItemCacheRetainDecision.RETURN_QUEUE)
									logNonStandardOutcome = true; // for logging non-standard outcomes
								break;
						}
						
						// Give implementations chance to override resetFailureCount value
						{
							final boolean isResetFailureCounts = resetFailureCounts;
							final BooleanObjectWrapper invResult = BooleanObjectWrapper.of(false);
							guardedInvocation(() -> spiWriteLockMakeMainQueueProcessingDecision_isResetFailureCounts(isResetFailureCounts, decisionOutcome, key, cacheEntry, payload), WBRBCacheMessage.SPI_EXCEPTION_MakeMainQueueProcessingDecision_isResetFailureCounts, key)
								.ifPresentInterruptibly(r -> invResult.set(nn(r))); // nn() seems to be the easiest way to convince compiler it cannot be null
							resetFailureCounts = invResult.isTrue();
						}
						
						long lastReadTimestamp = Math.abs(payload.getLastReadTimestamp());
						if (resetFailureCounts)
						{
							// force read-timestamp to non-positive value in order
							// to keep track if there are any new reads since 'now'
							// (new reads will have positive timestamp)
							payload.setLastReadTimestamp(-lastReadTimestamp);
							payload.getFullCacheCycleFailureCount().reset();
						}
						else
						{
							// make sure item goes through the full cycle 
							// (set positive 'last read timestamp' so it 
							// shouldn't be removed by the return queue processor)
							payload.setLastReadTimestamp( lastReadTimestamp > 0 ? lastReadTimestamp : timeNow());
							payload.getFullCacheCycleFailureCount().incrementAndGet();
							getStats().mainQueueNotAllOkCount.incrementAndGet();
						}
						
						// Log non-standard outcome (flag controls whether default implementation will actually log
						{
							final boolean doLog = logNonStandardOutcome;
							guardedInvocation(() -> spiWriteLockMakeMainQueueProcessingDecision_logNonStandardOutcome(doLog, decisionOutcome, key, cacheEntry, payload), WBRBCacheMessage.SPI_EXCEPTION_MakeMainQueueProcessingDecision_logNonStandardOutcome, key);
						}
						
						// Notification event
						wrappedSpiUnknownLock_Event(WBRBEvent.MAIN_QUEUE_ITEM_PROCESSED, key, cacheEntry, payload, null, decision);
						
					} // end of sleep or processing IF
					
				}); // end of write lock
				
				{
					long sleep = sleepDelayInsteadOfProcessing.get();
					if (sleep > 0)
						Thread.sleep(sleep); // Delay and iterate the same element again
					else
						currentCacheEntry = null; // done with the current, may process the next entry
				}
				
			} // end infinite while() loop
			
		} finally
		{
			// so the fallback position here is that if there's current
			// currentCacheEntry value then it was removed from main processing queues
			// and was not yet re-added to a processing queue or removed from
			// in-flight map
			// Therefore to prevent in-flight map retaining orphan entries we
			// must remove it from the map here
			if (currentCacheEntry != null)
			{
				haveNoLock_RemoveFromCache(currentCacheEntry);
				logMessage(WBRBCacheMessage.UNEXPECTED_CACHE_REMOVAL_IN_MAIN_QUEUE_PROCESSING, null, currentCacheEntry.getKey());
			}
		}
	}
	
	/**
	 * CCC comment
	 */
	@SuppressWarnings("unused")
	protected void spiWriteLockEvent_MainQueueProcessed(@Nonnull K key,
		WBRBMainQueueProcessingDecision decision,
		WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		// default does nothing
	}

	// CCC comment
	protected WBRBMainQueueProcessingDecision spiWriteLockMakeMainQueueProcessingDecision(
		K key, WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		S cacheData = payload.getValue();
		
		WBRBCacheEntryReadStatus readStatus = payload.getReadStatus();
		
		ObjectWrapper<WBRBMainQueueItemCacheRetainDecision> readStatusDecision = ObjectWrapper.of(null);  
		switch (readStatus)
		{
			case READ_FAILED_FINAL:
			case NOT_READ_YET: // if initial read hasn't yet completed, there's no point keeping it around
				readStatusDecision.set(WBRBMainQueueItemCacheRetainDecision.REMOVE_FROM_CACHE);
				break;

			case REMOVED_FROM_CACHE:
				// If element is marked as removed, assume information was logged before, so do the 'normal' removal
				readStatusDecision.set(WBRBMainQueueItemCacheRetainDecision.EXPIRE_FROM_CACHE);
				break;
				
			case DATA_READY_RESYNC_FAILED_FINAL:
				guardedInvocationNonNull(() -> spiWriteLockMakeMainQueueProcessingDecision_ResyncFailedFinal(key, cacheEntry, payload), WBRBCacheMessage.SPI_EXCEPTION_MakeMainQueueProcessingDecision_ResyncFailedFinal, key)
					.ifPresentInterruptibly(decision -> {
						readStatusDecision.set(decision);
					})
					.ifExceptionInterruptibly(e -> {
						readStatusDecision.set(WBRBMainQueueItemCacheRetainDecision.REMOVE_FROM_CACHE);
					});
				break;
			
			case DATA_READY_RESYNC_PENDING:
				guardedInvocationNonNull(() -> spiWriteLockMakeMainQueueProcessingDecision_ResyncPending(key, cacheEntry, payload), WBRBCacheMessage.SPI_EXCEPTION_MakeMainQueueProcessingDecision_ResyncPending, key)
					.ifPresentInterruptibly(decision -> {
						readStatusDecision.set(decision);
					})
					.ifExceptionInterruptibly(e -> {
						readStatusDecision.set(WBRBMainQueueItemCacheRetainDecision.REMOVE_FROM_CACHE);
					});
				break;
				
			case DATA_READY:
				readStatusDecision.set(WBRBMainQueueItemCacheRetainDecision.RETURN_QUEUE);
				break;
				
		}

		WBRBCacheEntryWriteStatus writeStatus = payload.getWriteStatus();
		ObjectWrapper<WBRBMainQueueItemCacheRetainDecision> writeStatusDecision = ObjectWrapper.of(null);
		switch (writeStatus)
		{

			case REMOVED_FROM_CACHE:
				// If element is marked as removed, assume information was logged before, so do the 'normal' removal
				writeStatusDecision.set(WBRBMainQueueItemCacheRetainDecision.EXPIRE_FROM_CACHE);
				break;
			
			case WRITE_FAILED_FINAL:
				guardedInvocationNonNull(() -> spiWriteLockMakeMainQueueProcessingDecision_WriteFailedFinal(key, cacheEntry, payload), WBRBCacheMessage.SPI_EXCEPTION_MakeMainQueueProcessingDecision_WriteFailedFinal, key)
					.ifPresentInterruptibly(decision -> {
						writeStatusDecision.set(decision);
					})
					.ifExceptionInterruptibly(e -> {
						writeStatusDecision.set(WBRBMainQueueItemCacheRetainDecision.REMOVE_FROM_CACHE);
					});
				break;
			
			case WRITE_PENDING:
				guardedInvocationNonNull(() -> spiWriteLockMakeMainQueueProcessingDecision_WritePending(key, cacheEntry, payload), WBRBCacheMessage.SPI_EXCEPTION_MakeMainQueueProcessingDecision_WritePending, key)
					.ifPresentInterruptibly(decision -> {
						writeStatusDecision.set(decision);
					})
					.ifExceptionInterruptibly(e -> {
						writeStatusDecision.set(WBRBMainQueueItemCacheRetainDecision.REMOVE_FROM_CACHE);
					});
				break;
			
			case WRITE_SUCCESS:
			case NO_WRITE_REQUESTED_YET:
				writeStatusDecision.set(WBRBMainQueueItemCacheRetainDecision.RETURN_QUEUE);
				
		}
		
		// Figure out the 'worst' status for the final decision.
		WBRBMainQueueItemCacheRetainDecision finalDecision = readStatusDecision.get();
		{
			WBRBMainQueueItemCacheRetainDecision otherDecision = writeStatusDecision.get();
			
			if ((finalDecision == null) || (otherDecision == null))
			{
				logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), "code should not be reachable, finalDecision: " + finalDecision);
				finalDecision = WBRBMainQueueItemCacheRetainDecision.REMOVE_FROM_CACHE;
			}
			else
			{
				if (otherDecision.getFailureRating() > finalDecision.getFailureRating())
					finalDecision = otherDecision;
			}
		}
		
		switch (finalDecision)
		{
			case EXPIRE_FROM_CACHE:
				// can't create WriteSplit over null value (since element might have not been read yet), so use prefab
				return EXPIRE_FROM_CACHE_MAIN_QUEUE_DECISION; 
			case REMOVE_FROM_CACHE:
				// can't create WriteSplit over null value (since element might have not been read yet), so use prefab
				return REMOVE_FROM_CACHE_MAIN_QUEUE_DECISION; 
			case MAIN_QUEUE:
			case RETURN_QUEUE_NO_WRITE:
				return new WBRBMainQueueProcessingDecision(finalDecision, new WBRBWriteSplitWithFlags(new WriteSplit(cacheData), false/*does NOT contain all updates*/));
			case RETURN_QUEUE_KEEP_FULL_CYCLE_FAILURE_COUNT:
			case RETURN_QUEUE:
				NullableOptional<@Nonnull WBRBWriteSplitWithFlags> result = guardedInvocationNonNull(
					() -> spiWriteLockProcessSplitForWrite(key, cacheData, cacheEntry, payload), WBRBCacheMessage.SPLIT_FOR_WRITE_FAIL, key);
				
				if (result.hasException())
					return REMOVE_FROM_CACHE_MAIN_QUEUE_DECISION;
				
				return new WBRBMainQueueProcessingDecision(finalDecision, result.get()); 
		}
		
		logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), "code should not be reachable");
		return REMOVE_FROM_CACHE_MAIN_QUEUE_DECISION;
	}
	
	// CCC comment
	protected WBRBMainQueueItemCacheRetainDecision spiWriteLockMakeMainQueueProcessingDecision_ResyncFailedFinal(
		K key, WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		// use >= for cycle count check because increment happens after this check
		if ((!wrappedSpiWriteLockUpdates_isMergePossible(key, cacheEntry, payload, NullableOptional.empty())) 
			|| (payload.getFullCacheCycleFailureCount().get() >= config.getFullCacheCycleFailureMaxRetryCount()))
		{
			// Either merge is not possible (e.g. if updates are not collected) or too many failures -- 
			// either way no point trying to reschedule this for resync
			
			// If we are allowed to write out data without resync -- do so
			if (config.isAllowDataWritingAfterResyncFailedFinal())
			{
				logMessage(WBRBCacheMessage.RESYNC_FAILED_FINAL_STORAGE_DATA_OVERWRITE, null, key);
				return WBRBMainQueueItemCacheRetainDecision.RETURN_QUEUE;
			}
			
			// Not allowed to write out without resync -- fail and remove from cache 
			logMessage(WBRBCacheMessage.RESYNC_FAILED_FINAL_DATA_DISCARDED, null, key);
			return WBRBMainQueueItemCacheRetainDecision.REMOVE_FROM_CACHE;
		}
		
		return WBRBMainQueueItemCacheRetainDecision.RETURN_QUEUE_NO_WRITE;
	}
	
	// CCC comment
	protected WBRBMainQueueItemCacheRetainDecision spiWriteLockMakeMainQueueProcessingDecision_ResyncPending(
		K key, WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		// use >= for cycle count check because increment happens after this check
		if ((!wrappedSpiWriteLockUpdates_isMergePossible(key, cacheEntry, payload, NullableOptional.empty())) 
			|| (payload.getFullCacheCycleFailureCount().get() >= config.getFullCacheCycleFailureMaxRetryCount()))
		{
			// Either merge is not possible (e.g. if updates are not collected) or too many failures -- 
			// either way no point trying to reschedule this for resync
			
			// If we are allowed to write out data without resync -- do so
			if (config.isAllowDataWritingAfterResyncFailedFinal())
			{
				logMessage(WBRBCacheMessage.RESYNC_FAILED_FINAL_STORAGE_DATA_OVERWRITE, null, key);
				return WBRBMainQueueItemCacheRetainDecision.RETURN_QUEUE;
			}
			
			// Not allowed to write out without resync -- fail and remove from cache 
			logMessage(WBRBCacheMessage.RESYNC_FAILED_FINAL_DATA_DISCARDED, null, key);
			return WBRBMainQueueItemCacheRetainDecision.REMOVE_FROM_CACHE;
		}
		
		return WBRBMainQueueItemCacheRetainDecision.MAIN_QUEUE; // resync still pending, so just send back to main queue
	}
	
	
	// CCC comment
	@SuppressWarnings("unused")
	protected WBRBMainQueueItemCacheRetainDecision spiWriteLockMakeMainQueueProcessingDecision_WriteFailedFinal(
		K key, WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		// use >= because increment happens after this check
		if (payload.getFullCacheCycleFailureCount().get() >= config.getFullCacheCycleFailureMaxRetryCount())
		{
			// Too many full-cycle failures, give up
			logMessage(WBRBCacheMessage.WRITE_FAILED_FINAL_DATA_DISCARDED, null, key);
			return WBRBMainQueueItemCacheRetainDecision.REMOVE_FROM_CACHE;
		}
		
		return WBRBMainQueueItemCacheRetainDecision.RETURN_QUEUE_KEEP_FULL_CYCLE_FAILURE_COUNT;
	}
	
	// CCC comment
	@SuppressWarnings("unused")
	protected WBRBMainQueueItemCacheRetainDecision spiWriteLockMakeMainQueueProcessingDecision_WritePending(
		K key, WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		// use >= because increment happens after this check
		if (payload.getFullCacheCycleFailureCount().get() >= config.getFullCacheCycleFailureMaxRetryCount())
		{
			// Too many full-cycle failures, give up
			logMessage(WBRBCacheMessage.WRITE_FAILED_FINAL_DATA_DISCARDED, null, key);
			return WBRBMainQueueItemCacheRetainDecision.REMOVE_FROM_CACHE;
		}
		
		return WBRBMainQueueItemCacheRetainDecision.RETURN_QUEUE_NO_WRITE;
	}
	
	/**
	 * Allows implementations to override decision on whether main queue
	 * processing code will reset accumulated failures counts after item processing.
	 * <p>
	 * Default implementation simply returns isResetFailureCounts
	 * 
	 * @param isResetFailureCounts 'opinion' of the main queue processor as
	 * 		to whether failure counts should be reset
	 * @param decisionOutcome main queue processing decision that led to this
	 * 		invocation
	 */
	@SuppressWarnings("unused")
	protected boolean spiWriteLockMakeMainQueueProcessingDecision_isResetFailureCounts(
		boolean isResetFailureCounts,
		WBRBMainQueueItemCacheRetainDecision decisionOutcome,
		K key, WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		return isResetFailureCounts;
	}
	
	/**
	 * Allows implementations to override decision/logging on whether main queue
	 * processing code should report a non-standard outcome message
	 * <p>
	 * Default implementation log non-standard outcome message if doLog is true
	 * 
	 * @param doLog 'opinion' of the main queue processor as
	 * 		to whether message should be logged
	 * @param decisionOutcome main queue processing decision that led to this
	 * 		invocation
	 */
	@SuppressWarnings("unused")
	protected void spiWriteLockMakeMainQueueProcessingDecision_logNonStandardOutcome(
		boolean doLog,
		WBRBMainQueueItemCacheRetainDecision decisionOutcome,
		K key, WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		if (doLog)
			logMessage(WBRBCacheMessage.MAIN_QUEUE_NON_STANDARD_OUTCOME, null, key, decisionOutcome);		
	}
	
	/**
	 * Creates return queue processor thread.
	 */
	protected ExitableThread createReturnQueueProcessor()
	{
		InterruptHandlingExitableThread thread = new InterruptHandlingExitableThread(threadGroup, commonNamingPrefix + " Return Queue Processor")
		{
			@Override
			protected void run1(boolean reentry)
				throws InterruptedException
			{
				runnableReturnQueueProcessor();
			}

			@Override
			protected boolean handleUnexpectedInterruptedException(
				InterruptedException e)
			{
				logMessage(WBRBCacheMessage.RETURN_QUEUE_PROCESSOR_UNEXPECTED_INTERRUPT, e);
				
				return true; // don't exit, restart processing
			}
			
		};
		
		thread.setPriority(config.getReturnQueueProcessingThreadPriority());
		
		return thread;
	}
	
	/**
	 * Code executed by {@link #returnQueueProcessingThread}
	 */
	protected void runnableReturnQueueProcessor() throws InterruptedException
	{
		WBRBCacheEntry currentCacheEntry = null;
		try
		{
			SimpleLongCounter sleepDelayInsteadOfProcessing = new SimpleLongCounter(-1);
			while(true)
			{
				if (currentCacheEntry == null) // We could have element that hasn't finished processing yet
					currentCacheEntry = returnQueue.take(); // get next element
				
				// This is set to positive value in order to delay processing of the current element.
				sleepDelayInsteadOfProcessing.set(-1); // reset sleep flag
				WBRBCacheEntry cacheEntry = currentCacheEntry; // process this, don't touch 'current' variable unnecessarily
				
				withWriteLock(cacheEntry, () -> {				
					K key = cacheEntry.getKey();
					WBRBCachePayload payload = cacheEntry.getPayload();

					// Check if we should process or sleep.
					final long now = timeNow();
					long sleepFor = -1;
					{
						long cacheUntil = timeAddVirtualIntervalToRealWorldTime(
							payload.getInQueueSince(), config.getReturnQueueCacheTimeMin());
						
						// During shutdown we process ASAP; but if an item has write currently pending, there's no reason not to wait
						if (isFlushing())
						{
							boolean needToWait = false;
							switch(payload.getWriteStatus())
							{
								case NO_WRITE_REQUESTED_YET:
								case REMOVED_FROM_CACHE:
								case WRITE_FAILED_FINAL:
								case WRITE_SUCCESS:
									break;
								case WRITE_PENDING:
									needToWait = true;
									break;
							}
							if (!needToWait)
								cacheUntil = 0;
						}
						
						if (cacheUntil > now)
						{
							// need to wait
							long maxWaitUntil = timeAddVirtualIntervalToRealWorldTime(
								now, config.getMaxSleepTime()); // never wait more than max sleep time at a time to avoid issues due to shutdown or changing time factor
							
							long waitUntil = Math.min(maxWaitUntil, cacheUntil);
							
							sleepFor = waitUntil - now;
						}
					}
					
					boolean canDoNormalProcessing = true;
					switch (payload.getReadStatus())
					{
						case REMOVED_FROM_CACHE:
							canDoNormalProcessing = false;
							break;
							
						case READ_FAILED_FINAL:
						case NOT_READ_YET:
						case DATA_READY_RESYNC_FAILED_FINAL:
						case DATA_READY_RESYNC_PENDING:
						case DATA_READY:
							break;
					}
					
					if (sleepFor > 0)
					{
						// Sleep instead of processing.
						sleepDelayInsteadOfProcessing.set(sleepFor);
					}
					else if (canDoNormalProcessing) // if not, we just do nothing as e.g. element was removed from cache or some such
					{
						// Process item.
						getStats().returnQueueProcessedItems.incrementAndGet();
						getStats().returnQueueLastItemInQueueDurationMs.set(
							timeGapVirtual(payload.getInQueueSince(), now)
						);
						
						final boolean itemHadAccessSinceMainQueue;
						final long itemUntouchedMs;
						{
							long lastRead = payload.getLastReadTimestamp();
							long lastWrite = payload.getLastWriteTimestamp();
							
							itemHadAccessSinceMainQueue = (lastRead > 0) || (lastWrite > 0); 

							long lastTouched = Math.max(Math.abs(lastRead), Math.abs(lastWrite));
							long untouchedMs = timeGapVirtual(lastTouched, timeNow());
							
							if (untouchedMs < 0)
							{
								// This is basically an error
								getStats().returnQueueNegativeTimeSinceLastAccessErrorCount.incrementAndGet();
								logMessage(WBRBCacheMessage.RETURN_QUEUE_NEGATIVE_TIME_SINCE_TOUCHED, null, key, untouchedMs);
								// if item had negative 'time since touched', replace with max value intending for item to be removed from cache
								untouchedMs = Long.MAX_VALUE; 
							}
							
							itemUntouchedMs = untouchedMs;
						}
						
						WBRBReturnQueueItemProcessingDecision decision;
						{
							ObjectWrapper<@Nonnull WBRBReturnQueueItemProcessingDecision> decisionWrapper = ObjectWrapper.of(WBRBReturnQueueItemProcessingDecision.REMOVE_FROM_CACHE);
							guardedInvocationNonNull(() -> spiWriteLockMakeReturnQueueProcessingDecision(key, itemHadAccessSinceMainQueue, itemUntouchedMs, cacheEntry, payload), WBRBCacheMessage.SPI_EXCEPTION_MakeReturnQueueProcessingDecision, key)
								.ifPresentInterruptibly(d -> decisionWrapper.set(d))
								.ifExceptionInterruptibly(e -> decisionWrapper.set(WBRBReturnQueueItemProcessingDecision.REMOVE_FROM_CACHE));
							decision = decisionWrapper.get();
						}
						
						if (decision.isStopCollectingUpdates())
						{
							wrappedSpiWriteLockUpdates_reset(WBRBUpdatesResetReason.RETURN_QUEUE_DECISION, false, key, cacheEntry, payload);
						}											
						
						// Do monitoring stuff
						switch (decision.getAction())
						{
							case EXPIRE_FROM_CACHE:
							case MAIN_QUEUE_NO_RESYNC:
							case MAIN_QUEUE_PLUS_RESYNC:
							case REMOVE_FROM_CACHE:
								{
									// full cache cycle monitoring
									int cycleCount = payload.getFullCacheCycleCountByReturnQueue().incrementAndGet();
									boolean processed = false;
									List<Integer> cycleThresholds = config.getMonitoringFullCacheCyclesThresholds();
									for (int i = 0; i < cycleThresholds.size(); i++)
									{
										if (cycleCount <= cycleThresholds.get(i))
										{
											processed = true;
											getStats().fullCycleCountThresholdCounters[i].incrementAndGet();
											break;
										}
									}
									if (!processed) // process 'more than all thresholds'
										getStats().fullCycleCountThresholdCounters[cycleThresholds.size()].incrementAndGet();
								}
								
								{
									// time since last access monitoring
									List<Long> untouchedThresholds = config.getMonitoringTimeSinceAccessThresholds();
									boolean processed = false;
									for (int i = 0; i < untouchedThresholds.size(); i++)
									{
										if (itemUntouchedMs <= untouchedThresholds.get(i))
										{
											processed = true;
											getStats().timeSinceLastAccessThresholdCounters[i].incrementAndGet();
											break;
										}
									}
									if (!processed) // process 'more than all thresholds'
										getStats().timeSinceLastAccessThresholdCounters[untouchedThresholds.size()].incrementAndGet();
								}
								
								break;
							case DO_NOTHING:
							case RETURN_QUEUE:
								break; // if we didn't properly 'finish' return queue processing, don't update stats
						}
						
						boolean logNonStandardOutcome = false;
						switch (decision.getAction())
						{
							case DO_NOTHING:
								getStats().returnQueueDoNothingCount.incrementAndGet();
								logNonStandardOutcome = true;
								break;
							case EXPIRE_FROM_CACHE:
								getStats().returnQueueExpiredFromCacheCount.incrementAndGet();
								haveWriteLock_RemoveFromCache_NoMessageIfAlreadyRemoved(cacheEntry);
								break;
							case REMOVE_FROM_CACHE:
								getStats().returnQueueRemovedFromCacheCount.incrementAndGet();
								haveWriteLock_RemoveFromCache_NoMessageIfAlreadyRemoved(cacheEntry);
								logNonStandardOutcome = true;
								break;
							case RETURN_QUEUE:
								getStats().returnQueueRequeueToReturnQueueCount.incrementAndGet();
								payload.setInQueueSince(timeNow());
								returnQueue.add(cacheEntry);
								logNonStandardOutcome = true;
								break;
							case MAIN_QUEUE_NO_RESYNC:
								payload.setInQueueSince(timeNow());
								mainQueue.add(cacheEntry);
								logNonStandardOutcome = true; // not 100% positive this is good, but it can be overridden
								break;
							case MAIN_QUEUE_PLUS_RESYNC:
								boolean proceed = true;
								switch (payload.getReadStatus())
								{
									case REMOVED_FROM_CACHE:
										proceed = false;
										haveWriteLock_RemoveFromCache(cacheEntry);
										logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), "code should not be reachable");
										break;
									case NOT_READ_YET:
									case READ_FAILED_FINAL:
										payload.setReadStatus(WBRBCacheEntryReadStatus.NOT_READ_YET); // indicate initial read
										break;
									case DATA_READY_RESYNC_PENDING:
									case DATA_READY:
									case DATA_READY_RESYNC_FAILED_FINAL:
										payload.setReadStatus(WBRBCacheEntryReadStatus.DATA_READY_RESYNC_PENDING); // indicate resync
										break;
								}
								
								if (proceed)
								{
									payload.setInQueueSince(timeNow());
									mainQueue.add(cacheEntry);
									readQueue.add(cacheEntry);
									getStats().returnQueueScheduledResyncs.incrementAndGet();
								}
								break;
						}
						
						// Log non-standard outcome (flag controls whether default implementation will actually log
						{
							final boolean doLog = logNonStandardOutcome;
							guardedInvocation(() -> spiWriteLockMakeReturnQueueProcessingDecision_logNonStandardOutcome(doLog, decision, key, cacheEntry, payload), WBRBCacheMessage.SPI_EXCEPTION_MakeReturnQueueProcessingDecision_logNonStandardOutcome, key);
						}
						
						// Notification event
						wrappedSpiUnknownLock_Event(WBRBEvent.RETURN_QUEUE_ITEM_PROCESSED, key, cacheEntry, payload, null, decision);
						
					} // end of sleep or processing IF
					
				}); // end of write lock
				
				{
					long sleep = sleepDelayInsteadOfProcessing.get();
					if (sleep > 0)
						Thread.sleep(sleep); // Delay and iterate the same element again
					else
						currentCacheEntry = null; // done with the current, may process the next entry
				}
				
			} // end infinite while() loop
			
		} finally
		{
			// so the fallback position here is that if there's current
			// currentCacheEntry value then it was removed from return processing queues
			// and was not yet re-added to a processing queue or removed from
			// in-flight map
			// Therefore to prevent in-flight map retaining orphan entries we
			// must remove it from the map here
			if (currentCacheEntry != null)
			{
				haveNoLock_RemoveFromCache(currentCacheEntry);
				logMessage(WBRBCacheMessage.UNEXPECTED_CACHE_REMOVAL_IN_RETURN_QUEUE_PROCESSING, null, currentCacheEntry.getKey());
			}
		}
	}

	// CCC comment
	protected WBRBReturnQueueItemProcessingDecision spiWriteLockMakeReturnQueueProcessingDecision(
		K key, boolean itemHadAccessSinceMainQueue, long itemUntouchedMs, WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		ObjectWrapper<@Nonnull WBRBReturnQueueItemProcessingDecision> decision = ObjectWrapper.of(WBRBReturnQueueItemProcessingDecision.REMOVE_FROM_CACHE);
		
		WBRBCacheEntryWriteStatus writeStatus = payload.getWriteStatus();
		switch (writeStatus)
		{
			case REMOVED_FROM_CACHE:
				return WBRBReturnQueueItemProcessingDecision.DO_NOTHING;
			case WRITE_PENDING:
				guardedInvocationNonNull(() -> spiWriteLockMakeReturnQueueProcessingDecision_WritePending(key, cacheEntry, payload), WBRBCacheMessage.SPI_EXCEPTION_MakeReturnQueueProcessingDecision_WritePending, key)
					.ifPresentInterruptibly(d -> decision.set(d) )
					.ifExceptionInterruptibly(e -> decision.set(WBRBReturnQueueItemProcessingDecision.REMOVE_FROM_CACHE) );
				return decision.get();
			case WRITE_FAILED_FINAL:
				guardedInvocationNonNull(() -> spiWriteLockMakeReturnQueueProcessingDecision_WriteFailedFinal(key, cacheEntry, payload), WBRBCacheMessage.SPI_EXCEPTION_MakeReturnQueueProcessingDecision_WriteFailedFinal, key)
					.ifPresentInterruptibly(d -> decision.set(d) )
					.ifExceptionInterruptibly(e -> decision.set(WBRBReturnQueueItemProcessingDecision.REMOVE_FROM_CACHE) );
				return decision.get();
			case NO_WRITE_REQUESTED_YET: // this one is valid, it's set by main queue processing if write is not requested on the split (e.g. if there were no writes to cache entry)
			case WRITE_SUCCESS:
				// Standard handling
				guardedInvocationNonNull(() -> spiWriteLockMakeReturnQueueProcessingDecision_WriteOk(key, itemHadAccessSinceMainQueue, itemUntouchedMs, cacheEntry, payload), WBRBCacheMessage.SPI_EXCEPTION_MakeReturnQueueProcessingDecision_WriteOk, key)
					.ifPresentInterruptibly(d -> decision.set(d) )
					.ifExceptionInterruptibly(e -> decision.set(WBRBReturnQueueItemProcessingDecision.REMOVE_FROM_CACHE) );
				return decision.get();
		}
		
		logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), "code should not be reachable");
		return WBRBReturnQueueItemProcessingDecision.REMOVE_FROM_CACHE;
	}
	
	// CCC
	private static final WBRBReturnQueueItemProcessingDecision RETURN_DECISION_MAIN_QUEUE_NO_RESYNC =
		new WBRBReturnQueueItemProcessingDecision(
			WBRBReturnQueueItemProcessingDecisionAction.MAIN_QUEUE_NO_RESYNC, 
			true);
	// CCC
	private static final WBRBReturnQueueItemProcessingDecision RETURN_DECISION_MAIN_QUEUE_NO_RESYNC_MAY_COLLECT_UPDATES =
		new WBRBReturnQueueItemProcessingDecision(
			WBRBReturnQueueItemProcessingDecisionAction.MAIN_QUEUE_NO_RESYNC, 
			false);
	// CCC
	@SuppressWarnings("unused")
	protected WBRBReturnQueueItemProcessingDecision spiWriteLockMakeReturnQueueProcessingDecision_WriteFailedFinal(
		K key, WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		// Re-put into main queue and then rely on main processing queue handler to handle this
		return RETURN_DECISION_MAIN_QUEUE_NO_RESYNC;
	}
	
	// CCC
	private static final WBRBReturnQueueItemProcessingDecision RETURN_DECISION_RETURN_QUEUE =
		new WBRBReturnQueueItemProcessingDecision(
			WBRBReturnQueueItemProcessingDecisionAction.RETURN_QUEUE, 
			false);
	// CCC
	protected WBRBReturnQueueItemProcessingDecision spiWriteLockMakeReturnQueueProcessingDecision_WritePending(
		K key, WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		if (payload.getReturnQueueRetryCount().incrementAndGet() > config.getReturnQueueMaxRequeueCount())
		{
			// Too many retries, handle the same as write failure
			return spiWriteLockMakeReturnQueueProcessingDecision_WriteFailedFinal(key, cacheEntry, payload);
		}
		
		return RETURN_DECISION_RETURN_QUEUE;
	}
	
	// CCC
	private static final WBRBReturnQueueItemProcessingDecision RETURN_DECISION_MAIN_QUEUE_PLUS_RESYNC =
		new WBRBReturnQueueItemProcessingDecision(
			WBRBReturnQueueItemProcessingDecisionAction.MAIN_QUEUE_PLUS_RESYNC, 
			false);
	// CCC
	protected WBRBReturnQueueItemProcessingDecision spiWriteLockMakeReturnQueueProcessingDecision_WriteOk(
		K key, boolean itemHadAccessSinceMainQueue, long itemUntouchedMs, WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		if (!itemHadAccessSinceMainQueue)
		{
			// Item was not touched in return queue, we need to figure out
			// whether it was 'recently' accessed to determine whether to keep
			// or expire it
			
			// When flushing (also when shutting down), do not retain items unnecessarily
			if (isFlushing())
				return WBRBReturnQueueItemProcessingDecision.EXPIRE_FROM_CACHE;
			
			if (itemUntouchedMs < 0)
			{
				CacheInternalException e = new CacheInternalException("ASSERTION FAILED: unreachable code block reached for key [" + key + "]");
				logMessage(WBRBCacheMessage.ASSERTION_FAILED, e, e.toString());
				return WBRBReturnQueueItemProcessingDecision.EXPIRE_FROM_CACHE;
			}
			
			// If item is untouched 'long enough', then expire it
			if (itemUntouchedMs >= config.getUntouchedItemCacheExpirationDelay())
				return WBRBReturnQueueItemProcessingDecision.EXPIRE_FROM_CACHE;

			long mainQueueSize = mainQueue.size();
			if (mainQueueSize >= config.getMainQueueMaxTargetSize())
			{
				// If cache is too full, expire item anyway
				getStats().returnQueueItemNotRetainedDueToMainQueueSizeCount.incrementAndGet();
				logMessage(WBRBCacheMessage.RETURN_QUEUE_ITEM_NOT_RETAINED_DUE_TO_MAIN_QUEUE_SIZE, null, key, mainQueueSize);
				return WBRBReturnQueueItemProcessingDecision.EXPIRE_FROM_CACHE;
			}
			
			// fall-through to decide whether we need to issue resync etc.
		}
		
		if (wrappedSpiWriteLockUpdates_isMergePossible(key, cacheEntry, payload, NullableOptional.empty()))
		{
			WBRBCacheEntryReadStatus readStatus = payload.getReadStatus();
			switch (readStatus)
			{
				case REMOVED_FROM_CACHE:
					return WBRBReturnQueueItemProcessingDecision.DO_NOTHING;
				case NOT_READ_YET:
				case DATA_READY_RESYNC_PENDING:
					// Previous read hasn't completed one way or another yet, we mustn't start another read
					if (config.isAllowUpdatesCollectionForMultipleFullCycles())
						return RETURN_DECISION_MAIN_QUEUE_NO_RESYNC_MAY_COLLECT_UPDATES; // no resync, but may continue to collect updates
					else
						return RETURN_DECISION_MAIN_QUEUE_NO_RESYNC;
				case DATA_READY:
				case READ_FAILED_FINAL:
				case DATA_READY_RESYNC_FAILED_FINAL:
					return RETURN_DECISION_MAIN_QUEUE_PLUS_RESYNC;
			}
			
			logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), "code should not be reachable");
			return WBRBReturnQueueItemProcessingDecision.REMOVE_FROM_CACHE;
		}
		else
			return RETURN_DECISION_MAIN_QUEUE_NO_RESYNC; // FIX-ME check boolean is set properly for instances of this class
	}
	
	/**
	 * Allows implementations to override decision/logging on whether main queue
	 * processing code should report a non-standard outcome message
	 * <p>
	 * Default implementation log non-standard outcome message if doLog is true
	 * 
	 * @param doLog 'opinion' of the main queue processor as
	 * 		to whether message should be logged
	 * @param decisionOutcome return queue processing decision that led to this
	 * 		invocation
	 */
	@SuppressWarnings("unused")
	protected void spiWriteLockMakeReturnQueueProcessingDecision_logNonStandardOutcome(
		boolean doLog,
		WBRBReturnQueueItemProcessingDecision decisionOutcome,
		K key, WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		if (doLog)
			logMessage(WBRBCacheMessage.RETURN_QUEUE_NON_STANDARD_OUTCOME, null, key, decisionOutcome);		
	}
	
	/**
	 * CCC comment
	 */
	@SuppressWarnings("unused")
	protected void spiWriteLockEvent_ReturnQueueProcessed(@Nonnull K key,
		WBRBReturnQueueItemProcessingDecision decision,
		WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		// default does nothing
	}
	
	
	/**
	 * @param maxWaitVirtualMsec if positive, might try to add element to cache /
	 * 		wait for an element to load; use a negative value to never add element/
	 * 		wait for the loading; zero will try to add element to cache (if
	 * 		missing), but will not wait
	 * 
	 * @return {@link NullableOptional} -- empty if element is not yet loaded,
	 * 		with value if value was read; if it is empty, may contain exception
	 * 		indicating some additional reason as to why element not yet loaded,
	 * 		e.g. CacheInternalException "Too many attempts ... encountered REMOVED_FROM_CACHE"
	 * 		or CacheElementFailedLoadingException (in case underlying load failed)
	 */
	// CCC comment and check exceptions
	protected NullableOptional<V> internalRead(K key, long realWorldAnchorTime, long maxWaitVirtualMsec) 
		throws CacheIllegalStateException, CacheInternalException, WAInterruptedException
	{
		checkStandardCacheOperationsAllowed();
		
		boolean success = false;
		boolean interrupted = false;
		try
		{
			getStats().cacheReadAttempts.incrementAndGet();
			
			NullableOptional<V> result = internalRead0(key, realWorldAnchorTime, maxWaitVirtualMsec);
			
			if (result.isEmpty())
			{
				if (maxWaitVirtualMsec > 0) // no-wait stuff probably shouldn't count as timeout
					getStats().cacheReadTimeouts.incrementAndGet();
			}
			
			success = true;
			return result;
		} catch (Exception e)
		{
			if (ExceptionUtils.getRootCause(e) instanceof InterruptedException)
				interrupted = true;
			
			throw e;
		}
		finally
		{
			if (!success)
			{
				if (interrupted)
					getStats().cacheReadInterrupts.incrementAndGet();
				else
					getStats().cacheReadErrors.incrementAndGet();
			}
		}
	}
	
	/**
	 * @param maxWaitVirtualMsec if positive, might try to add element to cache /
	 * 		wait for an element to load; use a negative value to never add element/
	 * 		wait for the loading; zero will try to add element to cache (if
	 * 		missing), but will not wait
	 * 
	 * @return {@link NullableOptional} -- empty if element is not yet loaded,
	 * 		with value if value was read; if it is empty, may contain exception
	 * 		indicating some additional reason as to why element not yet loaded,
	 * 		e.g. CacheInternalException "Too many attempts ... encountered REMOVED_FROM_CACHE"
	 * 		or CacheElementFailedLoadingException (in case underlying load failed)
	 */
	// CCC comment and check exceptions
	// TO-DO tests for maxWaitVirtualMsec values
	protected NullableOptional<V> internalRead0(K key, long realWorldAnchorTime, long maxWaitVirtualMsec) 
		throws CacheIllegalStateException, CacheInternalException, WAInterruptedException
	{
		// Check key here to avoid wrapping exception from haveNoLock_checkCache later on
		if (nullable(key) == null)
		{
			getStats().checkCacheNullKeyCount.incrementAndGet();
			throw new IllegalArgumentException("Key must not be null.");
		}
		
		try
		{
			final int retries = config.getMaxCacheRemovedRetries();
			ObjectWrapper<Throwable> exceptionToThrow = ObjectWrapper.of(null);
			ObjectWrapper<Throwable> exceptionToReturn = ObjectWrapper.of(null);
			for (int removedFromCacheResultCounter = 0; removedFromCacheResultCounter < retries; removedFromCacheResultCounter++)
			{
				// Figure out cache entry
				final WBRBCacheEntry cacheEntry = haveNoLock_CheckCache(
					key, 
					(maxWaitVirtualMsec >= 0)/*whether to add if missing*/,
					(maxWaitVirtualMsec == 0)/*when no-waiting, just-added item should return null*/,
					removedFromCacheResultCounter == 0 ? Boolean.FALSE : null); // only first (non-retry) attempt should affect most counters
				
				if (cacheEntry == null)
					return NullableOptional.empty(); // If no wait and no pre-existing entry, indicate no result immediately
				
				boolean anotherDecisionAttempt = true;
				while (anotherDecisionAttempt)
				{
					anotherDecisionAttempt = false; // no another attempt unless specially requested
					// return values:
					// null -- element was removed from cache
					// exception is set -- write failed, re-throw the exception
					// optional is empty -- cache element not yet loaded
					// optional has value -- this is the actual value, to be returned
					@Nullable
					NullableOptional<V> result = withReadLock(cacheEntry, () -> {
						WBRBCachePayload payload = cacheEntry.getPayload();
						payload.setLastReadTimestamp(timeNow()); // this is volatile, so we can do this w/ read lock, also do it right after another volatile access to hopefully reduce perf hit
						
						NullableOptional<@Nonnull WBRBCacheAccessDecision> invocationResult = guardedInvocationNonNull(() -> 
							spiReadLockMakeCacheReadDecision(key, cacheEntry, payload), WBRBCacheMessage.SPI_EXCEPTION_MakeCacheReadDecision, key);
						
						// If there's been an exception -- handle it
						if (invocationResult.hasException())
						{
							exceptionToThrow.set(invocationResult.getException());
							return NullableOptional.empty(); // returns to read lock result
						}
						
						WBRBCacheAccessDecision decision = invocationResult.get();
						switch (decision.getAction())
						{
							case REMOVED_FROM_CACHE:
								return null; // returns to read lock result
							case THROW_EXCEPTION:
								throw decision.getException(); // BaseCacheException subclasses are re-thrown as is in upper-level code
							case RETURN_EXCEPTION:
								exceptionToReturn.set(decision.getException());
								return null; // returns to read lock result
							case WAIT_FOR_LATCH:
								return NullableOptional.empty(); // returns to read lock result
								
							case VALUE_RETURNED:
								S cachedData = decision.getReadValue();
								NullableOptional<V> resultOptional = guardedInvocation( () -> 
									spiSomeLockConvertFromCacheFormat(key, cachedData, cacheEntry, payload), WBRBCacheMessage.CONVERT_FROM_CACHE_FORMAT_TO_RETURN_VALUE_FAIL, key );
								
								if (resultOptional.hasException())
								{
									exceptionToThrow.set(resultOptional.getException());
									return NullableOptional.empty(); // returns to read lock result
								}
								
								// Event notification
								// NOTE!!! this event is also fired in internalWrite0()
								wrappedSpiUnknownLock_Event(WBRBEvent.CACHE_READ, key, cacheEntry, payload, null, resultOptional);								
								
								return resultOptional; // returns to read lock result
						} // end of decision switch loop
	
						CacheInternalException exc = new CacheInternalException("ASSERTION FAILED: Failed cache read for key [" + key + "]");
						logMessage(WBRBCacheMessage.ASSERTION_FAILED, exc, "code should not be reachable");
						exceptionToThrow.set(exc);
						return NullableOptional.empty(); // returns to read lock result
					}); // end read lock
					
					// First check if we have an exception
					{
						Throwable exc = exceptionToThrow.get();
						if (exc != null)
						{
							// TO-DO does it need logging?
							haveNoLock_RemoveFromCache(cacheEntry); // NOTE: there's counterpart for this in internalWriteIfCached0() 
							throw new CacheInternalException("Failed cache read for key [" + key + "]: " + exc, exc);
						}
					}
					
					// Check if there's an exception to return
					{
						Throwable exc = exceptionToReturn.get();
						if (exc !=null)
							return NullableOptional.emptyWithException(exc);
					}
					
					if (result != null) // non-null optional means we have final result
					{
						if (result.isPresent())
							return result;
						else
						{
							// No result is yet available, decide if we should wait (we're OUTSIDE read lock here, it's IMPORTANT!)
							// TO-DO: add to monitoring?
							// TO-DO: needs testing to see if it waits properly
							if (maxWaitVirtualMsec < 1)
							{
								// If no wait, indicate no result immediately
								return NullableOptional.empty();
							}
							else
							{
								waitForTime: // wait-for-time loop
								while (true) 
								{
									long now = timeNow();
									long waitUntil = timeAddVirtualIntervalToRealWorldTime(
										realWorldAnchorTime, maxWaitVirtualMsec);
									
									if (waitUntil > now)
									{
										// need to wait
										long maxWaitUntil = timeAddVirtualIntervalToRealWorldTime(
											now, config.getMaxSleepTime()); // never wait more than max sleep time at a time to avoid issues due to shutdown or changing time factor
										
										final boolean finalWait;
										if (maxWaitUntil < waitUntil)
										{
											finalWait = false;
											waitUntil = maxWaitUntil;
										}
										else
											finalWait = true;
										
										long waitFor = waitUntil - now;
										
										if (cacheEntry.getAccessLatch().await(waitFor, TimeUnit.MILLISECONDS))
										{
											// Latch unlocked, go for another attempt
											anotherDecisionAttempt = true;
											// also reset retries counter as we had a valid value that only later turned out 
											// 'bad' (e.g. due to initial read failure)
											// 2020-10-29 Solf: this turned out to be a BAD idea, it can potentially loop
											// repeatedly failing reads (if final fail lead to cache removal) until read 
											// timeout expires
//											removedFromCacheResultCounter = 0;{}
											break waitForTime; // break out of wait-for-time loop
										}
										else
										{
											// wait time has expired
											if (finalWait)
												return NullableOptional.empty(); // return from method if that was final wait attempt
											
											// not a final wait, need to try again
											checkStandardCacheOperationsAllowed(); // in case cache was moved to 'disallowed' state concurrently, such as shutdown
											continue waitForTime; // next iteration of wait-for-time
										}
									}
									else
									{
										// wait time has expired
										return NullableOptional.empty(); // return from method
									}
								} // end of wait-for-latch loop
								
							} // end of wait decision block
						} // end of result is empty block
					} // end of non-null result
				} // end of another decision attempt loop
					
				// Null result means we need to make another try at the map read (because previously read element was removed from cache)
			}
			
			CacheInternalException attemptsException = new CacheInternalException("Too many attempts [" + retries +"] encountered REMOVED_FROM_CACHE state while reading: " + key);
			logMessage(WBRBCacheMessage.TOO_MANY_REMOVED_FROM_CACHE_STATE_RETRIES, attemptsException, key, retries);
			
			// This can be caused by effectively 'not yet loaded' state (if element 
			// fails to load repeatedly), so return optional rather than throw
			//throw attemptsException;
			return NullableOptional.emptyWithException(attemptsException);
		} catch (InterruptedException e)
		{
			throw new WAInterruptedException(e);
		} catch (RuntimeException e)
		{
			if (e instanceof BaseCacheException)
				throw e;

			String msg = "ASSERTION FAILED: Failed cache read for key [" + key + "]: " + e;
			CacheInternalException e2 = new CacheInternalException(msg, e);
			try
			{
				logMessage(WBRBCacheMessage.ASSERTION_FAILED, e2, msg);
			} catch (Exception e3)
			{
				// Ignore this
			}
			throw e2;
		}
	}
	
	@Override
	public NullableOptional<V> readIfCached(K key) 
		throws IllegalArgumentException, CacheIllegalStateException, CacheInternalException, WAInterruptedException
	{
		return internalRead(key, 0, -1);
	}
	
	@Override
	public V readIfCachedOrException(K key) 
		throws CacheElementNotYetLoadedException, CacheIllegalExternalStateException, IllegalArgumentException, CacheIllegalStateException, CacheInternalException, WAInterruptedException
	{
		NullableOptional<V> result = readIfCached(key);
		if (result.isPresent())
			return result.get();
	
		// TO-DO monitor?
		throw generateAccessOrExceptionException(key, result); 
	}
	
	@Override
	public NullableOptional<V> readFor(K key, long limitMillis) 
		throws IllegalArgumentException, CacheFullException, CacheIllegalStateException, CacheInternalException, WAInterruptedException
	{
		return internalRead(key, timeNow(), limitMillis);
	}

	@Override
	public V readForOrException(K key, long limitMillis) 
		throws CacheElementNotYetLoadedException, CacheIllegalExternalStateException, IllegalArgumentException, CacheFullException, CacheIllegalStateException, CacheInternalException, WAInterruptedException
	{
		NullableOptional<V> result = readFor(key, limitMillis);
		if (result.isPresent())
			return result.get();
	
		// TO-DO monitor?
		throw generateAccessOrExceptionException(key, result); 
	}
	
	/**
	 * Prepare exception to be thrown by various (read/write)*OrException methods
	 * 
	 * @param exceptionSource if this contains exception, it will be re-thrown
	 * 		or used as cause exception
	 */
	protected RuntimeException generateAccessOrExceptionException(K key, NullableOptional<?> exceptionSource)
	{
		if (exceptionSource.isPresent())
		{
			logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), "code should not be reachable");
			throw new CacheInternalException("assertion failed -- code should not be reachable");
		}
		
		if (exceptionSource.hasException())
		{
			Throwable exception = exceptionSource.getException();
			if (exception instanceof CacheIllegalExternalStateException)
				return (CacheIllegalExternalStateException)exception; // these are thrown without wrapping
		}
		
		// wrap causing exception if any
		return new CacheElementNotYetLoadedException(commonNamingPrefix, key, exceptionSource);
	}
	
	
	@Override
	public NullableOptional<V> readUntil(K key, long limitTimestamp) 
		throws IllegalArgumentException, CacheFullException, CacheIllegalStateException, CacheInternalException, WAInterruptedException
	{
		long now = timeNow();
		long delta = limitTimestamp - now;
		if (delta < 1)
			delta = -1;
		
		return internalRead(key, now, delta);
	}

	@Override
	public V readUntilOrException(K key, long limitTimestamp) 
		throws CacheElementNotYetLoadedException, CacheIllegalExternalStateException, IllegalArgumentException, CacheFullException, CacheIllegalStateException, CacheInternalException, WAInterruptedException
	{
		NullableOptional<V> result = readUntil(key, limitTimestamp);
		if (result.isPresent())
			return result.get();
		
		// TO-DO monitor?
		throw generateAccessOrExceptionException(key, result); 
	}
	
	// CCC comment
	protected WBRBCacheAccessDecision spiReadLockMakeCacheReadDecision(K key, WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
		throws InterruptedException
	{
		WBRBCacheEntryReadStatus readStatus = payload.getReadStatus();
		switch (readStatus)
		{
			case NOT_READ_YET:
				return WBRBCacheAccessDecision_WAIT_FOR_LATCH;
			case REMOVED_FROM_CACHE:
				return WBRBCacheAccessDecision_REMOVED_FROM_CACHE;
			case READ_FAILED_FINAL:
				return new WBRBCacheAccessDecision(false, new CacheElementFailedLoadingException(commonNamingPrefix, key));
			case DATA_READY_RESYNC_FAILED_FINAL:
				// Do this without guarding the invocation because there isn't much we can do about exception
				// here anyway (can't upgrade read lock to write lock).
				CacheIllegalStateException e = spiReadLockMakeCacheReadDecision_ResyncFailedFinal(key, cacheEntry, payload);
				if (e != null)
					return new WBRBCacheAccessDecision(false, e);
				//$FALL-THROUGH$
			case DATA_READY:
			case DATA_READY_RESYNC_PENDING:
				return new WBRBCacheAccessDecision(payload.getValue());
		}

		CacheInternalException exception = new CacheInternalException("assertion failed -- code should not be reachable");
		logMessage(WBRBCacheMessage.ASSERTION_FAILED, exception, "code should not be reachable");
		throw exception;
	}
	
	/**
	 * @return null if should just return the cached value; exception to return/throw
	 * 		otherwise
	 */
	// CCC comment
	@SuppressWarnings("unused")
	@Nullable
	protected CacheIllegalStateException spiReadLockMakeCacheReadDecision_ResyncFailedFinal(
		K key, WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		if (config.isAllowDataReadingAfterResyncFailedFinal())
		{
			return null;
		}
		
		return new CacheElementFailedResyncException(commonNamingPrefix, key);
	}

	@Override
	// TO-DO needs testing that it returns boolean instead of exception properly
	public NullableOptional<@Nonnull Boolean> writeIfCached(K key, UExt update) 
		throws IllegalArgumentException, CacheIllegalStateException, CacheInternalException, WAInterruptedException
	{
		return NullableOptional.fromPrototype(Boolean.TRUE, 
			internalWriteIfCached(key, update, false));
	}
	
	/**
	 * Records given update in the cache value if item is already cached.
	 * <p>
	 * This is a wrapper method providing some monitoring capability over
	 * {@link #internalWriteIfCached0(Object, Object, boolean)}
	 * 
	 * @param produceReadResult if true, will produce actual read result (after
	 * 		a write) that can/should be returned to the client similar to the
	 * 		various 'read' methods -- this will incur additional costs, such
	 * 		as {@link #convertFromCacheFormatToReturnValue(Object, Object)};
	 * 		if false, will return a stand-in value that can be interpreted
	 * 		to determine whether operation is successful (see return description) 
	 * 
	 * @return optional containing an object if write was successfully
	 * 		queued; empty optional if write was not made because element is not
	 * 		cached or is otherwise in invalid state; if optional is empty,
	 * 		it may additionally contain exception with additional details as to
	 * 		why write has failed; optional may contain actual returnable
	 * 		V value (for returning to the client) if produceReadResult is set
	 */
	protected NullableOptional<V> internalWriteIfCached(K key, UExt update, boolean produceReadResult) 
		throws IllegalArgumentException, CacheIllegalStateException, CacheInternalException, WAInterruptedException
	{
		checkStandardCacheOperationsAllowed();
		
		boolean success = false;
		try
		{
			getStats().cacheWriteAttempts.incrementAndGet();
			
			NullableOptional<V> result = internalWriteIfCached0(key, update, produceReadResult);
			
			if (!result.isPresent())
				getStats().cacheWriteElementNotPresentCount.incrementAndGet();
			
			success = true;
			return result;
		}
		finally
		{
			if (!success)
				getStats().cacheWriteErrors.incrementAndGet();
		}
	}
	
	/**
	 * {@link NullableOptional} with a fake value of V that is used to indicate
	 * success in {@link #internalWriteIfCached0(Object, Object)} if actual V
	 * value is not returned.
	 */
	private final NullableOptional<V> WRITE_SUCCESS_INDICATOR_V_OPTIONAL = TypeUtil.coerce(NullableOptional.of(new Object()));
	
	/**
	 * Records given update in the cache value if item is already cached.
	 * 
	 * @param produceReadResult if true, will produce actual read result (after
	 * 		a write) that can/should be returned to the client similar to the
	 * 		various 'read' methods -- this will incur additional costs, such
	 * 		as {@link #convertFromCacheFormatToReturnValue(Object, Object)};
	 * 		if false, will return a stand-in value that can be interpreted
	 * 		to determine whether operation is successful (see return description) 
	 * 
	 * @return optional containing an object if write was successfully
	 * 		queued; empty optional if write was not made because element is not
	 * 		cached or is otherwise in invalid state; if optional is empty,
	 * 		it may additionally contain exception with additional details as to
	 * 		why write has failed; optional may contain actual returnable
	 * 		V value (for returning to the client) if produceReadResult is set
	 */
	// TO-DO needs testing that it returns boolean instead of exception properly
	protected NullableOptional<V> internalWriteIfCached0(K key, UExt externalUpdate, boolean produceReadResult)
		throws IllegalArgumentException, CacheIllegalStateException, CacheInternalException, WAInterruptedException
	{
		
		if (nullable(key) == null)
		{
			getStats().checkCacheNullKeyCount.incrementAndGet();
			throw new IllegalArgumentException("Key must not be null.");
		}
		
		try
		{
			// Convert external update type to internal update.
			UInt update;
			{
				NullableOptional<UInt> conversionResult = guardedInvocation(() -> convertToInternalUpdateFormatFromExternalUpdate(key, externalUpdate), WBRBCacheMessage.CONVERT_TO_INTERNAL_UPDATE_FORMAT_FROM_EXTERNAL_UPDATE_FAIL, key);
				if (conversionResult.hasException())
				{
					Throwable t = conversionResult.getException();
					if (t instanceof RuntimeException)
						throw (RuntimeException)t;
					if (t instanceof InterruptedException)
						throw (InterruptedException)t;
					
					CacheInternalException e = new CacheInternalException("ASSERTION FAILED: unreachable code block reached for key [" + key + "] with throwable: " + t, t);
					logMessage(WBRBCacheMessage.ASSERTION_FAILED, e, e.toString());
					throw e;
				}
				
				update = conversionResult.get();
			}
			
			
			final int retries = config.getMaxCacheRemovedRetries();
			ObjectWrapper<Throwable> exceptionToThrow = ObjectWrapper.of(null);
			for (int i = 0; i < retries; i++)
			{
				WBRBCacheEntry cacheEntry = inflightMap.get(key);
				
				if (cacheEntry == null)
					return NullableOptional.empty();
				
				// return values:
				// null -- element was removed from cache
				// exception is set -- write failed, re-throw the exception
				// optional is empty -- cache element not yet loaded
				// optional has value (actual value is meaningless) -- write was done 
				@Nullable
				NullableOptional<V> result = withWriteLock(cacheEntry, () -> {
					WBRBCachePayload payload = cacheEntry.getPayload();
					payload.setLastWriteTimestamp(timeNow());
					
					NullableOptional<@Nonnull WBRBCacheAccessDecision> invocationResult = guardedInvocationNonNull(() -> 
						spiWriteLockMakeCacheWriteDecision(key, cacheEntry, payload), WBRBCacheMessage.SPI_EXCEPTION_MakeCacheWriteDecision, key);
					
					// If there's been an exception -- handle it
					if (invocationResult.hasException())
					{
						exceptionToThrow.set(invocationResult.getException());
						haveWriteLock_RemoveFromCache(cacheEntry);
						return NullableOptional.empty(); // returns to write lock result
					}
					
					WBRBCacheAccessDecision decision = invocationResult.get();
					switch (decision.getAction())
					{
						case REMOVED_FROM_CACHE:
							return null; // returns to write lock result
						case THROW_EXCEPTION:
							throw decision.getException(); // BaseCacheException subclasses are re-thrown as is in upper-level code
						case RETURN_EXCEPTION:
							return NullableOptional.emptyWithException(decision.getException()); // returns to write lock result
						case WAIT_FOR_LATCH:
							// TO-DO: add to monitoring?
							return NullableOptional.empty(); // returns to write lock result
							
						case VALUE_RETURNED:
							S cacheData = payload.getValue();
							NullableOptional<S> resultOptional = guardedInvocation( () ->
								applyUpdate(cacheData, update), WBRBCacheMessage.APPLY_UPDATE_FAIL, key );
							
							if (resultOptional.hasException())
							{
								exceptionToThrow.set(resultOptional.getException());
//								haveWriteLock_RemoveFromCache(cacheEntry); // TO-DO failed update probably doesn't mean forced removal? do we need to log? to monitor?
								return NullableOptional.empty(); // returns to write lock result
							}
							
							S newCachedData = resultOptional.get();
							payload.setValue(newCachedData); // update cached value
							
							// Event notification
							wrappedSpiUnknownLock_Event(WBRBEvent.CACHE_WRITE, key, cacheEntry, payload, null, fakeNonNull(update));								
							
							{
								// Attempt to collect update regardless of state, implementation should make decision on what to do
								NullableOptional<@Nullable CacheIllegalStateException> collectResult = guardedInvocation(() -> 
									spiWriteLockUpdates_collect(key, cacheEntry, payload, update), WBRBCacheMessage.SPI_EXCEPTION_Updates_collect, key);								
								if (collectResult.hasException())
								{
									exceptionToThrow.set(collectResult.getException());
									haveWriteLock_RemoveFromCache(cacheEntry);
									return NullableOptional.empty(); // returns to write lock result
								}
								
								{
									CacheIllegalStateException collectException = collectResult.get();
									if (collectException != null)
									{
										getStats().cacheWriteTooManyUpdates.incrementAndGet();
										logMessage(WBRBCacheMessage.TOO_MANY_CACHE_ELEMENT_UPDATES, collectException, key);
										wrappedSpiWriteLockUpdates_reset(WBRBUpdatesResetReason.UPDATE_COLLECT_EXCEPTION, false, key, cacheEntry, payload);
									}
								}
							}
							
							// We reach here if write was successful.
							if (produceReadResult) // this is the read-after-write case -- handle it
							{
								NullableOptional<V> convertOptional = guardedInvocation( () -> 
									spiSomeLockConvertFromCacheFormat(key, newCachedData, cacheEntry, payload), WBRBCacheMessage.CONVERT_FROM_CACHE_FORMAT_TO_RETURN_VALUE_FAIL, key );
							
								if (convertOptional.hasException())
								{
									haveWriteLock_RemoveFromCache(cacheEntry); // NOTE: there's counterpart for this in internalRead0()
									exceptionToThrow.set(convertOptional.getException());
									return NullableOptional.empty(); // returns to write lock result
								}
								
								// NOTE!!! this event is also fired in internalRead0()
								wrappedSpiUnknownLock_Event(WBRBEvent.CACHE_READ, key, cacheEntry, payload, null, convertOptional);
								
								return convertOptional; // returns to write lock result
							}
							
							return WRITE_SUCCESS_INDICATOR_V_OPTIONAL; // no read is required, so indicate success using a pre-generated marker
					}

					CacheInternalException exc = new CacheInternalException("ASSERTION FAILED: Failed cache write for key [" + key + "]");
					logMessage(WBRBCacheMessage.ASSERTION_FAILED, exc, "code should not be reachable");
					exceptionToThrow.set(exc);
					haveWriteLock_RemoveFromCache(cacheEntry);
					return NullableOptional.empty(); // returns to write lock result
				}); // end write lock
				
				// First check if we have an exception
				{
					Throwable exc = exceptionToThrow.get();
					if (exc != null)
					{
						// TO-DO does it need logging?
						throw new CacheInternalException("Failed cache write for key [" + key + "]: " + exc, exc);
					}
				}
				
				if (result != null) // non-null result means we have final result
				{
					return result; // return out-of-method
				}
				
				// Null result means we need to make another try at the map write
			}
			
			CacheInternalException attemptsException = new CacheInternalException("Too many attempts [" + retries +"] encountered REMOVED_FROM_CACHE state while writing: " + key);
			logMessage(WBRBCacheMessage.TOO_MANY_REMOVED_FROM_CACHE_STATE_RETRIES, attemptsException, key, retries);
			
			throw attemptsException;
		} catch (InterruptedException e)
		{
			throw new WAInterruptedException(e);
		} catch (RuntimeException e)
		{
			if (e instanceof BaseCacheException)
				throw e;

			String msg = "ASSERTION FAILED: Failed cache write for key [" + key + "]: " + e;
			CacheInternalException e2 = new CacheInternalException(msg, e);
			try
			{
				logMessage(WBRBCacheMessage.ASSERTION_FAILED, e2, msg);
			} catch (Exception e3)
			{
				// Ignore this
			}
			throw e2;
		}
	}
	

	@Override
	// CCC comment and check exceptions
	public void writeIfCachedOrException(K key, UExt update) 
		throws CacheElementNotYetLoadedException, CacheIllegalExternalStateException, IllegalArgumentException, CacheIllegalStateException, CacheInternalException, WAInterruptedException
	{
		NullableOptional<@Nonnull Boolean> result = writeIfCached(key, update);
		
		if (!result.isPresent())
		{
			// TO-DO monitor?
			throw generateAccessOrExceptionException(key, result); 
		}
	}
	

	@Override
	public NullableOptional<V> writeIfCachedAndRead(@Nonnull K key, UExt update)
		throws IllegalArgumentException,
		CacheIllegalStateException,
		CacheInternalException,
		WAInterruptedException
	{
		return internalWriteIfCached(key, update, true);
	}

	@Override
	public V writeIfCachedAndReadOrException(@Nonnull K key, UExt update)
		throws CacheElementNotYetLoadedException,
		CacheIllegalExternalStateException,
		IllegalArgumentException,
		CacheIllegalStateException,
		CacheInternalException,
		WAInterruptedException
	{
		NullableOptional<V> result = writeIfCachedAndRead(key, update);
		
		if (!result.isPresent())
		{
			// TO-DO monitor?
			throw generateAccessOrExceptionException(key, result); 
		}
		
		return result.get();
	}
	
	
	
	// CCC comment
	protected WBRBCacheAccessDecision spiWriteLockMakeCacheWriteDecision(K key, WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
		throws InterruptedException
	{
		WBRBCacheEntryReadStatus readStatus = payload.getReadStatus();
		switch (readStatus)
		{
			case NOT_READ_YET:
				return WBRBCacheAccessDecision_WAIT_FOR_LATCH;
			case REMOVED_FROM_CACHE:
				return WBRBCacheAccessDecision_REMOVED_FROM_CACHE;
			case READ_FAILED_FINAL:
				return new WBRBCacheAccessDecision(false, new CacheElementFailedLoadingException(commonNamingPrefix, key));
			case DATA_READY_RESYNC_FAILED_FINAL:
				// Do this without guarding the invocation and handle it on a higher level
				CacheIllegalStateException e = spiWriteLockMakeCacheWriteDecision_ResyncFailedFinal(key, cacheEntry, payload);
				if (e != null)
					return new WBRBCacheAccessDecision(false, e);
				//$FALL-THROUGH$
			case DATA_READY:
			case DATA_READY_RESYNC_PENDING:
				break; // proceed to check write status
		}
		
		WBRBCacheEntryWriteStatus writeStatus = payload.getWriteStatus();
		switch (writeStatus)
		{
			case REMOVED_FROM_CACHE:
				return WBRBCacheAccessDecision_REMOVED_FROM_CACHE;
			case WRITE_FAILED_FINAL:
				CacheIllegalStateException e = spiWriteLockMakeCacheWriteDecision_WriteFailedFinal(key, cacheEntry, payload);
				if (e != null)
					return new WBRBCacheAccessDecision(false, e);
				//$FALL-THROUGH$
			case NO_WRITE_REQUESTED_YET:
			case WRITE_PENDING:
			case WRITE_SUCCESS:
				return WBRBCacheAccessDecision_WRITE_ALLOWED;
		}
		
		CacheInternalException exception = new CacheInternalException("assertion failed -- code should not be reachable");
		logMessage(WBRBCacheMessage.ASSERTION_FAILED, exception, "code should not be reachable");
		throw exception;
	}
	
	
	/**
	 * @return null if should just return the cached value; exception to return/throw
	 * 		otherwise
	 */
	// CCC comment
	@SuppressWarnings("unused")
	@Nullable
	protected CacheIllegalStateException spiWriteLockMakeCacheWriteDecision_ResyncFailedFinal(
		K key, WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		if (config.isAllowDataWritingAfterResyncFailedFinal())
		{
			return null;
		}
		
		return new CacheElementFailedResyncException(commonNamingPrefix, key);
	}
	
	/**
	 * @return null if should just return the cached value; exception to throw
	 * 		otherwise
	 */
	// CCC comment
	@SuppressWarnings("unused")
	@Nullable
	protected CacheIllegalStateException spiWriteLockMakeCacheWriteDecision_WriteFailedFinal(
		K key, WBRBCacheEntry cacheEntry, WBRBCachePayload payload)
			throws InterruptedException
	{
		return null; // possibly there will be a retry later on
	}
	
	/**
	 * If event notifications are enabled {@link WBRBConfig#isEventNotificationEnabled()},
	 * then this method will be called with various events
	 * <p>
	 * Default implementation logs to {@link #debugEventLogger} (if it is non-null)
	 * with info severity.
	 */
	@SuppressWarnings("unused")
	protected void spiUnknownLock_Event(WBRBEvent event, @Nonnull K key,
		@Nullable WBRBCacheEntry cacheEntry, @Nullable WBRBCachePayload payload, 
		@Nullable Throwable exception, Object... additionalArgs)
			throws InterruptedException
	{
		Logger logger = debugEventLogger;
		if (logger != null) // whether to log everything
		{
			Pair<String, @Nullable Object[]> output = debugUnknownLock_FormatEventForSlf4jLogging(event, key, cacheEntry, payload, exception, additionalArgs);
			logger.info(output.getValue0(), output.getValue1());
		}
	}

	/**
	 * Checks whether event notifications are enabled {@link WBRBConfig#isEventNotificationEnabled()};
	 * if enabled, then calls
	 * {@link #spiUnknownLock_Event(WBRBEvent, Object, WBRBCacheEntry, WBRBCachePayload, Throwable, Object...)}
	 * wrapped in error handler.
	 */
	protected void wrappedSpiUnknownLock_Event(WBRBEvent event, @Nonnull K key,
		@Nullable WBRBCacheEntry cacheEntry, @Nullable WBRBCachePayload payload, 
		@Nullable Throwable exception, Object... additionalArgs)
			throws InterruptedException
	{
		if (!config.isEventNotificationEnabled())
			return;
		
		guardedInvocation(() -> spiUnknownLock_Event(event, key, cacheEntry, payload, exception, additionalArgs), WBRBCacheMessage.SPI_EXCEPTION_Event);		
	}
	
	
	/**
	 * Checks whether the cache is currently flushing -- this can be caused by
	 * e.g. shutdown or flush operations.
	 * <p>
	 * During flushing cache tries to spool out existing data as soon as possible.
	 */
	protected boolean isFlushing()
	{
		switch (controlState.get())
		{
			case NOT_STARTED:
			case RUNNING:
			case SHUTDOWN_COMPLETED:
				return false;
			case FLUSHING:
			case SHUTDOWN_IN_PROGRESS:
				return true;
		}
		
		logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), "code should not be reachable");
		throw new CacheInternalException("code should not be reachable");
	}
	
	/**
	 * Throws {@link CacheControlStateException} if cache is in a state where
	 * standard operations (read/write, not control ones) are not allowed (e.g.
	 * if cache is shutdown, or not started, etc.).
	 */
	protected void checkStandardCacheOperationsAllowed()
		throws CacheControlStateException
	{
		switch (controlState.get())
		{
			case NOT_STARTED:
				throw new CacheControlStateException(commonNamingPrefix, "cache was not yet started, operation cannot be performed");
			case FLUSHING:
				throw new CacheControlStateException(commonNamingPrefix, "cache is flushing, operation cannot be performed");
			case SHUTDOWN_IN_PROGRESS:
			case SHUTDOWN_COMPLETED:
				throw new CacheControlStateException(commonNamingPrefix, "cache was shutdown already, operation cannot be performed");
			case RUNNING:
				return; // this is ok
		}
		
		logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), "code should not be reachable");
		throw new CacheInternalException("code should not be reachable");
	}

	@Override
	public boolean isAlive()
	{
		switch (controlState.get())
		{
			case NOT_STARTED:
			case SHUTDOWN_IN_PROGRESS:
			case SHUTDOWN_COMPLETED:
				return false;
			case RUNNING:
			case FLUSHING:
				return true;
		}
		
		logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), "code should not be reachable");
		throw new CacheInternalException("code should not be reachable");
	}

	@Override
	public boolean isUsable()
	{
		try
		{
			checkStandardCacheOperationsAllowed();
			return true;
		} catch (CacheControlStateException e)
		{
			return false; // cache is not alive
		}
	}

	@Override
	public WBRBCacheControlState getControlState()
	{
		return controlState.get();
	}

	@Override
	public boolean shutdownFor(long limitMillis)
		throws CacheControlStateException, WAInterruptedException
	{
		return internalShutdown(timeNow(), limitMillis);
	}
	
	@Override
	public boolean shutdownUntil(long limitTimestamp)
		throws CacheControlStateException, WAInterruptedException
	{
		long now = timeNow();
		long delta = limitTimestamp - now;
		if (delta < 1)
			delta = -1;
		
		return internalShutdown(now, delta); 
	}
	
	// CCC
	protected boolean internalShutdown(long realWorldAnchorTime, long maxWaitVirtualMsec) 
		throws CacheControlStateException, WAInterruptedException
	{
		switch (controlState.get())
		{
			case NOT_STARTED:
			case RUNNING:
			case FLUSHING:
				break;
			case SHUTDOWN_IN_PROGRESS:
			case SHUTDOWN_COMPLETED:
				throw new CacheControlStateException(commonNamingPrefix, "cache cannot be shutdown because it has been shutdown already.");
		}
		
		// shutdown flag is one-way street, so we do not care if someone managed to change the value concurrently
		controlState.set(WBRBCacheControlState.SHUTDOWN_IN_PROGRESS); // set shutdown flag, everything should start spooling down ASAP
		
		logMessage(WBRBCacheMessage.SHUTDOWN_REQUESTED, null);
		
		try
		{
			// Initial delay after shutdown flag is set to ensure all further operations are prevented
			Thread.sleep(timeRealWorldInterval(
				Math.min(Math.max(maxWaitVirtualMsec, 10), // minimum 10 ms 
				Math.min(500, config.getMaxSleepTime()))));
			
			// NOTE: this logic is similar to the flush one, if making changes, check both places
			boolean fullSpooldown;
			while (true)
			{
				if (inflightMap.size() == 0)
				{
					fullSpooldown = true; // all inflight data was processed, indicate success
					break; 
				}
				
				long now = timeNow();
				long waitUntil = timeAddVirtualIntervalToRealWorldTime(
					realWorldAnchorTime, maxWaitVirtualMsec);
				
				if (waitUntil <= now)
				{
					logMessage(WBRBCacheMessage.SHUTDOWN_SPOOLDOWN_NOT_ACHIEVED, null, maxWaitVirtualMsec);
					fullSpooldown = false; // time expired and we still had data in-flight
					break; 
				}
				
				// need to wait
				long maxWaitUntil = timeAddVirtualIntervalToRealWorldTime(
					now, config.getMaxSleepTime()); // never wait more than max sleep time at a time to avoid issues due to shutdown or changing time factor
				
				waitUntil = Math.min(maxWaitUntil, waitUntil);
					
				long waitFor = waitUntil - now;
			
				Thread.sleep(waitFor);
			}
			
			// Shutdown all the threads
			this.readQueueProcessingThread.exitAsap();
			this.writeQueueProcessingThread.exitAsap();
			this.mainQueueProcessingThread.exitAsap();
			this.returnQueueProcessingThread.exitAsap();
			
			if (writeThreadPool != null)
				writeThreadPool.shutdown();
			if (readThreadPool != null)
				readThreadPool.shutdown();
			
			logMessage(WBRBCacheMessage.SHUTDOWN_COMPLETED, null, inflightMap.size());
			
			return fullSpooldown;
		} catch (InterruptedException e)
		{
			throw new WAInterruptedException(e);
		} catch (RuntimeException e)
		{
			if (e instanceof BaseCacheException)
				throw e;

			String msg = "ASSERTION FAILED: Failed shutdown: " + e;
			CacheInternalException e2 = new CacheInternalException(msg, e);
			try
			{
				logMessage(WBRBCacheMessage.ASSERTION_FAILED, e2, msg);
			} catch (Exception e3)
			{
				// Ignore this
			}
			throw e2;
		} finally
		{
			controlState.set(WBRBCacheControlState.SHUTDOWN_COMPLETED); // consider everything shut down
		}
	}
	

	@Override
	public boolean flushFor(long limitMillis)
		throws CacheControlStateException, WAInterruptedException
	{
		return internalFlush(timeNow(), limitMillis);
	}
	
	@Override
	public boolean flushUntil(long limitTimestamp)
		throws CacheControlStateException, WAInterruptedException
	{
		long now = timeNow();
		long delta = limitTimestamp - now;
		if (delta < 1)
			delta = -1;
		
		return internalFlush(now, delta); 
	}
	
	// CCC
	protected boolean internalFlush(long realWorldAnchorTime, long maxWaitVirtualMsec) 
		throws CacheControlStateException, WAInterruptedException
	{
		// use both switch and compareAndSet
		// switch is to get warnings if states are added
		// compareAndSet for correctness
		switch (controlState.get())
		{
			case RUNNING:
				break;
			case NOT_STARTED:
				throw new CacheControlStateException(commonNamingPrefix, "cache cannot be flushed because it was not yet started.");
			case FLUSHING:
				throw new CacheControlStateException(commonNamingPrefix, "cache cannot be flushed because it is already flushing.");
			case SHUTDOWN_IN_PROGRESS:
			case SHUTDOWN_COMPLETED:
				throw new CacheControlStateException(commonNamingPrefix, "cache cannot be flushed because it has been shutdown already.");
		}
		
		if (!controlState.compareAndSet(WBRBCacheControlState.RUNNING, WBRBCacheControlState.FLUSHING))
			throw new CacheControlStateException(commonNamingPrefix, "cache cannot be flushed because its control state was changed concurrently.");

		// The state will be set back to RUNNING in the finally block at the end
		try
		{
			logMessage(WBRBCacheMessage.FLUSH_REQUESTED, null);
			
			// Initial delay after flush flag is set to ensure all further operations are prevented
			Thread.sleep(timeRealWorldInterval(
				Math.min(Math.max(maxWaitVirtualMsec, 10), // minimum 10 ms 
				         Math.min(500, config.getMaxSleepTime()))
			));
			
			// NOTE: this logic is similar to the shutdown one, if making changes, check both places
			boolean fullSpooldown;
			while (true)
			{
				int remaining = inflightMap.size();
				if (remaining == 0)
				{
					logMessage(WBRBCacheMessage.FLUSH_SUCCESFULLY_COMPLETED, null);
					fullSpooldown = true; // all inflight data was processed, indicate success
					break; 
				}
				
				long now = timeNow();
				long waitUntil = timeAddVirtualIntervalToRealWorldTime(
					realWorldAnchorTime, maxWaitVirtualMsec);
				
				if (waitUntil <= now)
				{
					logMessage(WBRBCacheMessage.FLUSH_SPOOLDOWN_NOT_ACHIEVED, null, remaining, maxWaitVirtualMsec);
					fullSpooldown = false; // time expired and we still had data in-flight
					break; 
				}
				
				// need to wait
				long maxWaitUntil = timeAddVirtualIntervalToRealWorldTime(
					now, config.getMaxSleepTime()); // never wait more than max sleep time at a time to avoid issues due to shutdown or changing time factor
				
				waitUntil = Math.min(maxWaitUntil, waitUntil);
					
				long waitFor = waitUntil - now;
			
				Thread.sleep(waitFor);
			}
			
			return fullSpooldown;
		} catch (InterruptedException e)
		{
			throw new WAInterruptedException(e);
		} catch (RuntimeException e)
		{
			if (e instanceof BaseCacheException)
				throw e;

			String msg = "ASSERTION FAILED: Failed flush: " + e;
			CacheInternalException e2 = new CacheInternalException(msg, e);
			try
			{
				logMessage(WBRBCacheMessage.ASSERTION_FAILED, e2, msg);
			} catch (Exception e3)
			{
				// Ignore this
			}
			throw e2;
		}
		finally
		{
			// restore cache status to running if appropriate
			controlState.compareAndSet(WBRBCacheControlState.FLUSHING, WBRBCacheControlState.RUNNING);
		}
	}
	
	
	/**
	 * Gets cache status (e.g. for monitoring).
	 * 
	 * @param maximum age for the retrieved status -- previously calculated status
	 * 		is cached so it can be re-used if it is not older that the given
	 * 		maximum age; this age is in 'virtual' ms, i.e. affected by {@link #timeFactor()};
	 * 		use 0 to disable caching
	 * 
	 * @throws IllegalArgumentException if maximum age is negative
	 */
	public WBRBStatus getStatus(final long maximumAgeMsVirtual)
		throws IllegalArgumentException
	{
		if (maximumAgeMsVirtual < 0)
			throw new IllegalArgumentException("Maximum age must be non-negative, got: " + maximumAgeMsVirtual);
		
		final long now = timeNow();
		if (maximumAgeMsVirtual > 0)
		{
			WBRBStatus status = cachedStatus;
			if (status != null)
			{
				long validUntil = timeAddVirtualIntervalToRealWorldTime(
					status.getStatusCreatedAt(), maximumAgeMsVirtual);
				
				if (validUntil >= now)
					return status; 
			}
		}
		
		// Need new status; 
		// given that building status accesses a lot of contended stuff, 
		// synchronize to make sure this is not duplicated
		WBRBStats cacheStats = getStats();
		synchronized (cacheStats)
		{
			// Double-check if status was updated concurrently and is maybe valid now
			if (maximumAgeMsVirtual > 0)
			{
				WBRBStatus status = cachedStatus;
				if (status != null)
				{
					long validUntil = timeAddVirtualIntervalToRealWorldTime(
						status.getStatusCreatedAt(), maximumAgeMsVirtual);
					
					if (validUntil >= now)
						return status; 
				}
			}
			
			// Need to build a new status
			
			BooleanObjectWrapper everythingAlive = BooleanObjectWrapper.of(true);
			Function<@Nonnull Thread, @Nonnull Boolean> resetEverythingAliveIfThreadIsDead = new Function<@Nonnull Thread, @Nonnull Boolean>()
			{
				@Override
				public Boolean apply(Thread t)
				{
					boolean alive = t.isAlive();
					
					if (!alive)
					{
						everythingAlive.setFalse();
						return Boolean.FALSE;
					}
					else
						return Boolean.TRUE;
				}
			};
			Function<@Nullable WAThreadPoolExecutor, @Nonnull Boolean> resetEverythingAliveIfPoolIsDead = new Function<@Nullable WAThreadPoolExecutor, @Nonnull Boolean>()
			{
				@Override
				public Boolean apply(@Nullable WAThreadPoolExecutor pool)
				{
					if (pool == null) // null pool doesn't reset 'everything alive' flag
						return Boolean.FALSE; 
					
					boolean alive = !pool.isShutdown();
					
					if (!alive)
					{
						everythingAlive.setFalse();
						return Boolean.FALSE;
					}
					else
						return Boolean.TRUE;
				}
			};
			Function<@Nonnull Boolean, @Nonnull Boolean> resetEverythingAliveIfFalse = new Function<@Nonnull Boolean, @Nonnull Boolean>()
			{
				@Override
				public Boolean apply(Boolean alive)
				{
					if (!alive)
						everythingAlive.setFalse();
					
					return alive;
				}
			};
			
			long checkCachePreloadCacheFullExceptionCount = cacheStats.checkCachePreloadCacheFullExceptionCount.get();
			long checkCacheReadCacheFullExceptionCount = cacheStats.checkCacheReadCacheFullExceptionCount.get();
			
			long storageReadInitialAttempts = cacheStats.storageReadInitialAttempts.get();
			long storageReadRefreshAttempts = cacheStats.storageReadRefreshAttempts.get();
			long storageReadInitialFailures = cacheStats.storageReadInitialFailures.get();
			long storageReadRefreshFailures  = cacheStats.storageReadRefreshFailures.get();
			long storageReadInitialSuccesses = cacheStats.storageReadInitialSuccesses.get();
			long storageReadRefreshSuccesses  = cacheStats.storageReadRefreshSuccesses.get();
			
			long msgWarnCount = cacheStats.msgWarnCount.get();
			long msgExternalWarnCount = cacheStats.msgExternalWarnCount.get();
			long msgExternalErrorCount = cacheStats.msgExternalErrorCount.get();
			long msgExternalDataLossCount = cacheStats.msgExternalDataLossCount.get();
			long msgErrorCount = cacheStats.msgErrorCount.get();
			long msgFatalCount = cacheStats.msgFatalCount.get();
			
			WBRBCacheControlState currentControlState = getControlState();
			
			long[] lastTimestampMsgPerSeverityOrdinal = new long[cacheStats.lastTimestampMsgPerSeverityOrdinal.length];
			@Nullable String[] lastLoggedTextMsgPerSeverityOrdinal = new @Nullable String[lastTimestampMsgPerSeverityOrdinal.length];
			for (int i = 0; i < lastTimestampMsgPerSeverityOrdinal.length; i++)
			{
				lastTimestampMsgPerSeverityOrdinal[i] = cacheStats.lastTimestampMsgPerSeverityOrdinal[i].get();
				lastLoggedTextMsgPerSeverityOrdinal[i] = cacheStats.lastLoggedTextMsgPerSeverityOrdinal[i].get();
			}
			
			long lastWarnMsgTimestamp = 0;
			@Nullable String lastWarnLoggedMsgText = null;
			long lastErrorMsgTimestamp = 0;
			@Nullable String lastErrorLoggedMsgText = null;
			long lastFatalMsgTimestamp = 0;
			@Nullable String lastFatalLoggedMsgText = null;
			for (WBRBCacheMessageSeverity severity : WBRBCacheMessageSeverity.values())
			{
				int index = severity.ordinal();
				switch (severity)
				{
					case DEBUG:
					case EXTERNAL_INFO:
					case INFO:
						continue;
					case EXTERNAL_WARN:
					case WARN:
						{
							long ts = lastTimestampMsgPerSeverityOrdinal[index];
							if (ts > lastWarnMsgTimestamp)
							{
								lastWarnMsgTimestamp = ts;
								lastWarnLoggedMsgText = lastLoggedTextMsgPerSeverityOrdinal[index];
							}
						}
						continue;
					case ERROR:
					case EXTERNAL_DATA_LOSS:
					case EXTERNAL_ERROR:
						{
							long ts = lastTimestampMsgPerSeverityOrdinal[index];
							if (ts > lastErrorMsgTimestamp)
							{
								lastErrorMsgTimestamp = ts;
								lastErrorLoggedMsgText = lastLoggedTextMsgPerSeverityOrdinal[index];
							}
						}
						continue;
					case FATAL:
						{
							long ts = lastTimestampMsgPerSeverityOrdinal[index];
							if (ts > lastFatalMsgTimestamp)
							{
								lastFatalMsgTimestamp = ts;
								lastFatalLoggedMsgText = lastLoggedTextMsgPerSeverityOrdinal[index];
							}
						}
						continue;
				}
				
				logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), "code should not be reachable");
			}
			
			AtomicLong[] fullCyclesMonitor = cacheStats.fullCycleCountThresholdCounters;
			AtomicLong[] timeSinceLastAccessMonitor = cacheStats.timeSinceLastAccessThresholdCounters;
			
			WBRBStatus status = WBRBStatusBuilder
				.statusCreatedAt(now)
				.cacheAlive(resetEverythingAliveIfFalse.apply( isAlive()))
				.cacheUsable(isUsable())
				.cacheControlState(currentControlState)
				.cacheControlStateString(currentControlState.name())
				
				.readQueueProcessingThreadAlive(resetEverythingAliveIfThreadIsDead.apply(   readQueueProcessingThread))
				.writeQueueProcessingThreadAlive(resetEverythingAliveIfThreadIsDead.apply(  writeQueueProcessingThread))
				.mainQueueProcessingThreadAlive(resetEverythingAliveIfThreadIsDead.apply(   mainQueueProcessingThread))
				.returnQueueProcessingThreadAlive(resetEverythingAliveIfThreadIsDead.apply( returnQueueProcessingThread))
				.readThreadPoolAlive(resetEverythingAliveIfPoolIsDead.apply(  readThreadPool))
				.writeThreadPoolAlive(resetEverythingAliveIfPoolIsDead.apply( writeThreadPool))
				.readThreadPoolActiveThreads( readThreadPool == null  ? -1 : nn(readThreadPool).getActiveCount())
				.writeThreadPoolActiveThreads( writeThreadPool == null ? -1 : nn(writeThreadPool).getActiveCount())
				.everythingAlive(everythingAlive.isTrue())
				
				.currentCacheSize(inflightMap.mappingCount())
				.mainQueueSize(mainQueue.size())
				.returnQueueSize(returnQueue.size())
				.readQueueSize(readQueue.size())
				.writeQueueSize(writeQueue.size())
				
				.configMainQueueCacheTimeMs(config.getMainQueueCacheTime())
				.configReturnQueueCacheTimeMinMs(config.getReturnQueueCacheTimeMin())
				.configMainQueueMaxTargetSize(config.getMainQueueMaxTargetSize())
				.configMaxCacheElementsHardLimit(config.getMaxCacheElementsHardLimit())
				.configUntouchedItemCacheExpirationDelay(config.getUntouchedItemCacheExpirationDelay())
				.configMonitoringFullCacheCyclesThresholdMax(config.getMonitoringFullCacheCyclesThresholds().get(config.getMonitoringFullCacheCyclesThresholds().size() - 1))
				.configMonitoringTimeSinceAccessThresholdMax(config.getMonitoringTimeSinceAccessThresholds().get(config.getMonitoringTimeSinceAccessThresholds().size() - 1))
				
				.storageReadQueueProcessedItems(cacheStats.storageReadQueueProcessedItems.get())
				.storageReadTotalAttempts(storageReadInitialAttempts + storageReadRefreshAttempts)
				.storageReadTotalSuccesses(storageReadInitialSuccesses + storageReadRefreshSuccesses)
				.storageReadTotalFailures(storageReadInitialFailures + storageReadRefreshFailures)
				.storageReadRefreshAttempts(storageReadRefreshAttempts)
				.storageReadRefreshSuccesses(storageReadRefreshSuccesses)
				.storageReadRefreshFailures(storageReadRefreshFailures)
				.storageReadRefreshTooLateCount(cacheStats.storageReadRefreshTooLateCount.get())
				.storageReadRefreshDataNotUsedCount(cacheStats.storageReadRefreshDataNotUsedCount.get())
				.storageReadInitialAttempts(storageReadInitialAttempts)
				.storageReadInitialSuccesses(storageReadInitialSuccesses)
				.storageReadInitialFailures(storageReadInitialFailures)
				
				.storageWriteQueueProcessedItems(cacheStats.storageWriteQueueProcessedItems.get())
				.storageWriteAttempts(cacheStats.storageWriteAttempts.get())
				.storageWriteSuccesses(cacheStats.storageWriteSuccesses.get())
				.storageWriteFailures(cacheStats.storageWriteFailures.get())
				
				.mainQueueProcessedItems(cacheStats.mainQueueProcessedItems.get())
				.mainQueueLastItemInQueueDurationMs(cacheStats.mainQueueLastItemInQueueDurationMs.get())
				.mainQueueSentWrites(cacheStats.mainQueueSentWrites.get())
				.mainQueueExpiredFromCacheCount(cacheStats.mainQueueExpiredFromCacheCount.get())
				.mainQueueRemovedFromCacheCount(cacheStats.mainQueueRemovedFromCacheCount.get())
				.mainQueueRequeueToMainQueueCount(cacheStats.mainQueueRequeueToMainQueueCount.get())
				.mainQueueNotAllOkCount(cacheStats.mainQueueNotAllOkCount.get())
				
				.returnQueueProcessedItems(cacheStats.returnQueueProcessedItems.get())
				.returnQueueLastItemInQueueDurationMs(cacheStats.returnQueueLastItemInQueueDurationMs.get())
				.returnQueueScheduledResyncs(cacheStats.returnQueueScheduledResyncs.get())
				.returnQueueDoNothingCount(cacheStats.returnQueueDoNothingCount.get())
				.returnQueueExpiredFromCacheCount(cacheStats.returnQueueExpiredFromCacheCount.get())
				.returnQueueRemovedFromCacheCount(cacheStats.returnQueueRemovedFromCacheCount.get())
				.returnQueueRequeueToReturnQueueCount(cacheStats.returnQueueRequeueToReturnQueueCount.get())
				.returnQueueNegativeTimeSinceLastAccessErrorCount(cacheStats.returnQueueNegativeTimeSinceLastAccessErrorCount.get())
				.returnQueueItemNotRetainedDueToMainQueueSizeCount(cacheStats.returnQueueItemNotRetainedDueToMainQueueSizeCount.get())
				
				.checkCacheAttemptsNoDedup(cacheStats.checkCacheAttemptsNoDedup.get())
				.checkCachePreloadAttempts(cacheStats.checkCachePreloadAttempts.get())
				.checkCachePreloadCacheHit(cacheStats.checkCachePreloadCacheHit.get())
				.checkCachePreloadCacheFullExceptionCount(checkCachePreloadCacheFullExceptionCount)
				.checkCacheReadAttempts(cacheStats.checkCacheReadAttempts.get())
				.checkCacheReadCacheHit(cacheStats.checkCacheReadCacheHit.get())
				.checkCacheReadCacheFullExceptionCount(checkCacheReadCacheFullExceptionCount)
				.checkCacheTotalCacheFullExceptionCount(checkCachePreloadCacheFullExceptionCount + checkCacheReadCacheFullExceptionCount)
				.checkCacheNullKeyCount(cacheStats.checkCacheNullKeyCount.get())
				
				.cacheReadAttempts(cacheStats.cacheReadAttempts.get())
				.cacheReadTimeouts(cacheStats.cacheReadTimeouts.get())
				.cacheReadInterrupts(cacheStats.cacheReadInterrupts.get())
				.cacheReadErrors(cacheStats.cacheReadErrors.get())
				
				.cacheWriteAttempts(cacheStats.cacheWriteAttempts.get())
				.cacheWriteElementNotPresentCount(cacheStats.cacheWriteElementNotPresentCount.get())
				.cacheWriteErrors(cacheStats.cacheWriteErrors.get())
				.cacheWriteTooManyUpdates(cacheStats.cacheWriteTooManyUpdates.get())
				
				.msgWarnCount(msgWarnCount)
				.msgExternalWarnCount(msgExternalWarnCount)
				.msgExternalErrorCount(msgExternalErrorCount)
				.msgExternalDataLossCount(msgExternalDataLossCount)
				.msgErrorCount(msgErrorCount)
				.msgFatalCount(msgFatalCount)
				.msgTotalWarnOrHigherCount(msgWarnCount + msgExternalWarnCount + msgExternalErrorCount + msgExternalDataLossCount + msgErrorCount + msgFatalCount)
				.msgTotalErrorOrHigherCount(msgExternalErrorCount + msgExternalDataLossCount + msgErrorCount + msgFatalCount)
				
				.lastTimestampMsgPerSeverityOrdinal(lastTimestampMsgPerSeverityOrdinal)
				.lastLoggedTextMsgPerSeverityOrdinal(lastLoggedTextMsgPerSeverityOrdinal)
				.lastWarnMsgTimestamp(lastWarnMsgTimestamp)
				.lastWarnLoggedMsgText(lastWarnLoggedMsgText)
				.lastErrorMsgTimestamp(lastErrorMsgTimestamp)
				.lastErrorLoggedMsgText(lastErrorLoggedMsgText)
				.lastFatalMsgTimestamp(lastFatalMsgTimestamp)
				.lastFatalLoggedMsgText(lastFatalLoggedMsgText)
				
				.fullCycleCountThreshold1(fullCyclesMonitor[0].get())
				.fullCycleCountThreshold2(fullCyclesMonitor[1].get())
				.fullCycleCountThreshold3(fullCyclesMonitor[2].get())
				.fullCycleCountThreshold4(fullCyclesMonitor[3].get())
				.fullCycleCountThreshold5(fullCyclesMonitor[4].get())
				.fullCycleCountAboveAllThresholds(fullCyclesMonitor[5].get())
				
				.timeSinceAccessThreshold1(timeSinceLastAccessMonitor[0].get())
				.timeSinceAccessThreshold2(timeSinceLastAccessMonitor[1].get())
				.timeSinceAccessThreshold3(timeSinceLastAccessMonitor[2].get())
				.timeSinceAccessThreshold4(timeSinceLastAccessMonitor[3].get())
				.timeSinceAccessThreshold5(timeSinceLastAccessMonitor[4].get())
				.timeSinceAccessThresholdAboveAllThresholds(timeSinceLastAccessMonitor[5].get())
				
				.buildWBRBStatus();
			
			cachedStatus = status; // cache status
			return status;
		}
	}
}
