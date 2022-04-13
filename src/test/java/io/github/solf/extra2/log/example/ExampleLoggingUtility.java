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

import static org.eclipse.jdt.annotation.DefaultLocation.FIELD;
import static org.eclipse.jdt.annotation.DefaultLocation.PARAMETER;
import static org.eclipse.jdt.annotation.DefaultLocation.RETURN_TYPE;
import static org.eclipse.jdt.annotation.DefaultLocation.TYPE_ARGUMENT;
import static org.eclipse.jdt.annotation.DefaultLocation.TYPE_BOUND;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;

import io.github.solf.extra2.exception.AssertionException;
import io.github.solf.extra2.log.BaseLoggingUtility;
import io.github.solf.extra2.log.LoggingConfig;
import io.github.solf.extra2.stacktrace.StackTrace;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import io.github.solf.extra2.log.LogMessageSeverity;

/**
 * Example of how to use {@link BaseLoggingUtility} with {@link ExampleLogMessage}
 * enum holding all the possible messages.
 * <p>
 * See the end of the file for the demonstration of some of the optional overrides.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault({PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT}) // exclude ARRAY_CONTENTS because we accept null msg args
@Slf4j
public class ExampleLoggingUtility extends BaseLoggingUtility<ExampleLogMessage>
{
	/**
	 * @param config
	 */
	public ExampleLoggingUtility(LoggingConfig config)
	{
		super(config);
	}

	@Override
	protected int onceGetMaxMessageOrdinal()
	{
		int maxOrdinal = 1;
		for (ExampleLogMessage entry : ExampleLogMessage.values())
		{
			maxOrdinal = Math.max(maxOrdinal, entry.ordinal());
		}
		
		return maxOrdinal;
	}

	@Override
	protected LogMessageSeverity getMessageSeverity(ExampleLogMessage msg)
	{
		return msg.getSeverity();
	}

	@Override
	protected int getMessageOrdinal(ExampleLogMessage msg)
	{
		return msg.ordinal();
	}

	@Override
	protected void logThrottledMessagesWereSkipped(String msgId, int approximateCount)
	{
		log(ExampleLogMessage.LOG_MESSAGE_TYPE_PREVIOUS_MESSAGES_SKIPPED, null, msgId, approximateCount);
	}
	
	@Override
	protected void logThrottledMessagesMayBeSkipped(@Nonnull String msgId,
		long timeIntervalMs)
	{
		log(ExampleLogMessage.LOG_MESSAGE_TYPE_MESSAGES_MAY_BE_SKIPPED_FOR, null, msgId, timeIntervalMs);
	}
	
	@Override
	protected void logMessageLoggingFailed(@Nonnull Throwable loggingException)
	{
		log(ExampleLogMessage.LOG_MESSAGE_FAILED, loggingException);
	}

	@Override
	protected boolean isThrottledByMessageOrdinal(ExampleLogMessage msg)
	{
		return msg.isThrottledByMessageOrdinal();
	}

	@Override
	protected boolean isMessageCanBeThrottled(ExampleLogMessage msg)
	{
		return msg.isMessageCanBeThrottled();
	}

	@Override
	protected boolean isUpdateStatsForMessage(ExampleLogMessage msg,
		@Nullable Throwable exception, Object... args)
	{
		return msg.isUpdateStatsForMessage();
	}

	@Override
	@SuppressWarnings("deprecation") // deprecated is used to mark message instances that are not supposed to be used directly by clients
	protected ExampleLogMessage getStandardMessageForNonStandardMessage(
		LogMessageSeverity severity, String classifier,
		@Nullable Throwable exception, Object... args)
	{
		switch (severity)
		{
			case TRACE:
				return ExampleLogMessage.NON_CLASSIFIED_TRACE;
			case DATA_LOSS:
				return ExampleLogMessage.NON_CLASSIFIED_DATA_LOSS;
			case DEBUG:
				return ExampleLogMessage.NON_CLASSIFIED_DEBUG;
			case ERROR:
				return ExampleLogMessage.NON_CLASSIFIED_ERROR;
			case EXTERNAL_DATA_LOSS:
				return ExampleLogMessage.NON_CLASSIFIED_EXTERNAL_DATA_LOSS;
			case EXTERNAL_ERROR:
				return ExampleLogMessage.NON_CLASSIFIED_EXTERNAL_ERROR;
			case EXTERNAL_INFO:
				return ExampleLogMessage.NON_CLASSIFIED_EXTERNAL_INFO;
			case EXTERNAL_WARN:
				return ExampleLogMessage.NON_CLASSIFIED_EXTERNAL_WARN;
			case CRITICAL:
				return ExampleLogMessage.NON_CLASSIFIED_FATAL;
			case INFO:
				return ExampleLogMessage.NON_CLASSIFIED_INFO;
			case INVALID_USER_INPUT:
				return ExampleLogMessage.NON_CLASSIFIED_INVALID_USER_INPUT;
			case SECURITY_ERROR:
				return ExampleLogMessage.NON_CLASSIFIED_SECURITY_ERROR;
			case WARN:
				return ExampleLogMessage.NON_CLASSIFIED_WARN;
			default:
				break;
		}
		
		throw new AssertionException("This code should not be reacheable!");
	}

	
	// =======================================================================
	// ==================   BELOW IS OPTIONAL OVERRIDE METHODS  ==============
	// =======================================================================
	

	// Override this to use specific logger, rather than the default one for BaseLoggingUtility
	@Override
	protected Logger spiGetLogger(ExampleLogMessage msg,
		@Nullable Throwable exception, Object @Nonnull... args)
		throws InterruptedException
	{
		return log;
	}

	// Override this and make public to allow access to non-classified logging
	@Override
	public void logNonClassified(LogMessageSeverity severity,
		@NonNull String classifier, @Nullable Throwable exception,
		Object @Nonnull... args)
	{
		super.logNonClassified(severity, classifier, exception, args);
	}

	// Example of how you can add context (MDC) information to messages when it is known that they will be logged
	@Override
	protected void spiLogMessage_FinalFormatAndLogMessage(Logger theLog,
		ExampleLogMessage msg, @Nullable Throwable exception,
		Object @Nonnull... args)
		throws InterruptedException
	{
		// Need to skip both self and the parent logging class in stack trace!
		try (MDCCloseable mdc1 = MDC.putCloseable("stackTrace", StackTrace.getShortInvocationTrace(BaseLoggingUtility.class, ExampleLoggingUtility.class)))
		{
			super.spiLogMessage_FinalFormatAndLogMessage(theLog, msg, exception, args);
		}
	}
	
	
}
