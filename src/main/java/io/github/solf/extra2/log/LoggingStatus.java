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
package io.github.solf.extra2.log;

import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Logging status.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
@ToString
@AllArgsConstructor
public class LoggingStatus
{
	/**
	 * Indicates when status was created (e.g. for caching purposes).
	 */
	@Getter
	private final long statusCreatedAt;
	
	
	/**
	 * Indicates problem that is probably caused by internal somewhat-known
	 * factors, such as potential concurrency/race conditions (which normally
	 * are not expected to occur).
	 * <p>
	 * These usually should not result in data loss.
	 */
	@Getter
	private final long loggedWarnCount;
	
	/**
	 * Indicates an externally-caused warning.
	 * <p>
	 * These messages usually indicate that there was no data loss (yet).
	 */
	@Getter
	private final long loggedExternalWarnCount;
	
	/**
	 * Indicates an error probably caused by external factors, such
	 * as underlying storage failing.
	 * <p>
	 * These messages usually indicate that there was no data loss (yet).
	 */
	@Getter
	private final long loggedExternalErrorCount;
	
	/**
	 * Indicates an error probably caused by external factors, such
	 * as underlying storage failing.
	 * <p>
	 * This is used when data loss is highly likely, e.g. when implementation
	 * gives up on writing piece of data to the underlying storage.
	 */
	@Getter
	private final long loggedExternalDataLossCount;
	
	/**
	 * Indicates an error which is likely to be caused by the 
	 * problems and/or unexpected behavior in the program code itself.
	 * <p>
	 * Data loss is likely although this should not be fatal.
	 */
	@Getter
	private final long loggedErrorCount;
	
	/**
	 * Indicates a critical error (that might well be fatal), meaning the
	 * software may well become unusable after this happens. 
	 */
	@Getter
	private final long loggedCriticalCount;
	
	/**
	 * Total count of messages with severity 'warn' or higher.
	 */
	@Getter
	private final long loggedTotalWarnOrHigherCount;
	
	/**
	 * Total count of messages with severity 'error' or higher.
	 */
	@Getter
	private final long loggedTotalErrorOrHigherCount;
	
	
	/**
	 * Collects last message timestamps per each severity in {@link LogMessageSeverity}
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
	private final long[] lastLoggedTimestampPerSeverityOrdinal;
	
	/**
	 * Collects last message text per each severity in {@link LogMessageSeverity}
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
	private final @Nullable String[] lastLoggedTextPerSeverityOrdinal;
	

	/**
	 * Timestamp for the last message (regardless of whether it was logged)
	 * of the WARN-type severity, 0 if no such messages were logged.
	 */
	@Getter
	private final long lastLoggedWarnTimestamp;
	
	/**
	 * Last logged message text of the WARN-type severity, null if no
	 * such messages were logged (this does not track messages that were not
	 * logged due to low severity or throttling).
	 */
	@Getter
	private final @Nullable String lastLoggedWarnText;
	
	/**
	 * Timestamp for the last message (regardless of whether it was logged)
	 * of the ERROR-type severity, 0 if no such messages were logged.
	 */
	@Getter
	private final long lastLoggedErrorTimestamp;
	
	/**
	 * Last logged message text of the ERROR-type severity, null if no
	 * such messages were logged (this does not track messages that were not
	 * logged due to low severity or throttling).
	 */
	@Getter
	private final @Nullable String lastLoggedErrorText;
	
	/**
	 * Timestamp for the last message (regardless of whether it was logged)
	 * of the CRITICAL-type severity, 0 if no such messages were logged.
	 */
	@Getter
	private final long lastLoggedCriticalTimestamp;
	
	/**
	 * Last logged message text of the CRITICAL-type severity, null if no
	 * such messages were logged (this does not track messages that were not
	 * logged due to low severity or throttling).
	 */
	@Getter
	private final @Nullable String lastLoggedCriticalText;
}
