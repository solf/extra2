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

import org.eclipse.jdt.annotation.NonNullByDefault;

import lombok.Getter;

/**
 * Possible message severities supported by the {@link BaseLoggingUtility}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public enum LogMessageSeverity
{
	TRACE(170),
	DEBUG(270),
	/**
	 * Indicates an info message probably caused by external factors, such
	 * as read retry executing.
	 * <p>
	 * These messages usually indicate that there was no data loss (yet).
	 */
	EXTERNAL_INFO(340),
	INFO(370),
	/**
	 * Indicates an externally-caused warning.
	 * <p>
	 * These messages usually indicate that there was no data loss (yet).
	 */
	EXTERNAL_WARN(440),
	/**
	 * Indicates a problem caused by invalid user input.
	 * <p>
	 * These messages usually indicate that there was no data loss (aside from
	 * the invalid input itself which may be lost).
	 */
	INVALID_USER_INPUT(450),
	/**
	 * Indicates problem that is probably caused by internal somewhat-known
	 * factors, such as potential concurrency/race conditions (which normally
	 * are not expected to occur).
	 * <p>
	 * These usually should not result in data loss.
	 */
	WARN(470),
	/**
	 * Indicates an error probably caused by external factors, such
	 * as underlying storage failing.
	 * <p>
	 * These messages usually indicate that there was no data loss (yet).
	 */
	EXTERNAL_ERROR(540),
	/**
	 * Indicates an error caused by security issues, such as client failing to
	 * provide proper access key or user failing to provide the correct password
	 * (that last one could also reasonably be considered {@link #INVALID_USER_INPUT},
	 * however in many cases it is desirable to separate security issues in order
	 * to monitor attacks and such).
	 * <p>
	 * These messages usually indicate that there was no data loss (aside from
	 * the potentially lost data in the input that had security issue).
	 */
	SECURITY_ERROR(550),
	/**
	 * Indicates an error which is likely to be caused by the 
	 * problems and/or unexpected behavior in the program code itself.
	 * <p>
	 * Data loss is likely although this should not be fatal.
	 */
	ERROR(570),
	/**
	 * Indicates an error probably caused by external factors, such
	 * as underlying storage failing.
	 * <p>
	 * This is used when data loss is highly likely, e.g. when implementation
	 * gives up on writing piece of data to the underlying storage.
	 */
	EXTERNAL_DATA_LOSS(640),
	/**
	 * Indicates an error (which is not likely caused by external factors) that
	 * is likely to cause data loss.
	 * <p>
	 * This is used when data loss is highly likely, e.g. when there's a
	 * state in the program that cannot be resolved while ensuring all data
	 * is preserved. 
	 */
	DATA_LOSS(670),
	/**
	 * Indicates a critical error (that might well be fatal), meaning the
	 * software may well become unusable after this happens. 
	 */
	CRITICAL(770),
	;
	
	/**
	 * Ascending severity index for messages (e.g. TRACE has lower index than
	 * INFO).
	 */
	@Getter
	private final int severityIndex;
	
	/**
	 * Constructor.
	 */
	private LogMessageSeverity(int severityIndex)
	{
		this.severityIndex = severityIndex;
	}
}
