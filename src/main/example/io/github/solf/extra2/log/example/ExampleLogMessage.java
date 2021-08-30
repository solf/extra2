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
package io.github.solf.extra2.log.example;

import static io.github.solf.extra2.log.LogMessageSeverity.CRITICAL;
import static io.github.solf.extra2.log.LogMessageSeverity.DEBUG;
import static io.github.solf.extra2.log.LogMessageSeverity.ERROR;
import static io.github.solf.extra2.log.LogMessageSeverity.EXTERNAL_DATA_LOSS;
import static io.github.solf.extra2.log.LogMessageSeverity.EXTERNAL_ERROR;
import static io.github.solf.extra2.log.LogMessageSeverity.EXTERNAL_INFO;
import static io.github.solf.extra2.log.LogMessageSeverity.EXTERNAL_WARN;
import static io.github.solf.extra2.log.LogMessageSeverity.INFO;
import static io.github.solf.extra2.log.LogMessageSeverity.WARN;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.cache.wbrb.WBRBCacheMessageSeverity;
import io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache;
import io.github.solf.extra2.log.BaseLoggingUtility;
import io.github.solf.extra2.log.LogMessageSeverity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Possible cache messages.
 * TO-DO check if they are all used?
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
@RequiredArgsConstructor
public enum ExampleLogMessage
{
	/**
	 * Indicates that some of the previous messages of the given type were skipped
	 * in logging (due to throttling most likely).
	 * <p>
	 * Arguments: skipped messages type, count
	 */
	LOG_MESSAGE_TYPE_PREVIOUS_MESSAGES_SKIPPED(INFO, true/*standard message type*/, false/*non-throttle-able*/, false/*update stats*/),
	/**
	 * Indicates that some of the next messages of the given type might be
	 * skipped in logging (due to throttling most likely) for a given amount
	 * of time
	 * <p>
	 * Arguments: skipped messages type, time (in ms)
	 */
	LOG_MESSAGE_TYPE_MESSAGES_MAY_BE_SKIPPED_FOR(INFO, true/*standard message type*/, false/*non-throttle-able*/, false/*update stats*/),
	/**
	 * Indicates that attempt to log a message has failed for whatever reason
	 * <p>
	 * Arguments: exception
	 */
	LOG_MESSAGE_FAILED(ERROR, true/*standard message type*/, false/*non-throttle-able*/, true/*update stats*/),
	
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
	NON_STANDARD_DEBUG(DEBUG, false),
	
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
	NON_STANDARD_INFO(INFO, false),
	
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
	NON_STANDARD_WARN(WARN, false),
	
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
	NON_STANDARD_EXTERNAL_INFO(EXTERNAL_INFO, false),
	
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
	NON_STANDARD_EXTERNAL_WARN(EXTERNAL_WARN, false),
	
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
	NON_STANDARD_EXTERNAL_ERROR(EXTERNAL_ERROR, false),
	
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
	NON_STANDARD_EXTERNAL_DATA_LOSS(EXTERNAL_DATA_LOSS, false),
	
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
	NON_STANDARD_ERROR(ERROR, false),
	
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
	NON_STANDARD_FATAL(CRITICAL, false),
	
	
	
	/**
	 * Code assertion failed, functionality may be seriously compromised.
	 * <p>
	 * Arguments: exception, text message with information
	 */
	ASSERTION_FAILED(CRITICAL),
	
	
	
	// =========================================================================
	// ==================  BELOW IS APPLICATION-SPECIFIC STUFF =================
	// =========================================================================
	
	
	
	/**
	 * Since there are no other messages with WARN severity (yet?) -- create this
	 * one to be able to test them.
	 */
	TEST_WARN(WARN),
	
	
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
	 * Cache was shutdown
	 * <p>
	 * Arguments: remaining items in the inflight queue
	 */
	SHUTDOWN_COMPLETED(INFO),
	
	;
	
	/**
	 * Message severity.
	 */
	@Getter
	private final LogMessageSeverity severity;
	
	/**
	 * Whether this is a 'standard' message; non-standard messages need further 
	 * classification.
	 * <p>
	 * For non-standard messages first message argument is expected to be non-null 
	 * String classifier that is used to distinguish between message types e.g.
	 * for throttling.
	 * 
	 * @see BaseLoggingUtility#logNonStandardMessage(LogMessageSeverity, String, Throwable, Object...)
	 */
	@Getter
	private final boolean standardMessage;
	
	/**
	 * Whether this message type can be throttled.
	 * <p>
	 * It is CRITICAL that throttling-related messages are not throttled 
	 * themselves!
	 */
	@Getter
	private final boolean messageCanBeThrottled;
	
	/**
	 * Whether standard stats should be updated when processing this message
	 * type.
	 * <p>
	 * Typically you do NOT want to update stats for throttling-related messages.
	 */
	@Getter
	private final boolean updateStatsForMessage; 
	
	/**
	 * Constructor for standard throttle-able message types.
	 */
	private ExampleLogMessage(LogMessageSeverity severity)
	{
		this(severity, true/*standard message*/);
	}
	
	/**
	 * Constructor for throttle-able message types.
	 */
	private ExampleLogMessage(LogMessageSeverity severity, boolean isStandardMessage)
	{
		this(severity, isStandardMessage, true/*can be throttled*/, true/*update stats*/);
	}
}
