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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.exception.AssertionException;
import io.github.solf.extra2.log.BaseLoggingUtility;
import io.github.solf.extra2.log.LogConfig;
import io.github.solf.extra2.log.LogMessageSeverity;

/**
 * aaa - class description and otherwise fix comments and stuff
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class ExampleLoggingUtility extends BaseLoggingUtility<ExampleLogMessage>
{
	/**
	 * @param config
	 */
	public ExampleLoggingUtility(LogConfig config)
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
	protected LogMessageSeverity getMessageSeverity(
		ExampleLogMessage msg)
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
		logMessage(ExampleLogMessage.LOG_MESSAGE_TYPE_PREVIOUS_MESSAGES_SKIPPED, null, msgId, approximateCount);
	}
	
	@Override
	protected void logThrottledMessagesMayBeSkipped(@Nonnull String msgId,
		long timeIntervalMs)
	{
		logMessage(ExampleLogMessage.LOG_MESSAGE_TYPE_MESSAGES_MAY_BE_SKIPPED_FOR, null, msgId, timeIntervalMs);
	}
	
	@Override
	protected void logMessageLoggingFailed(@Nonnull Throwable loggingException)
	{
		logMessage(ExampleLogMessage.LOG_MESSAGE_FAILED, loggingException);
	}

	@Override
	protected boolean isStandardMessage(ExampleLogMessage msg)
	{
		return msg.isStandardMessage();
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
			case DEBUG:
				return ExampleLogMessage.NON_STANDARD_DEBUG;
			case ERROR:
				return ExampleLogMessage.NON_STANDARD_ERROR;
			case EXTERNAL_DATA_LOSS:
				return ExampleLogMessage.NON_STANDARD_EXTERNAL_DATA_LOSS;
			case EXTERNAL_ERROR:
				return ExampleLogMessage.NON_STANDARD_EXTERNAL_ERROR;
			case EXTERNAL_INFO:
				return ExampleLogMessage.NON_STANDARD_EXTERNAL_INFO;
			case EXTERNAL_WARN:
				return ExampleLogMessage.NON_STANDARD_EXTERNAL_WARN;
			case CRITICAL:
				return ExampleLogMessage.NON_STANDARD_FATAL;
			case INFO:
				return ExampleLogMessage.NON_STANDARD_INFO;
			case WARN:
				return ExampleLogMessage.NON_STANDARD_WARN;
		}
		
		throw new AssertionException("This code should not be reacheable!");
	}

	
	
}
