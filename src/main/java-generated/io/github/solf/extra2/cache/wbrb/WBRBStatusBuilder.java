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

import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.eclipse.jdt.annotation.NonNullByDefault;
import io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheControlState;
import io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import io.github.solf.extra2.codegenerate.stepbuilder.unused.UnusedInterface;

/**
 *  Step Builder class for {@link WBRBStatus}
 * <p>
 * {@link WriteBehindResyncInBackgroundCache} cache status (for e.g. monitoring).
 *
 *  @author Sergey Olefir
 */
@NonNullByDefault
@SuppressWarnings("unused")
public class WBRBStatusBuilder {

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_builder {

        public WBRBStatus buildWBRBStatus();
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg103 {

        /**
         * How many items were above all thresholds for
         * {@link WBRBConfig#getMonitoringTimeSinceAccessThresholds()}
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_builder timeSinceAccessThresholdAboveAllThresholds(long timeSinceAccessThresholdAboveAllThresholds);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg102 {

        /**
         * How many items were at or below threshold 5 for
         * {@link WBRBConfig#getMonitoringTimeSinceAccessThresholds()}
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg103 timeSinceAccessThreshold5(long timeSinceAccessThreshold5);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg101 {

        /**
         * How many items were at or below threshold 4 for
         * {@link WBRBConfig#getMonitoringTimeSinceAccessThresholds()}
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg102 timeSinceAccessThreshold4(long timeSinceAccessThreshold4);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg100 {

        /**
         * How many items were at or below threshold 3 for
         * {@link WBRBConfig#getMonitoringTimeSinceAccessThresholds()}
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg101 timeSinceAccessThreshold3(long timeSinceAccessThreshold3);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg99 {

        /**
         * How many items were at or below threshold 2 for
         * {@link WBRBConfig#getMonitoringTimeSinceAccessThresholds()}
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg100 timeSinceAccessThreshold2(long timeSinceAccessThreshold2);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg98 {

        /**
         * How many items were at or below threshold 1 for
         * {@link WBRBConfig#getMonitoringTimeSinceAccessThresholds()}
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg99 timeSinceAccessThreshold1(long timeSinceAccessThreshold1);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg97 {

        /**
         * How many items were above all thresholds for
         * {@link WBRBConfig#getMonitoringFullCacheCyclesThresholds()}
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg98 fullCycleCountAboveAllThresholds(long fullCycleCountAboveAllThresholds);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg96 {

        /**
         * How many items were at or below threshold 5 for
         * {@link WBRBConfig#getMonitoringFullCacheCyclesThresholds()}
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg97 fullCycleCountThreshold5(long fullCycleCountThreshold5);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg95 {

        /**
         * How many items were at or below threshold 4 for
         * {@link WBRBConfig#getMonitoringFullCacheCyclesThresholds()}
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg96 fullCycleCountThreshold4(long fullCycleCountThreshold4);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg94 {

        /**
         * How many items were at or below threshold 3 for
         * {@link WBRBConfig#getMonitoringFullCacheCyclesThresholds()}
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg95 fullCycleCountThreshold3(long fullCycleCountThreshold3);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg93 {

        /**
         * How many items were at or below threshold 2 for
         * {@link WBRBConfig#getMonitoringFullCacheCyclesThresholds()}
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg94 fullCycleCountThreshold2(long fullCycleCountThreshold2);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg92 {

        /**
         * How many items were at or below threshold 1 for
         * {@link WBRBConfig#getMonitoringFullCacheCyclesThresholds()}
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg93 fullCycleCountThreshold1(long fullCycleCountThreshold1);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg91 {

        /**
         * Last logged message text of the FATAL-type severity, null if no
         * such messages were logged (this does not track messages that were not
         * logged due to low severity or throttling).
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg92 lastFatalLoggedMsgText(@Nullable String lastFatalLoggedMsgText);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg90 {

        /**
         * Timestamp for the last message (regardless of whether it was logged)
         * of the FATAL-type severity, 0 if no such messages were logged.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg91 lastFatalMsgTimestamp(long lastFatalMsgTimestamp);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg89 {

        /**
         * Last logged message text of the ERROR-type severity, null if no
         * such messages were logged (this does not track messages that were not
         * logged due to low severity or throttling).
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg90 lastErrorLoggedMsgText(@Nullable String lastErrorLoggedMsgText);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg88 {

        /**
         * Timestamp for the last message (regardless of whether it was logged)
         * of the ERROR-type severity, 0 if no such messages were logged.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg89 lastErrorMsgTimestamp(long lastErrorMsgTimestamp);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg87 {

        /**
         * Last logged message text of the WARN-type severity, null if no
         * such messages were logged (this does not track messages that were not
         * logged due to low severity or throttling).
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg88 lastWarnLoggedMsgText(@Nullable String lastWarnLoggedMsgText);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg86 {

        /**
         * Timestamp for the last message (regardless of whether it was logged)
         * of the WARN-type severity, 0 if no such messages were logged.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg87 lastWarnMsgTimestamp(long lastWarnMsgTimestamp);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg85 {

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
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg86 lastLoggedTextMsgPerSeverityOrdinal(@Nullable String[] lastLoggedTextMsgPerSeverityOrdinal);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg84 {

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
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg85 lastTimestampMsgPerSeverityOrdinal(long[] lastTimestampMsgPerSeverityOrdinal);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg83 {

        /**
         * Total count of messages with severity 'error' or higher.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg84 msgTotalErrorOrHigherCount(long msgTotalErrorOrHigherCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg82 {

        /**
         * Total count of messages with severity 'warn' or higher.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg83 msgTotalWarnOrHigherCount(long msgTotalWarnOrHigherCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg81 {

        /**
         * Indicates a likely fatal error, meaning cache may well become unusable
         * after this happens.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg82 msgFatalCount(long msgFatalCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg80 {

        /**
         * Indicates an error which is likely to be caused by the
         * problems and/or unexpected behavior in the cache code itself.
         * <p>
         * Data loss is likely although this should not be fatal.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg81 msgErrorCount(long msgErrorCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg79 {

        /**
         * Indicates an error probably caused by external factors, such
         * as underlying storage failing.
         * <p>
         * This is used when data loss is highly likely, e.g. when cache implementation
         * gives up on writing piece of data to the underlying storage.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg80 msgExternalDataLossCount(long msgExternalDataLossCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg78 {

        /**
         * Indicates an error probably caused by external factors, such
         * as underlying storage failing.
         * <p>
         * These messages usually indicate that there was no data loss (yet).
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg79 msgExternalErrorCount(long msgExternalErrorCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg77 {

        /**
         * Indicates an externally-caused warning.
         * <p>
         * These messages usually indicate that there was no data loss (yet).
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg78 msgExternalWarnCount(long msgExternalWarnCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg76 {

        /**
         * Indicates problem that is probably caused by internal somewhat-known
         * factors, such as potential concurrency/race conditions (which normally
         * are not expected to occur).
         * <p>
         * These usually should not result in data loss.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg77 msgWarnCount(long msgWarnCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg75 {

        /**
         * How many times did we encounter {@link WBRBCacheMessage#TOO_MANY_CACHE_ELEMENT_UPDATES}
         * issue (which potentially leads to data loss).
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg76 cacheWriteTooManyUpdates(long cacheWriteTooManyUpdates);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg74 {

        /**
         * How many errors during cache write occurred.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg75 cacheWriteErrors(long cacheWriteErrors);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg73 {

        /**
         * How many cache writes failed because relevant cache element was not present.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg74 cacheWriteElementNotPresentCount(long cacheWriteElementNotPresentCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg72 {

        /**
         * How many cache write attempts were made.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg73 cacheWriteAttempts(long cacheWriteAttempts);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg71 {

        /**
         * How many errors during cache read occurred.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg72 cacheReadErrors(long cacheReadErrors);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg70 {

        /**
         * How many times cache reads were interrupted (externally).
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg71 cacheReadInterrupts(long cacheReadInterrupts);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg69 {

        /**
         * How many cache reads timed out (haven't got result in allowed time).
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg70 cacheReadTimeouts(long cacheReadTimeouts);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg68 {

        /**
         * How many cache read attempts were made.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg69 cacheReadAttempts(long cacheReadAttempts);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg67 {

        /**
         * How many check-cache (via read or preload) attempts hit the 'null key' error.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg68 checkCacheNullKeyCount(long checkCacheNullKeyCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg66 {

        /**
         * How many check-cache (via read or preload) attempts hit the 'cache full' state.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg67 checkCacheTotalCacheFullExceptionCount(long checkCacheTotalCacheFullExceptionCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg65 {

        /**
         * How many check-cache (via read) attempts hit the 'cache full' state.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg66 checkCacheReadCacheFullExceptionCount(long checkCacheReadCacheFullExceptionCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg64 {

        /**
         * How many check-cache (via read) attempts hit the cache.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg65 checkCacheReadCacheHit(long checkCacheReadCacheHit);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg63 {

        /**
         * How many check-cache (via read) attempts were made.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg64 checkCacheReadAttempts(long checkCacheReadAttempts);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg62 {

        /**
         * How many check-cache (via preload) attempts hit the 'cache full' state.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg63 checkCachePreloadCacheFullExceptionCount(long checkCachePreloadCacheFullExceptionCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg61 {

        /**
         * How many check-cache (via preload) attempts hit the cache.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg62 checkCachePreloadCacheHit(long checkCachePreloadCacheHit);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg60 {

        /**
         * How many check-cache (via preload) attempts were made.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg61 checkCachePreloadAttempts(long checkCachePreloadAttempts);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg59 {

        /**
         * How many check-cache attempts were made -- these are not separated
         * between preload & read and are not de-dupped -- one external operation
         * may result in many internal attempts.
         * <p>
         * This is basically for debugging/testing purposes.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg60 checkCacheAttemptsNoDedup(long checkCacheAttemptsNoDedup);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg58 {

        /**
         * In return queue processing there's decision as to whether to keep an
         * element in the cache; this monitors cases when item is ineligible to
         * be retained due to main queue size already being at the limit.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg59 returnQueueItemNotRetainedDueToMainQueueSizeCount(long returnQueueItemNotRetainedDueToMainQueueSizeCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg57 {

        /**
         * In return queue processing we calculate time since last access in
         * order to determine what to do with the cache item; this counts how
         * many times that resulted in the negative value (this should not
         * normally happen, but could possibly happen if time is adjusted or
         * some such).
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg58 returnQueueNegativeTimeSinceLastAccessErrorCount(long returnQueueNegativeTimeSinceLastAccessErrorCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg56 {

        /**
         * How many items were requeued back to the return queue
         * as the result of the return queue processing.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg57 returnQueueRequeueToReturnQueueCount(long returnQueueRequeueToReturnQueueCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg55 {

        /**
         * How many items were removed from cache (removal is generally an 'error',
         * non-error is 'expire') as the result of the return queue processing.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg56 returnQueueRemovedFromCacheCount(long returnQueueRemovedFromCacheCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg54 {

        /**
         * How many items were expired from cache as the result of the return queue processing.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg55 returnQueueExpiredFromCacheCount(long returnQueueExpiredFromCacheCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg53 {

        /**
         * How many items were processed as 'do nothing' as the result of the return queue processing.
         * <p>
         * This is usually not a normal behavior.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg54 returnQueueDoNothingCount(long returnQueueDoNothingCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg52 {

        /**
         * How many resyncs were scheduled while processing return queue.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg53 returnQueueScheduledResyncs(long returnQueueScheduledResyncs);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg51 {

        /**
         * For the last processed item in the return queue -- how long it actually
         * was in the queue (in virtual ms).
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg52 returnQueueLastItemInQueueDurationMs(long returnQueueLastItemInQueueDurationMs);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg50 {

        /**
         * How many items were processed out of return queue.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg51 returnQueueProcessedItems(long returnQueueProcessedItems);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg49 {

        /**
         * How many items weren't marked as 'all ok' as the result of main queue processing.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg50 mainQueueNotAllOkCount(long mainQueueNotAllOkCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg48 {

        /**
         * How many items were requeued back to the main processing queue
         * as the result of main queue processing.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg49 mainQueueRequeueToMainQueueCount(long mainQueueRequeueToMainQueueCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg47 {

        /**
         * How many items were removed from cache as the result of main queue processing.
         * <p>
         * 'removed' action indicates abnormal processing, an error is logged
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg48 mainQueueRemovedFromCacheCount(long mainQueueRemovedFromCacheCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg46 {

        /**
         * How many items were expired from cache as the result of main queue processing.
         * <p>
         * 'expired' action indicates normal processing, no error is logged
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg47 mainQueueExpiredFromCacheCount(long mainQueueExpiredFromCacheCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg45 {

        /**
         * How many writes were sent while processing main processing queue.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg46 mainQueueSentWrites(long mainQueueSentWrites);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg44 {

        /**
         * For the last processed item in the main queue -- how long it actually
         * was in the queue (in virtual ms).
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg45 mainQueueLastItemInQueueDurationMs(long mainQueueLastItemInQueueDurationMs);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg43 {

        /**
         * How many items were processed out of main processing queue.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg44 mainQueueProcessedItems(long mainQueueProcessedItems);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg42 {

        /**
         * How many writes failed (via {@link WriteBehindResyncInBackgroundCache#apiStorageWriteFail(Throwable, io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBWriteQueueEntry)}
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg43 storageWriteFailures(long storageWriteFailures);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg41 {

        /**
         * How many writes succeeded (via {@link WriteBehindResyncInBackgroundCache#apiStorageWriteSuccess(io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBWriteQueueEntry)}
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg42 storageWriteSuccesses(long storageWriteSuccesses);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg40 {

        /**
         * How many write attempts were made.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg41 storageWriteAttempts(long storageWriteAttempts);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg39 {

        /**
         * How many items were processed out of write queue.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg40 storageWriteQueueProcessedItems(long storageWriteQueueProcessedItems);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg38 {

        /**
         * How many reads (initial) failed (via {@link WriteBehindResyncInBackgroundCache#apiStorageReadFail(Throwable, WBRBCacheEntry)}
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg39 storageReadInitialFailures(long storageReadInitialFailures);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg37 {

        /**
         * How many reads (initial) succeeded (via {@link WriteBehindResyncInBackgroundCache#apiStorageReadSuccess(Object, WBRBCacheEntry)}
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg38 storageReadInitialSuccesses(long storageReadInitialSuccesses);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg36 {

        /**
         * How many read (initial) attempts were made.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg37 storageReadInitialAttempts(long storageReadInitialAttempts);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg35 {

        /**
         * How many reads (refresh) arrived but data was not used (not set/merged) for
         * whatever reason./
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg36 storageReadRefreshDataNotUsedCount(long storageReadRefreshDataNotUsedCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg34 {

        /**
         * How many reads (refresh) arrived too late for a proper resync.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg35 storageReadRefreshTooLateCount(long storageReadRefreshTooLateCount);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg33 {

        /**
         * How many reads (refresh) failed (via {@link WriteBehindResyncInBackgroundCache#apiStorageReadFail(Throwable, WBRBCacheEntry)}
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg34 storageReadRefreshFailures(long storageReadRefreshFailures);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg32 {

        /**
         * How many reads (refresh) succeeded (via {@link WriteBehindResyncInBackgroundCache#apiStorageReadSuccess(Object, WBRBCacheEntry)}
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg33 storageReadRefreshSuccesses(long storageReadRefreshSuccesses);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg31 {

        /**
         * How many read (refresh) attempts were made.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg32 storageReadRefreshAttempts(long storageReadRefreshAttempts);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg30 {

        /**
         * How many reads (total, initial + refresh) failed (via {@link WriteBehindResyncInBackgroundCache#apiStorageReadFail(Throwable, WBRBCacheEntry)}
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg31 storageReadTotalFailures(long storageReadTotalFailures);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg29 {

        /**
         * How many reads (total, initial + refresh) succeeded (via {@link WriteBehindResyncInBackgroundCache#apiStorageReadSuccess(Object, WBRBCacheEntry)}
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg30 storageReadTotalSuccesses(long storageReadTotalSuccesses);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg28 {

        /**
         * How many read attempts (total, initial + refresh) were made.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg29 storageReadTotalAttempts(long storageReadTotalAttempts);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg27 {

        /**
         * How many items were processed out of read queue.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg28 storageReadQueueProcessedItems(long storageReadQueueProcessedItems);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg26 {

        /**
         * The value of the highest threshold in {@link WBRBConfig#getMonitoringTimeSinceAccessThresholds()}
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg27 configMonitoringTimeSinceAccessThresholdMax(long configMonitoringTimeSinceAccessThresholdMax);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg25 {

        /**
         * The value of the highest threshold in {@link WBRBConfig#getMonitoringFullCacheCyclesThresholds()}
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg26 configMonitoringFullCacheCyclesThresholdMax(int configMonitoringFullCacheCyclesThresholdMax);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg24 {

        /**
         * 'untouched' item cache expiration delay (as configured)
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg25 configUntouchedItemCacheExpirationDelay(long configUntouchedItemCacheExpirationDelay);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg23 {

        /**
         * Hard limit on the cache size (as configured)
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg24 configMaxCacheElementsHardLimit(long configMaxCacheElementsHardLimit);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg22 {

        /**
         * Maximum allowed targeted (it's possible to exceed this value) queue size
         * for the main processing queue (as configured)
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg23 configMainQueueMaxTargetSize(long configMainQueueMaxTargetSize);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg21 {

        /**
         * Return queue minimum/target cache time (in ms) (as configured)
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg22 configReturnQueueCacheTimeMinMs(long configReturnQueueCacheTimeMinMs);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg20 {

        /**
         * Main queue target cache time (in ms) (as configured)
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg21 configMainQueueCacheTimeMs(long configMainQueueCacheTimeMs);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg19 {

        /**
         * Write queue size.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg20 writeQueueSize(long writeQueueSize);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg18 {

        /**
         * Read queue size.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg19 readQueueSize(long readQueueSize);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg17 {

        /**
         * Return queue size.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg18 returnQueueSize(long returnQueueSize);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg16 {

        /**
         * Main processing queue size.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg17 mainQueueSize(long mainQueueSize);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg15 {

        /**
         * Current cache size.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg16 currentCacheSize(long currentCacheSize);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg14 {

        /**
         * Whether cache itself AND all the threads & thread pools required for the
         * cache operation are still alive; if pools don't exist, they don't affect this value.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg15 everythingAlive(boolean everythingAlive);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg13 {

        /**
         * Number of currently active threads in the pool; -1 if there's no pool.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg14 writeThreadPoolActiveThreads(int writeThreadPoolActiveThreads);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg12 {

        /**
         * Number of currently active threads in the pool; -1 if there's no pool.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg13 readThreadPoolActiveThreads(int readThreadPoolActiveThreads);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg11 {

        /**
         * Whether thread pool is alive (false if there's no pool).
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg12 writeThreadPoolAlive(boolean writeThreadPoolAlive);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg10 {

        /**
         * Whether thread pool is alive (false if there's no pool).
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg11 readThreadPoolAlive(boolean readThreadPoolAlive);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg9 {

        /**
         * Whether thread is alive.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg10 returnQueueProcessingThreadAlive(boolean returnQueueProcessingThreadAlive);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg8 {

        /**
         * Whether thread is alive.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg9 mainQueueProcessingThreadAlive(boolean mainQueueProcessingThreadAlive);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg7 {

        /**
         * Whether thread is alive.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg8 writeQueueProcessingThreadAlive(boolean writeQueueProcessingThreadAlive);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg6 {

        /**
         * Whether thread is alive.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg7 readQueueProcessingThreadAlive(boolean readQueueProcessingThreadAlive);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg5 {

        /**
         * Current cache control state as String (from WBRBCacheControlState) -- NOT_STARTED, RUNNING, SHUTDOWN...
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg6 cacheControlStateString(String cacheControlStateString);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg4 {

        /**
         * Current cache control state as enum -- NOT_STARTED, RUNNING, SHUTDOWN...
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg5 cacheControlState(WBRBCacheControlState cacheControlState);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg3 {

        /**
         * Whether cache is usable -- that is standard read & write operations
         * can be performed; this can differ from {@link #isAlive()} value for various
         * reasons such as cache flush.
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg4 cacheUsable(boolean cacheUsable);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg2 {

        /**
         * Whether cache is alive (that is it was started and not stopped yet).
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg3 cacheAlive(boolean cacheAlive);
    }

    public interface ZBSI_WBRBStatusBuilder_statusCreatedAt_arg1 {

        /**
         * Indicates when status was created (e.g. for caching purposes).
         */
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg2 statusCreatedAt(long statusCreatedAt);
    }

    private static final class ZBSI_WBRBStatusBuilder_statusCreatedAt_builderClass implements ZBSI_WBRBStatusBuilder_statusCreatedAt_builder, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg103, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg102, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg101, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg100, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg99, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg98, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg97, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg96, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg95, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg94, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg93, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg92, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg91, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg90, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg89, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg88, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg87, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg86, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg85, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg84, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg83, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg82, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg81, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg80, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg79, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg78, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg77, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg76, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg75, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg74, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg73, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg72, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg71, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg70, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg69, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg68, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg67, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg66, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg65, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg64, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg63, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg62, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg61, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg60, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg59, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg58, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg57, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg56, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg55, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg54, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg53, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg52, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg51, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg50, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg49, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg48, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg47, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg46, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg45, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg44, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg43, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg42, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg41, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg40, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg39, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg38, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg37, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg36, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg35, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg34, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg33, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg32, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg31, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg30, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg29, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg28, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg27, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg26, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg25, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg24, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg23, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg22, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg21, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg20, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg19, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg18, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg17, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg16, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg15, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg14, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg13, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg12, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg11, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg10, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg9, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg8, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg7, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg6, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg5, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg4, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg3, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg2, ZBSI_WBRBStatusBuilder_statusCreatedAt_arg1 {

        @SuppressWarnings("all")
        private long timeSinceAccessThresholdAboveAllThresholds;

        @SuppressWarnings("all")
        private long timeSinceAccessThreshold5;

        @SuppressWarnings("all")
        private long timeSinceAccessThreshold4;

        @SuppressWarnings("all")
        private long timeSinceAccessThreshold3;

        @SuppressWarnings("all")
        private long timeSinceAccessThreshold2;

        @SuppressWarnings("all")
        private long timeSinceAccessThreshold1;

        @SuppressWarnings("all")
        private long fullCycleCountAboveAllThresholds;

        @SuppressWarnings("all")
        private long fullCycleCountThreshold5;

        @SuppressWarnings("all")
        private long fullCycleCountThreshold4;

        @SuppressWarnings("all")
        private long fullCycleCountThreshold3;

        @SuppressWarnings("all")
        private long fullCycleCountThreshold2;

        @SuppressWarnings("all")
        private long fullCycleCountThreshold1;

        @Nullable
        @SuppressWarnings("all")
        private String lastFatalLoggedMsgText;

        @SuppressWarnings("all")
        private long lastFatalMsgTimestamp;

        @Nullable
        @SuppressWarnings("all")
        private String lastErrorLoggedMsgText;

        @SuppressWarnings("all")
        private long lastErrorMsgTimestamp;

        @Nullable
        @SuppressWarnings("all")
        private String lastWarnLoggedMsgText;

        @SuppressWarnings("all")
        private long lastWarnMsgTimestamp;

        @Nullable
        @SuppressWarnings("all")
        private String[] lastLoggedTextMsgPerSeverityOrdinal;

        @SuppressWarnings("all")
        private long[] lastTimestampMsgPerSeverityOrdinal;

        @SuppressWarnings("all")
        private long msgTotalErrorOrHigherCount;

        @SuppressWarnings("all")
        private long msgTotalWarnOrHigherCount;

        @SuppressWarnings("all")
        private long msgFatalCount;

        @SuppressWarnings("all")
        private long msgErrorCount;

        @SuppressWarnings("all")
        private long msgExternalDataLossCount;

        @SuppressWarnings("all")
        private long msgExternalErrorCount;

        @SuppressWarnings("all")
        private long msgExternalWarnCount;

        @SuppressWarnings("all")
        private long msgWarnCount;

        @SuppressWarnings("all")
        private long cacheWriteTooManyUpdates;

        @SuppressWarnings("all")
        private long cacheWriteErrors;

        @SuppressWarnings("all")
        private long cacheWriteElementNotPresentCount;

        @SuppressWarnings("all")
        private long cacheWriteAttempts;

        @SuppressWarnings("all")
        private long cacheReadErrors;

        @SuppressWarnings("all")
        private long cacheReadInterrupts;

        @SuppressWarnings("all")
        private long cacheReadTimeouts;

        @SuppressWarnings("all")
        private long cacheReadAttempts;

        @SuppressWarnings("all")
        private long checkCacheNullKeyCount;

        @SuppressWarnings("all")
        private long checkCacheTotalCacheFullExceptionCount;

        @SuppressWarnings("all")
        private long checkCacheReadCacheFullExceptionCount;

        @SuppressWarnings("all")
        private long checkCacheReadCacheHit;

        @SuppressWarnings("all")
        private long checkCacheReadAttempts;

        @SuppressWarnings("all")
        private long checkCachePreloadCacheFullExceptionCount;

        @SuppressWarnings("all")
        private long checkCachePreloadCacheHit;

        @SuppressWarnings("all")
        private long checkCachePreloadAttempts;

        @SuppressWarnings("all")
        private long checkCacheAttemptsNoDedup;

        @SuppressWarnings("all")
        private long returnQueueItemNotRetainedDueToMainQueueSizeCount;

        @SuppressWarnings("all")
        private long returnQueueNegativeTimeSinceLastAccessErrorCount;

        @SuppressWarnings("all")
        private long returnQueueRequeueToReturnQueueCount;

        @SuppressWarnings("all")
        private long returnQueueRemovedFromCacheCount;

        @SuppressWarnings("all")
        private long returnQueueExpiredFromCacheCount;

        @SuppressWarnings("all")
        private long returnQueueDoNothingCount;

        @SuppressWarnings("all")
        private long returnQueueScheduledResyncs;

        @SuppressWarnings("all")
        private long returnQueueLastItemInQueueDurationMs;

        @SuppressWarnings("all")
        private long returnQueueProcessedItems;

        @SuppressWarnings("all")
        private long mainQueueNotAllOkCount;

        @SuppressWarnings("all")
        private long mainQueueRequeueToMainQueueCount;

        @SuppressWarnings("all")
        private long mainQueueRemovedFromCacheCount;

        @SuppressWarnings("all")
        private long mainQueueExpiredFromCacheCount;

        @SuppressWarnings("all")
        private long mainQueueSentWrites;

        @SuppressWarnings("all")
        private long mainQueueLastItemInQueueDurationMs;

        @SuppressWarnings("all")
        private long mainQueueProcessedItems;

        @SuppressWarnings("all")
        private long storageWriteFailures;

        @SuppressWarnings("all")
        private long storageWriteSuccesses;

        @SuppressWarnings("all")
        private long storageWriteAttempts;

        @SuppressWarnings("all")
        private long storageWriteQueueProcessedItems;

        @SuppressWarnings("all")
        private long storageReadInitialFailures;

        @SuppressWarnings("all")
        private long storageReadInitialSuccesses;

        @SuppressWarnings("all")
        private long storageReadInitialAttempts;

        @SuppressWarnings("all")
        private long storageReadRefreshDataNotUsedCount;

        @SuppressWarnings("all")
        private long storageReadRefreshTooLateCount;

        @SuppressWarnings("all")
        private long storageReadRefreshFailures;

        @SuppressWarnings("all")
        private long storageReadRefreshSuccesses;

        @SuppressWarnings("all")
        private long storageReadRefreshAttempts;

        @SuppressWarnings("all")
        private long storageReadTotalFailures;

        @SuppressWarnings("all")
        private long storageReadTotalSuccesses;

        @SuppressWarnings("all")
        private long storageReadTotalAttempts;

        @SuppressWarnings("all")
        private long storageReadQueueProcessedItems;

        @SuppressWarnings("all")
        private long configMonitoringTimeSinceAccessThresholdMax;

        @SuppressWarnings("all")
        private int configMonitoringFullCacheCyclesThresholdMax;

        @SuppressWarnings("all")
        private long configUntouchedItemCacheExpirationDelay;

        @SuppressWarnings("all")
        private long configMaxCacheElementsHardLimit;

        @SuppressWarnings("all")
        private long configMainQueueMaxTargetSize;

        @SuppressWarnings("all")
        private long configReturnQueueCacheTimeMinMs;

        @SuppressWarnings("all")
        private long configMainQueueCacheTimeMs;

        @SuppressWarnings("all")
        private long writeQueueSize;

        @SuppressWarnings("all")
        private long readQueueSize;

        @SuppressWarnings("all")
        private long returnQueueSize;

        @SuppressWarnings("all")
        private long mainQueueSize;

        @SuppressWarnings("all")
        private long currentCacheSize;

        @SuppressWarnings("all")
        private boolean everythingAlive;

        @SuppressWarnings("all")
        private int writeThreadPoolActiveThreads;

        @SuppressWarnings("all")
        private int readThreadPoolActiveThreads;

        @SuppressWarnings("all")
        private boolean writeThreadPoolAlive;

        @SuppressWarnings("all")
        private boolean readThreadPoolAlive;

        @SuppressWarnings("all")
        private boolean returnQueueProcessingThreadAlive;

        @SuppressWarnings("all")
        private boolean mainQueueProcessingThreadAlive;

        @SuppressWarnings("all")
        private boolean writeQueueProcessingThreadAlive;

        @SuppressWarnings("all")
        private boolean readQueueProcessingThreadAlive;

        @SuppressWarnings("all")
        private String cacheControlStateString;

        @SuppressWarnings("all")
        private WBRBCacheControlState cacheControlState;

        @SuppressWarnings("all")
        private boolean cacheUsable;

        @SuppressWarnings("all")
        private boolean cacheAlive;

        @SuppressWarnings("all")
        private long statusCreatedAt;

        /**
         * How many items were above all thresholds for
         * {@link WBRBConfig#getMonitoringTimeSinceAccessThresholds()}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_builder timeSinceAccessThresholdAboveAllThresholds(long timeSinceAccessThresholdAboveAllThresholds) {
            this.timeSinceAccessThresholdAboveAllThresholds = timeSinceAccessThresholdAboveAllThresholds;
            return this;
        }

        /**
         * How many items were at or below threshold 5 for
         * {@link WBRBConfig#getMonitoringTimeSinceAccessThresholds()}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg103 timeSinceAccessThreshold5(long timeSinceAccessThreshold5) {
            this.timeSinceAccessThreshold5 = timeSinceAccessThreshold5;
            return this;
        }

        /**
         * How many items were at or below threshold 4 for
         * {@link WBRBConfig#getMonitoringTimeSinceAccessThresholds()}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg102 timeSinceAccessThreshold4(long timeSinceAccessThreshold4) {
            this.timeSinceAccessThreshold4 = timeSinceAccessThreshold4;
            return this;
        }

        /**
         * How many items were at or below threshold 3 for
         * {@link WBRBConfig#getMonitoringTimeSinceAccessThresholds()}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg101 timeSinceAccessThreshold3(long timeSinceAccessThreshold3) {
            this.timeSinceAccessThreshold3 = timeSinceAccessThreshold3;
            return this;
        }

        /**
         * How many items were at or below threshold 2 for
         * {@link WBRBConfig#getMonitoringTimeSinceAccessThresholds()}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg100 timeSinceAccessThreshold2(long timeSinceAccessThreshold2) {
            this.timeSinceAccessThreshold2 = timeSinceAccessThreshold2;
            return this;
        }

        /**
         * How many items were at or below threshold 1 for
         * {@link WBRBConfig#getMonitoringTimeSinceAccessThresholds()}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg99 timeSinceAccessThreshold1(long timeSinceAccessThreshold1) {
            this.timeSinceAccessThreshold1 = timeSinceAccessThreshold1;
            return this;
        }

        /**
         * How many items were above all thresholds for
         * {@link WBRBConfig#getMonitoringFullCacheCyclesThresholds()}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg98 fullCycleCountAboveAllThresholds(long fullCycleCountAboveAllThresholds) {
            this.fullCycleCountAboveAllThresholds = fullCycleCountAboveAllThresholds;
            return this;
        }

        /**
         * How many items were at or below threshold 5 for
         * {@link WBRBConfig#getMonitoringFullCacheCyclesThresholds()}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg97 fullCycleCountThreshold5(long fullCycleCountThreshold5) {
            this.fullCycleCountThreshold5 = fullCycleCountThreshold5;
            return this;
        }

        /**
         * How many items were at or below threshold 4 for
         * {@link WBRBConfig#getMonitoringFullCacheCyclesThresholds()}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg96 fullCycleCountThreshold4(long fullCycleCountThreshold4) {
            this.fullCycleCountThreshold4 = fullCycleCountThreshold4;
            return this;
        }

        /**
         * How many items were at or below threshold 3 for
         * {@link WBRBConfig#getMonitoringFullCacheCyclesThresholds()}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg95 fullCycleCountThreshold3(long fullCycleCountThreshold3) {
            this.fullCycleCountThreshold3 = fullCycleCountThreshold3;
            return this;
        }

        /**
         * How many items were at or below threshold 2 for
         * {@link WBRBConfig#getMonitoringFullCacheCyclesThresholds()}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg94 fullCycleCountThreshold2(long fullCycleCountThreshold2) {
            this.fullCycleCountThreshold2 = fullCycleCountThreshold2;
            return this;
        }

        /**
         * How many items were at or below threshold 1 for
         * {@link WBRBConfig#getMonitoringFullCacheCyclesThresholds()}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg93 fullCycleCountThreshold1(long fullCycleCountThreshold1) {
            this.fullCycleCountThreshold1 = fullCycleCountThreshold1;
            return this;
        }

        /**
         * Last logged message text of the FATAL-type severity, null if no
         * such messages were logged (this does not track messages that were not
         * logged due to low severity or throttling).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg92 lastFatalLoggedMsgText(@Nullable String lastFatalLoggedMsgText) {
            this.lastFatalLoggedMsgText = lastFatalLoggedMsgText;
            return this;
        }

        /**
         * Timestamp for the last message (regardless of whether it was logged)
         * of the FATAL-type severity, 0 if no such messages were logged.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg91 lastFatalMsgTimestamp(long lastFatalMsgTimestamp) {
            this.lastFatalMsgTimestamp = lastFatalMsgTimestamp;
            return this;
        }

        /**
         * Last logged message text of the ERROR-type severity, null if no
         * such messages were logged (this does not track messages that were not
         * logged due to low severity or throttling).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg90 lastErrorLoggedMsgText(@Nullable String lastErrorLoggedMsgText) {
            this.lastErrorLoggedMsgText = lastErrorLoggedMsgText;
            return this;
        }

        /**
         * Timestamp for the last message (regardless of whether it was logged)
         * of the ERROR-type severity, 0 if no such messages were logged.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg89 lastErrorMsgTimestamp(long lastErrorMsgTimestamp) {
            this.lastErrorMsgTimestamp = lastErrorMsgTimestamp;
            return this;
        }

        /**
         * Last logged message text of the WARN-type severity, null if no
         * such messages were logged (this does not track messages that were not
         * logged due to low severity or throttling).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg88 lastWarnLoggedMsgText(@Nullable String lastWarnLoggedMsgText) {
            this.lastWarnLoggedMsgText = lastWarnLoggedMsgText;
            return this;
        }

        /**
         * Timestamp for the last message (regardless of whether it was logged)
         * of the WARN-type severity, 0 if no such messages were logged.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg87 lastWarnMsgTimestamp(long lastWarnMsgTimestamp) {
            this.lastWarnMsgTimestamp = lastWarnMsgTimestamp;
            return this;
        }

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
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg86 lastLoggedTextMsgPerSeverityOrdinal(@Nullable String[] lastLoggedTextMsgPerSeverityOrdinal) {
            this.lastLoggedTextMsgPerSeverityOrdinal = lastLoggedTextMsgPerSeverityOrdinal;
            return this;
        }

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
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg85 lastTimestampMsgPerSeverityOrdinal(long[] lastTimestampMsgPerSeverityOrdinal) {
            this.lastTimestampMsgPerSeverityOrdinal = lastTimestampMsgPerSeverityOrdinal;
            return this;
        }

        /**
         * Total count of messages with severity 'error' or higher.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg84 msgTotalErrorOrHigherCount(long msgTotalErrorOrHigherCount) {
            this.msgTotalErrorOrHigherCount = msgTotalErrorOrHigherCount;
            return this;
        }

        /**
         * Total count of messages with severity 'warn' or higher.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg83 msgTotalWarnOrHigherCount(long msgTotalWarnOrHigherCount) {
            this.msgTotalWarnOrHigherCount = msgTotalWarnOrHigherCount;
            return this;
        }

        /**
         * Indicates a likely fatal error, meaning cache may well become unusable
         * after this happens.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg82 msgFatalCount(long msgFatalCount) {
            this.msgFatalCount = msgFatalCount;
            return this;
        }

        /**
         * Indicates an error which is likely to be caused by the
         * problems and/or unexpected behavior in the cache code itself.
         * <p>
         * Data loss is likely although this should not be fatal.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg81 msgErrorCount(long msgErrorCount) {
            this.msgErrorCount = msgErrorCount;
            return this;
        }

        /**
         * Indicates an error probably caused by external factors, such
         * as underlying storage failing.
         * <p>
         * This is used when data loss is highly likely, e.g. when cache implementation
         * gives up on writing piece of data to the underlying storage.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg80 msgExternalDataLossCount(long msgExternalDataLossCount) {
            this.msgExternalDataLossCount = msgExternalDataLossCount;
            return this;
        }

        /**
         * Indicates an error probably caused by external factors, such
         * as underlying storage failing.
         * <p>
         * These messages usually indicate that there was no data loss (yet).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg79 msgExternalErrorCount(long msgExternalErrorCount) {
            this.msgExternalErrorCount = msgExternalErrorCount;
            return this;
        }

        /**
         * Indicates an externally-caused warning.
         * <p>
         * These messages usually indicate that there was no data loss (yet).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg78 msgExternalWarnCount(long msgExternalWarnCount) {
            this.msgExternalWarnCount = msgExternalWarnCount;
            return this;
        }

        /**
         * Indicates problem that is probably caused by internal somewhat-known
         * factors, such as potential concurrency/race conditions (which normally
         * are not expected to occur).
         * <p>
         * These usually should not result in data loss.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg77 msgWarnCount(long msgWarnCount) {
            this.msgWarnCount = msgWarnCount;
            return this;
        }

        /**
         * How many times did we encounter {@link WBRBCacheMessage#TOO_MANY_CACHE_ELEMENT_UPDATES}
         * issue (which potentially leads to data loss).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg76 cacheWriteTooManyUpdates(long cacheWriteTooManyUpdates) {
            this.cacheWriteTooManyUpdates = cacheWriteTooManyUpdates;
            return this;
        }

        /**
         * How many errors during cache write occurred.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg75 cacheWriteErrors(long cacheWriteErrors) {
            this.cacheWriteErrors = cacheWriteErrors;
            return this;
        }

        /**
         * How many cache writes failed because relevant cache element was not present.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg74 cacheWriteElementNotPresentCount(long cacheWriteElementNotPresentCount) {
            this.cacheWriteElementNotPresentCount = cacheWriteElementNotPresentCount;
            return this;
        }

        /**
         * How many cache write attempts were made.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg73 cacheWriteAttempts(long cacheWriteAttempts) {
            this.cacheWriteAttempts = cacheWriteAttempts;
            return this;
        }

        /**
         * How many errors during cache read occurred.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg72 cacheReadErrors(long cacheReadErrors) {
            this.cacheReadErrors = cacheReadErrors;
            return this;
        }

        /**
         * How many times cache reads were interrupted (externally).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg71 cacheReadInterrupts(long cacheReadInterrupts) {
            this.cacheReadInterrupts = cacheReadInterrupts;
            return this;
        }

        /**
         * How many cache reads timed out (haven't got result in allowed time).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg70 cacheReadTimeouts(long cacheReadTimeouts) {
            this.cacheReadTimeouts = cacheReadTimeouts;
            return this;
        }

        /**
         * How many cache read attempts were made.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg69 cacheReadAttempts(long cacheReadAttempts) {
            this.cacheReadAttempts = cacheReadAttempts;
            return this;
        }

        /**
         * How many check-cache (via read or preload) attempts hit the 'null key' error.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg68 checkCacheNullKeyCount(long checkCacheNullKeyCount) {
            this.checkCacheNullKeyCount = checkCacheNullKeyCount;
            return this;
        }

        /**
         * How many check-cache (via read or preload) attempts hit the 'cache full' state.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg67 checkCacheTotalCacheFullExceptionCount(long checkCacheTotalCacheFullExceptionCount) {
            this.checkCacheTotalCacheFullExceptionCount = checkCacheTotalCacheFullExceptionCount;
            return this;
        }

        /**
         * How many check-cache (via read) attempts hit the 'cache full' state.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg66 checkCacheReadCacheFullExceptionCount(long checkCacheReadCacheFullExceptionCount) {
            this.checkCacheReadCacheFullExceptionCount = checkCacheReadCacheFullExceptionCount;
            return this;
        }

        /**
         * How many check-cache (via read) attempts hit the cache.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg65 checkCacheReadCacheHit(long checkCacheReadCacheHit) {
            this.checkCacheReadCacheHit = checkCacheReadCacheHit;
            return this;
        }

        /**
         * How many check-cache (via read) attempts were made.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg64 checkCacheReadAttempts(long checkCacheReadAttempts) {
            this.checkCacheReadAttempts = checkCacheReadAttempts;
            return this;
        }

        /**
         * How many check-cache (via preload) attempts hit the 'cache full' state.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg63 checkCachePreloadCacheFullExceptionCount(long checkCachePreloadCacheFullExceptionCount) {
            this.checkCachePreloadCacheFullExceptionCount = checkCachePreloadCacheFullExceptionCount;
            return this;
        }

        /**
         * How many check-cache (via preload) attempts hit the cache.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg62 checkCachePreloadCacheHit(long checkCachePreloadCacheHit) {
            this.checkCachePreloadCacheHit = checkCachePreloadCacheHit;
            return this;
        }

        /**
         * How many check-cache (via preload) attempts were made.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg61 checkCachePreloadAttempts(long checkCachePreloadAttempts) {
            this.checkCachePreloadAttempts = checkCachePreloadAttempts;
            return this;
        }

        /**
         * How many check-cache attempts were made -- these are not separated
         * between preload & read and are not de-dupped -- one external operation
         * may result in many internal attempts.
         * <p>
         * This is basically for debugging/testing purposes.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg60 checkCacheAttemptsNoDedup(long checkCacheAttemptsNoDedup) {
            this.checkCacheAttemptsNoDedup = checkCacheAttemptsNoDedup;
            return this;
        }

        /**
         * In return queue processing there's decision as to whether to keep an
         * element in the cache; this monitors cases when item is ineligible to
         * be retained due to main queue size already being at the limit.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg59 returnQueueItemNotRetainedDueToMainQueueSizeCount(long returnQueueItemNotRetainedDueToMainQueueSizeCount) {
            this.returnQueueItemNotRetainedDueToMainQueueSizeCount = returnQueueItemNotRetainedDueToMainQueueSizeCount;
            return this;
        }

        /**
         * In return queue processing we calculate time since last access in
         * order to determine what to do with the cache item; this counts how
         * many times that resulted in the negative value (this should not
         * normally happen, but could possibly happen if time is adjusted or
         * some such).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg58 returnQueueNegativeTimeSinceLastAccessErrorCount(long returnQueueNegativeTimeSinceLastAccessErrorCount) {
            this.returnQueueNegativeTimeSinceLastAccessErrorCount = returnQueueNegativeTimeSinceLastAccessErrorCount;
            return this;
        }

        /**
         * How many items were requeued back to the return queue
         * as the result of the return queue processing.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg57 returnQueueRequeueToReturnQueueCount(long returnQueueRequeueToReturnQueueCount) {
            this.returnQueueRequeueToReturnQueueCount = returnQueueRequeueToReturnQueueCount;
            return this;
        }

        /**
         * How many items were removed from cache (removal is generally an 'error',
         * non-error is 'expire') as the result of the return queue processing.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg56 returnQueueRemovedFromCacheCount(long returnQueueRemovedFromCacheCount) {
            this.returnQueueRemovedFromCacheCount = returnQueueRemovedFromCacheCount;
            return this;
        }

        /**
         * How many items were expired from cache as the result of the return queue processing.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg55 returnQueueExpiredFromCacheCount(long returnQueueExpiredFromCacheCount) {
            this.returnQueueExpiredFromCacheCount = returnQueueExpiredFromCacheCount;
            return this;
        }

        /**
         * How many items were processed as 'do nothing' as the result of the return queue processing.
         * <p>
         * This is usually not a normal behavior.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg54 returnQueueDoNothingCount(long returnQueueDoNothingCount) {
            this.returnQueueDoNothingCount = returnQueueDoNothingCount;
            return this;
        }

        /**
         * How many resyncs were scheduled while processing return queue.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg53 returnQueueScheduledResyncs(long returnQueueScheduledResyncs) {
            this.returnQueueScheduledResyncs = returnQueueScheduledResyncs;
            return this;
        }

        /**
         * For the last processed item in the return queue -- how long it actually
         * was in the queue (in virtual ms).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg52 returnQueueLastItemInQueueDurationMs(long returnQueueLastItemInQueueDurationMs) {
            this.returnQueueLastItemInQueueDurationMs = returnQueueLastItemInQueueDurationMs;
            return this;
        }

        /**
         * How many items were processed out of return queue.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg51 returnQueueProcessedItems(long returnQueueProcessedItems) {
            this.returnQueueProcessedItems = returnQueueProcessedItems;
            return this;
        }

        /**
         * How many items weren't marked as 'all ok' as the result of main queue processing.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg50 mainQueueNotAllOkCount(long mainQueueNotAllOkCount) {
            this.mainQueueNotAllOkCount = mainQueueNotAllOkCount;
            return this;
        }

        /**
         * How many items were requeued back to the main processing queue
         * as the result of main queue processing.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg49 mainQueueRequeueToMainQueueCount(long mainQueueRequeueToMainQueueCount) {
            this.mainQueueRequeueToMainQueueCount = mainQueueRequeueToMainQueueCount;
            return this;
        }

        /**
         * How many items were removed from cache as the result of main queue processing.
         * <p>
         * 'removed' action indicates abnormal processing, an error is logged
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg48 mainQueueRemovedFromCacheCount(long mainQueueRemovedFromCacheCount) {
            this.mainQueueRemovedFromCacheCount = mainQueueRemovedFromCacheCount;
            return this;
        }

        /**
         * How many items were expired from cache as the result of main queue processing.
         * <p>
         * 'expired' action indicates normal processing, no error is logged
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg47 mainQueueExpiredFromCacheCount(long mainQueueExpiredFromCacheCount) {
            this.mainQueueExpiredFromCacheCount = mainQueueExpiredFromCacheCount;
            return this;
        }

        /**
         * How many writes were sent while processing main processing queue.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg46 mainQueueSentWrites(long mainQueueSentWrites) {
            this.mainQueueSentWrites = mainQueueSentWrites;
            return this;
        }

        /**
         * For the last processed item in the main queue -- how long it actually
         * was in the queue (in virtual ms).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg45 mainQueueLastItemInQueueDurationMs(long mainQueueLastItemInQueueDurationMs) {
            this.mainQueueLastItemInQueueDurationMs = mainQueueLastItemInQueueDurationMs;
            return this;
        }

        /**
         * How many items were processed out of main processing queue.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg44 mainQueueProcessedItems(long mainQueueProcessedItems) {
            this.mainQueueProcessedItems = mainQueueProcessedItems;
            return this;
        }

        /**
         * How many writes failed (via {@link WriteBehindResyncInBackgroundCache#apiStorageWriteFail(Throwable, io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBWriteQueueEntry)}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg43 storageWriteFailures(long storageWriteFailures) {
            this.storageWriteFailures = storageWriteFailures;
            return this;
        }

        /**
         * How many writes succeeded (via {@link WriteBehindResyncInBackgroundCache#apiStorageWriteSuccess(io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBWriteQueueEntry)}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg42 storageWriteSuccesses(long storageWriteSuccesses) {
            this.storageWriteSuccesses = storageWriteSuccesses;
            return this;
        }

        /**
         * How many write attempts were made.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg41 storageWriteAttempts(long storageWriteAttempts) {
            this.storageWriteAttempts = storageWriteAttempts;
            return this;
        }

        /**
         * How many items were processed out of write queue.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg40 storageWriteQueueProcessedItems(long storageWriteQueueProcessedItems) {
            this.storageWriteQueueProcessedItems = storageWriteQueueProcessedItems;
            return this;
        }

        /**
         * How many reads (initial) failed (via {@link WriteBehindResyncInBackgroundCache#apiStorageReadFail(Throwable, WBRBCacheEntry)}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg39 storageReadInitialFailures(long storageReadInitialFailures) {
            this.storageReadInitialFailures = storageReadInitialFailures;
            return this;
        }

        /**
         * How many reads (initial) succeeded (via {@link WriteBehindResyncInBackgroundCache#apiStorageReadSuccess(Object, WBRBCacheEntry)}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg38 storageReadInitialSuccesses(long storageReadInitialSuccesses) {
            this.storageReadInitialSuccesses = storageReadInitialSuccesses;
            return this;
        }

        /**
         * How many read (initial) attempts were made.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg37 storageReadInitialAttempts(long storageReadInitialAttempts) {
            this.storageReadInitialAttempts = storageReadInitialAttempts;
            return this;
        }

        /**
         * How many reads (refresh) arrived but data was not used (not set/merged) for
         * whatever reason./
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg36 storageReadRefreshDataNotUsedCount(long storageReadRefreshDataNotUsedCount) {
            this.storageReadRefreshDataNotUsedCount = storageReadRefreshDataNotUsedCount;
            return this;
        }

        /**
         * How many reads (refresh) arrived too late for a proper resync.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg35 storageReadRefreshTooLateCount(long storageReadRefreshTooLateCount) {
            this.storageReadRefreshTooLateCount = storageReadRefreshTooLateCount;
            return this;
        }

        /**
         * How many reads (refresh) failed (via {@link WriteBehindResyncInBackgroundCache#apiStorageReadFail(Throwable, WBRBCacheEntry)}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg34 storageReadRefreshFailures(long storageReadRefreshFailures) {
            this.storageReadRefreshFailures = storageReadRefreshFailures;
            return this;
        }

        /**
         * How many reads (refresh) succeeded (via {@link WriteBehindResyncInBackgroundCache#apiStorageReadSuccess(Object, WBRBCacheEntry)}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg33 storageReadRefreshSuccesses(long storageReadRefreshSuccesses) {
            this.storageReadRefreshSuccesses = storageReadRefreshSuccesses;
            return this;
        }

        /**
         * How many read (refresh) attempts were made.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg32 storageReadRefreshAttempts(long storageReadRefreshAttempts) {
            this.storageReadRefreshAttempts = storageReadRefreshAttempts;
            return this;
        }

        /**
         * How many reads (total, initial + refresh) failed (via {@link WriteBehindResyncInBackgroundCache#apiStorageReadFail(Throwable, WBRBCacheEntry)}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg31 storageReadTotalFailures(long storageReadTotalFailures) {
            this.storageReadTotalFailures = storageReadTotalFailures;
            return this;
        }

        /**
         * How many reads (total, initial + refresh) succeeded (via {@link WriteBehindResyncInBackgroundCache#apiStorageReadSuccess(Object, WBRBCacheEntry)}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg30 storageReadTotalSuccesses(long storageReadTotalSuccesses) {
            this.storageReadTotalSuccesses = storageReadTotalSuccesses;
            return this;
        }

        /**
         * How many read attempts (total, initial + refresh) were made.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg29 storageReadTotalAttempts(long storageReadTotalAttempts) {
            this.storageReadTotalAttempts = storageReadTotalAttempts;
            return this;
        }

        /**
         * How many items were processed out of read queue.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg28 storageReadQueueProcessedItems(long storageReadQueueProcessedItems) {
            this.storageReadQueueProcessedItems = storageReadQueueProcessedItems;
            return this;
        }

        /**
         * The value of the highest threshold in {@link WBRBConfig#getMonitoringTimeSinceAccessThresholds()}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg27 configMonitoringTimeSinceAccessThresholdMax(long configMonitoringTimeSinceAccessThresholdMax) {
            this.configMonitoringTimeSinceAccessThresholdMax = configMonitoringTimeSinceAccessThresholdMax;
            return this;
        }

        /**
         * The value of the highest threshold in {@link WBRBConfig#getMonitoringFullCacheCyclesThresholds()}
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg26 configMonitoringFullCacheCyclesThresholdMax(int configMonitoringFullCacheCyclesThresholdMax) {
            this.configMonitoringFullCacheCyclesThresholdMax = configMonitoringFullCacheCyclesThresholdMax;
            return this;
        }

        /**
         * 'untouched' item cache expiration delay (as configured)
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg25 configUntouchedItemCacheExpirationDelay(long configUntouchedItemCacheExpirationDelay) {
            this.configUntouchedItemCacheExpirationDelay = configUntouchedItemCacheExpirationDelay;
            return this;
        }

        /**
         * Hard limit on the cache size (as configured)
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg24 configMaxCacheElementsHardLimit(long configMaxCacheElementsHardLimit) {
            this.configMaxCacheElementsHardLimit = configMaxCacheElementsHardLimit;
            return this;
        }

        /**
         * Maximum allowed targeted (it's possible to exceed this value) queue size
         * for the main processing queue (as configured)
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg23 configMainQueueMaxTargetSize(long configMainQueueMaxTargetSize) {
            this.configMainQueueMaxTargetSize = configMainQueueMaxTargetSize;
            return this;
        }

        /**
         * Return queue minimum/target cache time (in ms) (as configured)
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg22 configReturnQueueCacheTimeMinMs(long configReturnQueueCacheTimeMinMs) {
            this.configReturnQueueCacheTimeMinMs = configReturnQueueCacheTimeMinMs;
            return this;
        }

        /**
         * Main queue target cache time (in ms) (as configured)
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg21 configMainQueueCacheTimeMs(long configMainQueueCacheTimeMs) {
            this.configMainQueueCacheTimeMs = configMainQueueCacheTimeMs;
            return this;
        }

        /**
         * Write queue size.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg20 writeQueueSize(long writeQueueSize) {
            this.writeQueueSize = writeQueueSize;
            return this;
        }

        /**
         * Read queue size.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg19 readQueueSize(long readQueueSize) {
            this.readQueueSize = readQueueSize;
            return this;
        }

        /**
         * Return queue size.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg18 returnQueueSize(long returnQueueSize) {
            this.returnQueueSize = returnQueueSize;
            return this;
        }

        /**
         * Main processing queue size.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg17 mainQueueSize(long mainQueueSize) {
            this.mainQueueSize = mainQueueSize;
            return this;
        }

        /**
         * Current cache size.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg16 currentCacheSize(long currentCacheSize) {
            this.currentCacheSize = currentCacheSize;
            return this;
        }

        /**
         * Whether cache itself AND all the threads & thread pools required for the
         * cache operation are still alive; if pools don't exist, they don't affect this value.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg15 everythingAlive(boolean everythingAlive) {
            this.everythingAlive = everythingAlive;
            return this;
        }

        /**
         * Number of currently active threads in the pool; -1 if there's no pool.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg14 writeThreadPoolActiveThreads(int writeThreadPoolActiveThreads) {
            this.writeThreadPoolActiveThreads = writeThreadPoolActiveThreads;
            return this;
        }

        /**
         * Number of currently active threads in the pool; -1 if there's no pool.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg13 readThreadPoolActiveThreads(int readThreadPoolActiveThreads) {
            this.readThreadPoolActiveThreads = readThreadPoolActiveThreads;
            return this;
        }

        /**
         * Whether thread pool is alive (false if there's no pool).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg12 writeThreadPoolAlive(boolean writeThreadPoolAlive) {
            this.writeThreadPoolAlive = writeThreadPoolAlive;
            return this;
        }

        /**
         * Whether thread pool is alive (false if there's no pool).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg11 readThreadPoolAlive(boolean readThreadPoolAlive) {
            this.readThreadPoolAlive = readThreadPoolAlive;
            return this;
        }

        /**
         * Whether thread is alive.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg10 returnQueueProcessingThreadAlive(boolean returnQueueProcessingThreadAlive) {
            this.returnQueueProcessingThreadAlive = returnQueueProcessingThreadAlive;
            return this;
        }

        /**
         * Whether thread is alive.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg9 mainQueueProcessingThreadAlive(boolean mainQueueProcessingThreadAlive) {
            this.mainQueueProcessingThreadAlive = mainQueueProcessingThreadAlive;
            return this;
        }

        /**
         * Whether thread is alive.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg8 writeQueueProcessingThreadAlive(boolean writeQueueProcessingThreadAlive) {
            this.writeQueueProcessingThreadAlive = writeQueueProcessingThreadAlive;
            return this;
        }

        /**
         * Whether thread is alive.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg7 readQueueProcessingThreadAlive(boolean readQueueProcessingThreadAlive) {
            this.readQueueProcessingThreadAlive = readQueueProcessingThreadAlive;
            return this;
        }

        /**
         * Current cache control state as String (from WBRBCacheControlState) -- NOT_STARTED, RUNNING, SHUTDOWN...
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg6 cacheControlStateString(String cacheControlStateString) {
            this.cacheControlStateString = cacheControlStateString;
            return this;
        }

        /**
         * Current cache control state as enum -- NOT_STARTED, RUNNING, SHUTDOWN...
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg5 cacheControlState(WBRBCacheControlState cacheControlState) {
            this.cacheControlState = cacheControlState;
            return this;
        }

        /**
         * Whether cache is usable -- that is standard read & write operations
         * can be performed; this can differ from {@link #isAlive()} value for various
         * reasons such as cache flush.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg4 cacheUsable(boolean cacheUsable) {
            this.cacheUsable = cacheUsable;
            return this;
        }

        /**
         * Whether cache is alive (that is it was started and not stopped yet).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg3 cacheAlive(boolean cacheAlive) {
            this.cacheAlive = cacheAlive;
            return this;
        }

        /**
         * Indicates when status was created (e.g. for caching purposes).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_WBRBStatusBuilder_statusCreatedAt_arg2 statusCreatedAt(long statusCreatedAt) {
            this.statusCreatedAt = statusCreatedAt;
            return this;
        }

        @Override
        @SuppressWarnings("all")
        public WBRBStatus buildWBRBStatus() {
            return new WBRBStatus(statusCreatedAt, cacheAlive, cacheUsable, cacheControlState, cacheControlStateString, readQueueProcessingThreadAlive, writeQueueProcessingThreadAlive, mainQueueProcessingThreadAlive, returnQueueProcessingThreadAlive, readThreadPoolAlive, writeThreadPoolAlive, readThreadPoolActiveThreads, writeThreadPoolActiveThreads, everythingAlive, currentCacheSize, mainQueueSize, returnQueueSize, readQueueSize, writeQueueSize, configMainQueueCacheTimeMs, configReturnQueueCacheTimeMinMs, configMainQueueMaxTargetSize, configMaxCacheElementsHardLimit, configUntouchedItemCacheExpirationDelay, configMonitoringFullCacheCyclesThresholdMax, configMonitoringTimeSinceAccessThresholdMax, storageReadQueueProcessedItems, storageReadTotalAttempts, storageReadTotalSuccesses, storageReadTotalFailures, storageReadRefreshAttempts, storageReadRefreshSuccesses, storageReadRefreshFailures, storageReadRefreshTooLateCount, storageReadRefreshDataNotUsedCount, storageReadInitialAttempts, storageReadInitialSuccesses, storageReadInitialFailures, storageWriteQueueProcessedItems, storageWriteAttempts, storageWriteSuccesses, storageWriteFailures, mainQueueProcessedItems, mainQueueLastItemInQueueDurationMs, mainQueueSentWrites, mainQueueExpiredFromCacheCount, mainQueueRemovedFromCacheCount, mainQueueRequeueToMainQueueCount, mainQueueNotAllOkCount, returnQueueProcessedItems, returnQueueLastItemInQueueDurationMs, returnQueueScheduledResyncs, returnQueueDoNothingCount, returnQueueExpiredFromCacheCount, returnQueueRemovedFromCacheCount, returnQueueRequeueToReturnQueueCount, returnQueueNegativeTimeSinceLastAccessErrorCount, returnQueueItemNotRetainedDueToMainQueueSizeCount, checkCacheAttemptsNoDedup, checkCachePreloadAttempts, checkCachePreloadCacheHit, checkCachePreloadCacheFullExceptionCount, checkCacheReadAttempts, checkCacheReadCacheHit, checkCacheReadCacheFullExceptionCount, checkCacheTotalCacheFullExceptionCount, checkCacheNullKeyCount, cacheReadAttempts, cacheReadTimeouts, cacheReadInterrupts, cacheReadErrors, cacheWriteAttempts, cacheWriteElementNotPresentCount, cacheWriteErrors, cacheWriteTooManyUpdates, msgWarnCount, msgExternalWarnCount, msgExternalErrorCount, msgExternalDataLossCount, msgErrorCount, msgFatalCount, msgTotalWarnOrHigherCount, msgTotalErrorOrHigherCount, lastTimestampMsgPerSeverityOrdinal, lastLoggedTextMsgPerSeverityOrdinal, lastWarnMsgTimestamp, lastWarnLoggedMsgText, lastErrorMsgTimestamp, lastErrorLoggedMsgText, lastFatalMsgTimestamp, lastFatalLoggedMsgText, fullCycleCountThreshold1, fullCycleCountThreshold2, fullCycleCountThreshold3, fullCycleCountThreshold4, fullCycleCountThreshold5, fullCycleCountAboveAllThresholds, timeSinceAccessThreshold1, timeSinceAccessThreshold2, timeSinceAccessThreshold3, timeSinceAccessThreshold4, timeSinceAccessThreshold5, timeSinceAccessThresholdAboveAllThresholds);
        }
    }

    /**
     *  FIELD COMMENT: Indicates when status was created (e.g. for caching purposes).
     * <p>
     * CONSTRUCTOR COMMENT: Indicates when status was created (e.g. for caching purposes).
     */
    public static ZBSI_WBRBStatusBuilder_statusCreatedAt_arg2 statusCreatedAt(long statusCreatedAt) {
        return new ZBSI_WBRBStatusBuilder_statusCreatedAt_builderClass().statusCreatedAt(statusCreatedAt);
    }
}
