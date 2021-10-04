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
import static io.github.solf.extra2.log.LogMessageSeverity.DATA_LOSS;
import static io.github.solf.extra2.log.LogMessageSeverity.DEBUG;
import static io.github.solf.extra2.log.LogMessageSeverity.ERROR;
import static io.github.solf.extra2.log.LogMessageSeverity.EXTERNAL_DATA_LOSS;
import static io.github.solf.extra2.log.LogMessageSeverity.EXTERNAL_ERROR;
import static io.github.solf.extra2.log.LogMessageSeverity.EXTERNAL_INFO;
import static io.github.solf.extra2.log.LogMessageSeverity.EXTERNAL_WARN;
import static io.github.solf.extra2.log.LogMessageSeverity.INFO;
import static io.github.solf.extra2.log.LogMessageSeverity.TRACE;
import static io.github.solf.extra2.log.LogMessageSeverity.WARN;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.log.BaseLoggingUtility;
import io.github.solf.extra2.log.LogMessageSeverity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Example file for handling messages logged via {@link BaseLoggingUtility}
 * as enums.
 * 
 * @see ExampleLoggingUtility
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
	LOG_MESSAGE_TYPE_PREVIOUS_MESSAGES_SKIPPED(INFO, true/*throttled-by-ordinal*/, false/*non-throttle-able*/, false/*update stats*/),
	/**
	 * Indicates that some of the next messages of the given type might be
	 * skipped in logging (due to throttling most likely) for a given amount
	 * of time
	 * <p>
	 * Arguments: skipped messages type, time (in ms)
	 */
	LOG_MESSAGE_TYPE_MESSAGES_MAY_BE_SKIPPED_FOR(INFO, true/*throttled-by-ordinal*/, false/*non-throttle-able*/, false/*update stats*/),
	/**
	 * Indicates that attempt to log a message has failed for whatever reason
	 * <p>
	 * Arguments: exception
	 */
	LOG_MESSAGE_FAILED(ERROR, true/*throttled-by-ordinal*/, false/*non-throttle-able*/, true/*update stats*/),

	
	
	/**
	 * Used to log non-classified messages (usually not a good idea) with the given
	 * severity.
	 * <p>
	 * Non-classified messages are supposed to specify their own String classifiers 
	 * (e.g. for throttling).
	 * 
	 * @deprecated these aren't supposed to be used directly, instead look into
	 * 		{@link BaseLoggingUtility#logNonClassifiedMessage(LogMessageSeverity, String, Throwable, Object...)}
	 */
	@Deprecated
	NON_CLASSIFIED_TRACE(TRACE, false),
	
	/**
	 * Used to log non-classified messages (usually not a good idea) with the given
	 * severity.
	 * <p>
	 * Non-classified messages are supposed to specify their own String classifiers 
	 * (e.g. for throttling).
	 * 
	 * @deprecated these aren't supposed to be used directly, instead look into
	 * 		{@link BaseLoggingUtility#logNonClassifiedMessage(LogMessageSeverity, String, Throwable, Object...)}
	 */
	@Deprecated
	NON_CLASSIFIED_DEBUG(DEBUG, false),
	
	/**
	 * Used to log non-classified messages (usually not a good idea) with the given
	 * severity.
	 * <p>
	 * Non-classified messages are supposed to specify their own String classifiers 
	 * (e.g. for throttling).
	 * 
	 * @deprecated these aren't supposed to be used directly, instead look into
	 * 		{@link BaseLoggingUtility#logNonClassifiedMessage(LogMessageSeverity, String, Throwable, Object...)}
	 */
	@Deprecated
	NON_CLASSIFIED_INFO(INFO, false),
	
	/**
	 * Used to log non-classified messages (usually not a good idea) with the given
	 * severity.
	 * <p>
	 * Non-classified messages are supposed to specify their own String classifiers 
	 * (e.g. for throttling).
	 * 
	 * @deprecated these aren't supposed to be used directly, instead look into
	 * 		{@link BaseLoggingUtility#logNonClassifiedMessage(LogMessageSeverity, String, Throwable, Object...)}
	 */
	@Deprecated
	NON_CLASSIFIED_WARN(WARN, false),
	
	/**
	 * Used to log non-classified messages (usually not a good idea) with the given
	 * severity.
	 * <p>
	 * Non-classified messages are supposed to specify their own String classifiers 
	 * (e.g. for throttling).
	 * 
	 * @deprecated these aren't supposed to be used directly, instead look into
	 * 		{@link BaseLoggingUtility#logNonClassifiedMessage(LogMessageSeverity, String, Throwable, Object...)}
	 */
	@Deprecated
	NON_CLASSIFIED_EXTERNAL_INFO(EXTERNAL_INFO, false),
	
	/**
	 * Used to log non-classified messages (usually not a good idea) with the given
	 * severity.
	 * <p>
	 * Non-classified messages are supposed to specify their own String classifiers 
	 * (e.g. for throttling).
	 * 
	 * @deprecated these aren't supposed to be used directly, instead look into
	 * 		{@link BaseLoggingUtility#logNonClassifiedMessage(LogMessageSeverity, String, Throwable, Object...)}
	 */
	@Deprecated
	NON_CLASSIFIED_EXTERNAL_WARN(EXTERNAL_WARN, false),
	
	/**
	 * Used to log non-classified messages (usually not a good idea) with the given
	 * severity.
	 * <p>
	 * Non-classified messages are supposed to specify their own String classifiers 
	 * (e.g. for throttling).
	 * 
	 * @deprecated these aren't supposed to be used directly, instead look into
	 * 		{@link BaseLoggingUtility#logNonClassifiedMessage(LogMessageSeverity, String, Throwable, Object...)}
	 */
	@Deprecated
	NON_CLASSIFIED_EXTERNAL_ERROR(EXTERNAL_ERROR, false),
	
	/**
	 * Used to log non-classified messages (usually not a good idea) with the given
	 * severity.
	 * <p>
	 * Non-classified messages are supposed to specify their own String classifiers 
	 * (e.g. for throttling).
	 * 
	 * @deprecated these aren't supposed to be used directly, instead look into
	 * 		{@link BaseLoggingUtility#logNonClassifiedMessage(LogMessageSeverity, String, Throwable, Object...)}
	 */
	@Deprecated
	NON_CLASSIFIED_EXTERNAL_DATA_LOSS(EXTERNAL_DATA_LOSS, false),
	
	/**
	 * Used to log non-classified messages (usually not a good idea) with the given
	 * severity.
	 * <p>
	 * Non-classified messages are supposed to specify their own String classifiers 
	 * (e.g. for throttling).
	 * 
	 * @deprecated these aren't supposed to be used directly, instead look into
	 * 		{@link BaseLoggingUtility#logNonClassifiedMessage(LogMessageSeverity, String, Throwable, Object...)}
	 */
	@Deprecated
	NON_CLASSIFIED_ERROR(ERROR, false),
	
	/**
	 * Used to log non-classified messages (usually not a good idea) with the given
	 * severity.
	 * <p>
	 * Non-classified messages are supposed to specify their own String classifiers 
	 * (e.g. for throttling).
	 * 
	 * @deprecated these aren't supposed to be used directly, instead look into
	 * 		{@link BaseLoggingUtility#logNonClassifiedMessage(LogMessageSeverity, String, Throwable, Object...)}
	 */
	@Deprecated
	NON_CLASSIFIED_DATA_LOSS(DATA_LOSS, false),
	
	/**
	 * Used to log non-classified messages (usually not a good idea) with the given
	 * severity.
	 * <p>
	 * Non-classified messages are supposed to specify their own String classifiers 
	 * (e.g. for throttling).
	 * 
	 * @deprecated these aren't supposed to be used directly, instead look into
	 * 		{@link BaseLoggingUtility#logNonClassifiedMessage(LogMessageSeverity, String, Throwable, Object...)}
	 */
	@Deprecated
	NON_CLASSIFIED_FATAL(CRITICAL, false),
	
	
	
	/**
	 * Code assertion failed, functionality may be seriously compromised.
	 * <p>
	 * Arguments: exception, text message with information
	 */
	ASSERTION_FAILED(CRITICAL),
	
	
	
	// =========================================================================
	// ==================  BELOW IS APPLICATION-SPECIFIC STUFF =================
	// =========================================================================
	
	
	// These are purely to support logging subsystem testing
	TEST_TRACE(TRACE),
	TEST_DEBUG(DEBUG),
	TEST_INFO(INFO),
	TEST_WARN(WARN),
	TEST_EXTERNAL_INFO(EXTERNAL_INFO),
	TEST_EXTERNAL_WARN(EXTERNAL_WARN),
	TEST_EXTERNAL_ERROR(EXTERNAL_ERROR),
	TEST_EXTERNAL_DATA_LOSS(EXTERNAL_DATA_LOSS),
	TEST_ERROR(ERROR),
	TEST_DATA_LOSS(DATA_LOSS),
	TEST_CRITICAL(CRITICAL),
	
	
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
	 * Application was started
	 * <p>
	 * Arguments: none
	 */
	STARTED(INFO),

	/**
	 * Application shutdown requested
	 * <p>
	 * Arguments: none
	 */
	SHUTDOWN_REQUESTED(INFO),
	
	/**
	 * Application was shutdown
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
	 * Whether this message is throttled by its message ordinal; if not, then
	 * messages of this type need further classification.
	 * <p>
	 * For these (non-ordinal-throttled) messages first message argument is 
	 * expected to be a non-null String classifier that is used to distinguish 
	 * between message types for throttling.
	 * 
	 * @see BaseLoggingUtility#logNonClassified(LogMessageSeverity, String, Throwable, Object...)
	 */
	@Getter
	private final boolean throttledByMessageOrdinal;
	
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
		this(severity, true/*throttled-by-ordinal*/);
	}
	
	/**
	 * Constructor for throttle-able message types.
	 */
	private ExampleLogMessage(LogMessageSeverity severity, boolean isThrottledByMessageOrdinal)
	{
		this(severity, isThrottledByMessageOrdinal, true/*can be throttled*/, true/*update stats*/);
	}
}
