/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.cache.wbrb;

import static com.github.solf.extra2.cache.wbrb.WBRBCacheMessageSeverity.*;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBMainQueueItemCacheRetainDecision;
import com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBReturnQueueItemProcessingDecision;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Possible cache messages.
 * TODO check if they are all used?
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
@RequiredArgsConstructor
public enum WBRBCacheMessage
{
	/**
	 * Used to log non-standard messages (e.g. from the subclasses) with the given
	 * severity.
	 * <p>
	 * Non-standard messages are supposed to specify their own String classifiers 
	 * (e.g. for throttling).
	 * 
	 * @deprecated these aren't supposed to be used directly, instead look into
	 * 		{@link WriteBehindResyncInBackgroundCache#logNonStandardMessage(WBRBCacheMessageSeverity, String, Throwable, Object...)}
	 */
	@Deprecated
	NON_STANDARD_DEBUG(DEBUG, true),
	
	/**
	 * Used to log non-standard messages (e.g. from the subclasses) with the given
	 * severity.
	 * <p>
	 * Non-standard messages are supposed to specify their own String classifiers 
	 * (e.g. for throttling).
	 * 
	 * @deprecated these aren't supposed to be used directly, instead look into
	 * 		{@link WriteBehindResyncInBackgroundCache#logNonStandardMessage(WBRBCacheMessageSeverity, String, Throwable, Object...)}
	 */
	@Deprecated
	NON_STANDARD_INFO(INFO, true),
	
	/**
	 * Used to log non-standard messages (e.g. from the subclasses) with the given
	 * severity.
	 * <p>
	 * Non-standard messages are supposed to specify their own String classifiers 
	 * (e.g. for throttling).
	 * 
	 * @deprecated these aren't supposed to be used directly, instead look into
	 * 		{@link WriteBehindResyncInBackgroundCache#logNonStandardMessage(WBRBCacheMessageSeverity, String, Throwable, Object...)}
	 */
	@Deprecated
	NON_STANDARD_WARN(WARN, true),
	
	/**
	 * Used to log non-standard messages (e.g. from the subclasses) with the given
	 * severity.
	 * <p>
	 * Non-standard messages are supposed to specify their own String classifiers 
	 * (e.g. for throttling).
	 * 
	 * @deprecated these aren't supposed to be used directly, instead look into
	 * 		{@link WriteBehindResyncInBackgroundCache#logNonStandardMessage(WBRBCacheMessageSeverity, String, Throwable, Object...)}
	 */
	@Deprecated
	NON_STANDARD_EXTERNAL_INFO(EXTERNAL_INFO, true),
	
	/**
	 * Used to log non-standard messages (e.g. from the subclasses) with the given
	 * severity.
	 * <p>
	 * Non-standard messages are supposed to specify their own String classifiers 
	 * (e.g. for throttling).
	 * 
	 * @deprecated these aren't supposed to be used directly, instead look into
	 * 		{@link WriteBehindResyncInBackgroundCache#logNonStandardMessage(WBRBCacheMessageSeverity, String, Throwable, Object...)}
	 */
	@Deprecated
	NON_STANDARD_EXTERNAL_WARN(EXTERNAL_WARN, true),
	
	/**
	 * Used to log non-standard messages (e.g. from the subclasses) with the given
	 * severity.
	 * <p>
	 * Non-standard messages are supposed to specify their own String classifiers 
	 * (e.g. for throttling).
	 * 
	 * @deprecated these aren't supposed to be used directly, instead look into
	 * 		{@link WriteBehindResyncInBackgroundCache#logNonStandardMessage(WBRBCacheMessageSeverity, String, Throwable, Object...)}
	 */
	@Deprecated
	NON_STANDARD_EXTERNAL_ERROR(EXTERNAL_ERROR, true),
	
	/**
	 * Used to log non-standard messages (e.g. from the subclasses) with the given
	 * severity.
	 * <p>
	 * Non-standard messages are supposed to specify their own String classifiers 
	 * (e.g. for throttling).
	 * 
	 * @deprecated these aren't supposed to be used directly, instead look into
	 * 		{@link WriteBehindResyncInBackgroundCache#logNonStandardMessage(WBRBCacheMessageSeverity, String, Throwable, Object...)}
	 */
	@Deprecated
	NON_STANDARD_EXTERNAL_DATA_LOSS(EXTERNAL_DATA_LOSS, true),
	
	/**
	 * Used to log non-standard messages (e.g. from the subclasses) with the given
	 * severity.
	 * <p>
	 * Non-standard messages are supposed to specify their own String classifiers 
	 * (e.g. for throttling).
	 * 
	 * @deprecated these aren't supposed to be used directly, instead look into
	 * 		{@link WriteBehindResyncInBackgroundCache#logNonStandardMessage(WBRBCacheMessageSeverity, String, Throwable, Object...)}
	 */
	@Deprecated
	NON_STANDARD_ERROR(ERROR, true),
	
	/**
	 * Used to log non-standard messages (e.g. from the subclasses) with the given
	 * severity.
	 * <p>
	 * Non-standard messages are supposed to specify their own String classifiers 
	 * (e.g. for throttling).
	 * 
	 * @deprecated these aren't supposed to be used directly, instead look into
	 * 		{@link WriteBehindResyncInBackgroundCache#logNonStandardMessage(WBRBCacheMessageSeverity, String, Throwable, Object...)}
	 */
	@Deprecated
	NON_STANDARD_FATAL(FATAL, true),
	
	/**
	 * Code assertion failed, functionality may be seriously compromised.
	 * <p>
	 * Arguments: exception, text message with information
	 */
	ASSERTION_FAILED(FATAL),
	
	/**
	 * Since there are no other messages with WARN severity (yet?) -- create this
	 * one to be able to test them.
	 */
	TEST_WARN(WARN),
	
	/**
	 * Item unexpectedly needed to be removed from cache in {@link WriteBehindResyncInBackgroundCache#addToCache(Object)}
	 * <p>
	 * Arguments: key
	 */
	UNEXPECTED_CACHE_REMOVAL_IN_ADD_ENTRY(ERROR),
	/**
	 * Item unexpectedly needed to be removed from cache in {@link WriteBehindResyncInBackgroundCache#mainQueueProcessingThread}
	 * <p>
	 * Arguments: key
	 */
	UNEXPECTED_CACHE_REMOVAL_IN_MAIN_QUEUE_PROCESSING(ERROR),
	/**
	 * Item unexpectedly needed to be removed from cache in {@link WriteBehindResyncInBackgroundCache#returnQueueProcessingThread}
	 * <p>
	 * Arguments: key
	 */
	UNEXPECTED_CACHE_REMOVAL_IN_RETURN_QUEUE_PROCESSING(ERROR),
	
	
	/**
	 * Indicates unexpected interrupt in {@link WriteBehindResyncInBackgroundCache#readQueueProcessingThread}
	 * <p>
	 * Thread will re-start processing (does not exit).
	 * <p>
	 * Arguments: exception
	 */
	READ_QUEUE_PROCESSOR_UNEXPECTED_INTERRUPT(ERROR),
	/**
	 * Successful storage read incoming at an unexpected time.
	 * <p>
	 * Arguments: key, unexpected cache state
	 */
	UNEXPECTED_CACHE_STATE_FOR_READ_MERGE(ERROR),
	/**
	 * Failed storage read incoming at an unexpected time.
	 * <p>
	 * Arguments: key, unexpected cache state
	 */
	UNEXPECTED_CACHE_STATE_FOR_READ_FAIL(ERROR),
	/**
	 * Read queue processing thread encountered an unexpected state.
	 * <p>
	 * Arguments: key, unexpected cache state
	 */
	UNEXPECTED_CACHE_STATE_FOR_READ_QUEUE_PROCESSING(ERROR),
	/**
	 * Internal attempt to remove from cache an element that wasn't already there.
	 * <p>
	 * Arguments: key, exception (for stack trace)
	 */
	NOT_PRESENT_ELEMENT_REMOVAL_ATTEMPT(ERROR),
	
	
	/**
	 * Indicates unexpected interrupt in {@link WriteBehindResyncInBackgroundCache#writeQueueProcessingThread}
	 * <p>
	 * Thread will re-start processing (does not exit).
	 * <p>
	 * Arguments: exception
	 */
	WRITE_QUEUE_PROCESSOR_UNEXPECTED_INTERRUPT(ERROR),
	/**
	 * Successful storage write incoming at an unexpected time.
	 * <p>
	 * Arguments: key, unexpected cache state
	 */
	UNEXPECTED_CACHE_STATE_FOR_WRITE_SUCCESS(ERROR),
	/**
	 * Failed storage write incoming at an unexpected time.
	 * <p>
	 * Arguments: key, unexpected cache state
	 */
	UNEXPECTED_CACHE_STATE_FOR_WRITE_FAIL(ERROR),
	/**
	 * Write queue processing thread encountered an unexpected state.
	 * <p>
	 * This is not reported by default implementation, but could be useful
	 * for custom implementations.
	 * <p>
	 * Arguments: key, unexpected cache state
	 */
	UNEXPECTED_CACHE_STATE_FOR_WRITE_QUEUE_PROCESSING(ERROR),
	
	
	/**
	 * Indicates unexpected interrupt in {@link WriteBehindResyncInBackgroundCache#mainQueueProcessingThread}
	 * <p>
	 * Thread will re-start processing (does not exit).
	 * <p>
	 * Arguments: exception
	 */
	MAIN_QUEUE_PROCESSOR_UNEXPECTED_INTERRUPT(ERROR),
	
	
	/**
	 * Indicates unexpected interrupt in {@link WriteBehindResyncInBackgroundCache#returnQueueProcessingThread}
	 * <p>
	 * Thread will re-start processing (does not exit).
	 * <p>
	 * Arguments: exception
	 */
	RETURN_QUEUE_PROCESSOR_UNEXPECTED_INTERRUPT(ERROR),
	
	/**
	 * Indicates that resync came too late -- updates are no longer collecting
	 * <p>
	 * Arguments: key, decision (what action will be taken on data)
	 */
	RESYNC_IS_TOO_LATE(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates that data resync failed but write is still issued (may lose
	 * data in storage that should've been resynced).
	 * <p>
	 * Arguments: key
	 */
	RESYNC_FAILED_FINAL_STORAGE_DATA_OVERWRITE(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates that data resync failed and write is not allowed (all in-memory
	 * changes are lost).
	 * <p>
	 * Arguments: key
	 */
	RESYNC_FAILED_FINAL_DATA_DISCARDED(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates that write failed for too many full cache cycles, data has been
	 * discarded.
	 * <p>
	 * Arguments: key
	 */
	WRITE_FAILED_FINAL_DATA_DISCARDED(EXTERNAL_DATA_LOSS),
	
	
	
	/**
	 * Reading from external storage has failed (may or may not be retried after).
	 * <p>
	 * Arguments: exception, key
	 */
	STORAGE_READ_FAIL(EXTERNAL_ERROR),
	/**
	 * Reading from external storage has failed (this is the final failure, no 
	 * more retries).
	 * <p>
	 * Arguments: exception (optional), key
	 */
	STORAGE_READ_FAIL_FINAL(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates that an item went for a read retry.
	 * <p>
	 * Arguments: exception that caused previous read to fail (optional), key
	 */
	STORAGE_READ_RETRY_ISSUED(EXTERNAL_INFO),
	
	
	/**
	 * Writing to external storage has failed (may or may not be retried after).
	 * <p>
	 * Arguments: exception, key
	 */
	STORAGE_WRITE_FAIL(EXTERNAL_ERROR),
	/**
	 * Writing to external storage has failed (this is the final failure, no 
	 * more retries).
	 * <p>
	 * Arguments: exception (optional), key
	 */
	STORAGE_WRITE_FAIL_FINAL(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates that an item went for a write retry.
	 * <p>
	 * Arguments: exception that caused previous write to fail (optional), key
	 */
	STORAGE_WRITE_RETRY_ISSUED(EXTERNAL_INFO),
	
	
	
	/**
	 * Failed to split an item for the write.
	 * <p>
	 * Arguments: exception, key
	 */
	SPLIT_FOR_WRITE_FAIL(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#convertToInternalUpdateFormatFromExternalUpdate(Object, Object)}
	 * <p>
	 * Arguments: exception, key
	 */
	CONVERT_TO_INTERNAL_UPDATE_FORMAT_FROM_EXTERNAL_UPDATE_FAIL(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#convertToCacheFormatFromStorageData(Object, Object)}
	 * <p>
	 * Arguments: exception, key
	 */
	CONVERT_TO_CACHE_FORMAT_FROM_STORAGE_DATA_FAIL(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#convertFromCacheFormatToReturnValue(Object, Object)}
	 * <p>
	 * Arguments: exception, key
	 */
	CONVERT_FROM_CACHE_FORMAT_TO_RETURN_VALUE_FAIL(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#mergeCacheAndStorageData(Object, Object, Object, java.util.List)}
	 * <p>
	 * Arguments: exception, key
	 */
	MERGE_CACHE_AND_STORAGE_DATA_FAIL(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#applyUpdate(Object, Object)}
	 * <p>
	 * Arguments: exception, key
	 */
	APPLY_UPDATE_FAIL(EXTERNAL_DATA_LOSS),
	

	/**
	 * Main queue processing non-standard outcome
	 * <p>
	 * Arguments: key, outcome ( {@link WBRBMainQueueItemCacheRetainDecision} )
	 */
	MAIN_QUEUE_NON_STANDARD_OUTCOME(EXTERNAL_WARN),

	/**
	 * Main queue processing non-standard outcome
	 * <p>
	 * Arguments: key, outcome ( {@link WBRBReturnQueueItemProcessingDecision} )
	 */
	RETURN_QUEUE_NON_STANDARD_OUTCOME(EXTERNAL_WARN),
	
	
	/**
	 * Cache operation has failed due to too many times encountering 'REMOVED_FROM_CACHE'
	 * state; this shouldn't happen more than once normally.
	 * <p>
	 * Arguments: exception(for stack trace), key, number of failed attempts 
	 */
	TOO_MANY_REMOVED_FROM_CACHE_STATE_RETRIES(ERROR),
	
	/**
	 * Too many updates for the cache element -- they cannot be collected anymore
	 * (this likely results in data loss)
	 * <p>
	 * Arguments: exception with description of reason, key
	 */
	TOO_MANY_CACHE_ELEMENT_UPDATES(EXTERNAL_DATA_LOSS),

	
	/**
	 * Indicates that some of the previous messages of the given type were skipped
	 * in logging (due to throttling most likely).
	 * <p>
	 * Arguments: skipped messages type, count
	 */
	LOG_MESSAGE_TYPE_PREVIOUS_MESSAGES_SKIPPED(INFO),
	/**
	 * Indicates that some of the next messages of the given type might be
	 * skipped in logging (due to throttling most likely) for a given amount
	 * of time
	 * <p>
	 * Arguments: skipped messages type, time (in ms)
	 */
	LOG_MESSAGE_TYPE_MESSAGES_MAY_BE_SKIPPED_FOR(INFO),
	
	
	/**
	 * Cache was started
	 * <p>
	 * Arguments: none
	 */
	STARTED(INFO),

	/**
	 * Cache shutdown requested
	 * <p>
	 * Arguments: none
	 */
	SHUTDOWN_REQUESTED(INFO),
	
	/**
	 * Indicates that spool down was not achieved during the shutdown (in the
	 * given time limit) -- this means that data most likely will be lost.
	 * <p>
	 * Arguments: shutdown time limit given in virtual milliseconds
	 */
	SHUTDOWN_SPOOLDOWN_NOT_ACHIEVED(EXTERNAL_DATA_LOSS),
	
	/**
	 * Cache was shutdown
	 * <p>
	 * Arguments: remaining items in the inflight queue
	 */
	SHUTDOWN_COMPLETED(INFO),
	
	
	/**
	 * Cache data flush was requested
	 */
	FLUSH_REQUESTED(INFO),
	
	/**
	 * Complete data flush was not achieved during the flush operation
	 * <p>
	 * Arguments: remaining in-cache elements and flush time limit given in 
	 * virtual milliseconds 
	 */
	FLUSH_SPOOLDOWN_NOT_ACHIEVED(EXTERNAL_WARN),
	
	/**
	 * Data flush completed successfully, all data was flushed.
	 */
	FLUSH_SUCCESFULLY_COMPLETED(INFO),
	
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiNoLockReadFromStorage(Object, boolean, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_ReadFromStorage(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiNoLockReadBatchDelayExpired()}
	 * <p>
	 * Arguments: exception
	 */
	SPI_EXCEPTION_ReadBatchDelayExpired(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockMakeMergeDecision(Object, Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_MakeMergeDecision(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockMakeInitialReadFailedFinalDecision(Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_MakeInitialReadFailedFinalDecision(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockMakeResyncFailedFinalDecision(Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_MakeResyncFailedFinalDecision(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockUpdates_reset(boolean, Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_Updates_reset(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockUpdates_collect(Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload, Object)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_Updates_collect(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockUpdates_isMergePossible(Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload, site.sonata.extra2.nullable.NullableOptional)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_Updates_isMergePossible(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockMakeReadRetryDecision(Throwable, Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_MakeReadRetryDecision(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiNoLockGetReadQueueProcessorLock(site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_GetReadQueueProcessorLock(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiSomeLockMakeReadQueueProcessingDecision(Object, WBRBCacheEntryStatus, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_MakeReadQueueProcessingDecision(EXTERNAL_DATA_LOSS),
	
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiNoLockWriteToStorage(Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBWriteQueueEntry)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_WriteToStorage(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiNoLockWriteBatchDelayExpired()}
	 * <p>
	 * Arguments: exception
	 */
	SPI_EXCEPTION_WriteBatchDelayExpired(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiNoLockMakeWriteQueueProcessingDecision(Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBWriteQueueEntry)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_MakeWriteQueueProcessingDecision(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockMakeReadRetryDecision(Throwable, Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_MakeWriteRetryDecision(EXTERNAL_DATA_LOSS),
	
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockMakeMainQueueProcessingDecision_ResyncFailedFinal(Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_MakeMainQueueProcessingDecision_ResyncFailedFinal(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockMakeMainQueueProcessingDecision_ResyncPending(Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_MakeMainQueueProcessingDecision_ResyncPending(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockMakeMainQueueProcessingDecision_WriteFailedFinal(Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_MakeMainQueueProcessingDecision_WriteFailedFinal(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockMakeMainQueueProcessingDecision_WritePending(Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_MakeMainQueueProcessingDecision_WritePending(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockMakeMainQueueProcessingDecision(Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_MakeMainQueueProcessingDecision(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockMakeMainQueueProcessingDecision_isResetFailureCounts(boolean, WBRBMainQueueItemCacheRetainDecision, Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_MakeMainQueueProcessingDecision_isResetFailureCounts(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockMakeMainQueueProcessingDecision_logNonStandardOutcome(boolean, WBRBMainQueueItemCacheRetainDecision, Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_MakeMainQueueProcessingDecision_logNonStandardOutcome(EXTERNAL_ERROR),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockMakeReturnQueueProcessingDecision(Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_MakeReturnQueueProcessingDecision(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockMakeReturnQueueProcessingDecision_WriteFailedFinal(Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_MakeReturnQueueProcessingDecision_WriteFailedFinal(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockMakeReturnQueueProcessingDecision_WritePending(Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_MakeReturnQueueProcessingDecision_WritePending(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockMakeReturnQueueProcessingDecision_WriteOk(Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_MakeReturnQueueProcessingDecision_WriteOk(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockMakeReturnQueueProcessingDecision_logNonStandardOutcome(boolean, WBRBReturnQueueItemProcessingDecision, Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_MakeReturnQueueProcessingDecision_logNonStandardOutcome(EXTERNAL_ERROR),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiReadLockMakeCacheReadDecision(Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_MakeCacheReadDecision(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiWriteLockMakeCacheWriteDecision(Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * <p>
	 * Arguments: exception, key
	 */
	SPI_EXCEPTION_MakeCacheWriteDecision(EXTERNAL_DATA_LOSS),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiUnknownLockLogMessage(WBRBCacheMessage, Throwable, Object...)}
	 * <p>
	 * Arguments: exception
	 */
	SPI_EXCEPTION_LogMessage(ERROR),
	/**
	 * Indicates exception in {@link WriteBehindResyncInBackgroundCache#spiUnknownLock_Event(site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBEvent, Object, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, site.sonata.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload, Throwable, Object...)
	 * <p>
	 * Arguments: exception
	 */
	SPI_EXCEPTION_Event(ERROR),
	;
	
	/**
	 * Message severity.
	 */
	@Getter
	private final WBRBCacheMessageSeverity severity;
	
	/**
	 * Whether this is a 'non-standard' message that needs further classification.
	 * <p>
	 * For non-standard messages first message argument is expected to be non-null 
	 * String classifier that is used to distinguish between message types e.g.
	 * for throttling.
	 * 
	 * @see WriteBehindResyncInBackgroundCache#logNonStandardMessage(WBRBCacheMessageSeverity, String, Throwable, Object...)
	 */
	@Getter
	private final boolean nonStandard;
	
	/**
	 * Constructor for non-generic message types.
	 */
	private WBRBCacheMessage(WBRBCacheMessageSeverity severity)
	{
		this(severity, false);
	}
}
