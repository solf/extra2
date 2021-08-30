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

/**
 * Possible message severities supported by the {@link BaseLoggingUtility}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public enum LogMessageSeverity
{
	TRACE,
	DEBUG,
	INFO,
	/**
	 * Indicates problem that is probably caused by internal somewhat-known
	 * factors, such as potential concurrency/race conditions (which normally
	 * are not expected to occur).
	 * <p>
	 * These usually should not result in data loss.
	 */
	WARN,
	/**
	 * Indicates an info message probably caused by external factors, such
	 * as read retry executing.
	 * <p>
	 * These messages usually indicate that there was no data loss (yet).
	 */
	EXTERNAL_INFO,
	/**
	 * Indicates an externally-caused warning.
	 * <p>
	 * These messages usually indicate that there was no data loss (yet).
	 */
	EXTERNAL_WARN,
	/**
	 * Indicates an error probably caused by external factors, such
	 * as underlying storage failing.
	 * <p>
	 * These messages usually indicate that there was no data loss (yet).
	 */
	EXTERNAL_ERROR,
	/**
	 * Indicates an error probably caused by external factors, such
	 * as underlying storage failing.
	 * <p>
	 * This is used when data loss is highly likely, e.g. when implementation
	 * gives up on writing piece of data to the underlying storage.
	 */
	EXTERNAL_DATA_LOSS,
	/**
	 * Indicates an error which is likely to be caused by the 
	 * problems and/or unexpected behavior in the program code itself.
	 * <p>
	 * Data loss is likely although this should not be fatal.
	 */
	ERROR,
	/**
	 * Indicates a critical error (that might well be fatal), meaning the
	 * software may well become unusable after this happens. 
	 */
	CRITICAL,
	;
}
