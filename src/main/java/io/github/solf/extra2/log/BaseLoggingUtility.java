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

import static io.github.solf.extra2.util.NullUtil.nnChecked;
import static org.eclipse.jdt.annotation.DefaultLocation.FIELD;
import static org.eclipse.jdt.annotation.DefaultLocation.PARAMETER;
import static org.eclipse.jdt.annotation.DefaultLocation.RETURN_TYPE;
import static org.eclipse.jdt.annotation.DefaultLocation.TYPE_ARGUMENT;
import static org.eclipse.jdt.annotation.DefaultLocation.TYPE_BOUND;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import io.github.solf.extra2.exception.AssertionException;
import io.github.solf.extra2.stacktrace.StackTrace;
import io.github.solf.extra2.util.TypeUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;

/**
 * A base implementation of logging/error reporting for applications that 
 * supports message throttling and monitoring (i.e. counts of warnings/errors/etc).
 * <p>
 * By far the easiest way to use this should be to copy paste example files
 * (under 'test') -- ExampleLogMessage and ExampleLoggingUtility
 * <p> 
 * Generally speaking you subclass this, implement required methods, and then
 * you have option to override other methods to further customize the behavior.
 * <p>
 * Typical targets for overrides are:
 * <p>
 * {@link #spiGetLogger(Object, Throwable, Object...)} -- to specify logger for
 * messages rather than using the default one (which is {@link BaseLoggingUtility})
 * <p>
 * {@link #logNonClassified(LogMessageSeverity, String, Throwable, Object...)} --
 * override and make public if you want to allow logging of non-classified
 * (non-typeized) messages
 *
 * @author Sergey Olefir
 * 
 * @param <LogMessageType> indicates the type used for log messages (could be
 * 		e.g. an enum) 
 */
@NonNullByDefault({PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT}) // exclude ARRAY_CONTENTS because we accept null msg args
public abstract class BaseLoggingUtility<@Nonnull LogMessageType>
{
	/**
	 * Config for this logging utility.
	 */
	protected final LoggingConfig config;
	
	/**
	 * Pre-prepared common naming prefix for all logging messages.
	 */
	protected final String commonNamingPrefix;
	
	/**
	 * Cached status of the logging if previously calculated.
	 */
	@Nullable
	protected volatile LoggingStatus cachedStatus;
	
	/**
	 * Stats for this logging utility.
	 * 
	 * @deprecated use {@link #getStats()} instead; deprecated only to ensure no
	 * 		accidental access to the field
	 */
	@Deprecated
	protected final LogStats internalStatsField = new LogStats();
	/**
	 * Gets stats for this logging utility instance, separated in a method in case
	 * subclasses want to use their own implementation.
	 */
	protected LogStats getStats()
	{
		return internalStatsField;
	}
	
	/**
	 * Logged used by default implementation in {@link #spiGetLogger(LogMessageType, Throwable, Object...)}
	 * AND as final fallback if logging fails repeatedly.
	 */
	private static final Logger defaultLog = LoggerFactory.getLogger(BaseLoggingUtility.class);		
	
	/**
	 * Stats collected by this logging utility
	 * <p>
	 * Fields are not final in case there needs to be a subclass of {@link LogStats}
	 * that replaces some of the monitors.
	 * <p>
	 * Class and fields are public in order to make it more realistic for subclasses
	 * to override behavior if needed (such as put custom handling on value changes
	 * or something).
	 * <p>
	 * This class is still only intended to be used internally for stats collection.
	 */
	public static class LogStats
	{
		/**
		 * Maximum ordinal for the {@link LogMessageSeverity}
		 */
		public static final int MAX_SEVERITY_ORDINAL;
		static
		{
			{
				int maxOrdinal = 1;
				for (LogMessageSeverity entry : LogMessageSeverity.values())
				{
					maxOrdinal = Math.max(maxOrdinal, entry.ordinal());
				}
				
				MAX_SEVERITY_ORDINAL = maxOrdinal;
			}
		}
		
		
		
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
		 * Indicates a problem caused by invalid user input.
		 * <p>
		 * These messages usually indicate that there was no data loss (aside from
		 * the invalid input itself which may be lost).
		 */
		public AtomicLong msgInvalidUserInputCount = new AtomicLong(0);
		
		/**
		 * Indicates an error probably caused by external factors, such
		 * as underlying storage failing.
		 * <p>
		 * These messages usually indicate that there was no data loss (yet).
		 */
		public AtomicLong msgExternalErrorCount = new AtomicLong(0);
		
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
		public AtomicLong msgSecurityErrorCount = new AtomicLong(0);
		
		/**
		 * Indicates an error probably caused by external factors, such
		 * as underlying storage failing.
		 * <p>
		 * This is used when data loss is highly likely, e.g. when implementation
		 * gives up on writing piece of data to the underlying storage.
		 */
		public AtomicLong msgExternalDataLossCount = new AtomicLong(0);
		
		/**
		 * Indicates an error which is likely to be caused by the 
		 * problems and/or unexpected behavior in the program code itself.
		 * <p>
		 * Data loss is likely although this should not be fatal.
		 */
		public AtomicLong msgErrorCount = new AtomicLong(0);
		
		/**
		 * Indicates an error (which is not likely caused by external factors) that
		 * is likely to cause data loss.
		 * <p>
		 * This is used when data loss is highly likely, e.g. when there's a
		 * state in the program that cannot be resolved while ensuring all data
		 * is preserved. 
		 */
		public AtomicLong msgDataLossCount = new AtomicLong(0);
		
		/**
		 * Indicates a critical error (that might well be fatal), meaning the
		 * software may well become unusable after this happens. 
		 */
		public AtomicLong msgCriticalCount = new AtomicLong(0);
		
		/**
		 * Collects last message timestamps per each severity in {@link LogMessageSeverity}
		 * <p>
		 * It is set to 0 until first matching message happens.
		 * <p>
		 * NOTE: these are tracked even if the message itself is not logged due
		 * to log severity settings or something.
		 */
		public AtomicLong[] lastTimestampMsgPerSeverityOrdinal;
		
		/**
		 * Collects last message text per each severity in {@link LogMessageSeverity}
		 * <p>
		 * It's {@link AtomicReference} contains null until matching message happens.
		 * <p>
		 * NOTE: these are tracked ONLY if message is actually sent to logging,
		 * i.e. if it passes severity & throttling check.
		 */
		public AtomicReference<@Nullable String>[] lastLoggedTextMsgPerSeverityOrdinal;
		
		/**
		 * Constructor.
		 */
		public LogStats()
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
		}
		
	}
	

	/**
	 * Constructor.
	 */
	public BaseLoggingUtility(LoggingConfig config) 
	{
		this.config = config;
		{
			String p = config.getCommonNamingPrefix();
			this.commonNamingPrefix = p.isEmpty() ? "" : p + " ";
		}
		
		
		// Initialize logging stuff
		{
			int maxOrdinal = onceGetMaxMessageOrdinal();
			
			messageTypeCountersArray = TypeUtil.coerce(new AtomicReference[maxOrdinal + 1]); // must be + 1 for last index to work!
			for (int i = 0; i < messageTypeCountersArray.length; i++)
				messageTypeCountersArray[i] = new AtomicReference<>(new LogMessageTypeLoggingCounter(0));
		}
	}
	
	/**
	 * DATA LOSS marker -- used when there was a loss of data.
	 */
	protected static final Marker datalossMarker = MarkerFactory.getMarker("DATALOSS");
	
	/**
	 * CRITICAL marker -- used in preference to FATAL because FATAL originally
	 * meant application shutdown (which is not in the purview of this library).
	 */
	protected static final Marker criticalMarker = MarkerFactory.getMarker("CRITICAL");
	
	/**
	 * Returns logger used by this instance for this particular event logging
	 * <p>
	 * Default implementation just returns {@link #defaultLog} which is
	 * based of our own class name. 
	 */
	@SuppressWarnings("unused")
	protected Logger spiGetLogger(LogMessageType msg, @Nullable Throwable exception, Object... args)
		throws InterruptedException
	{
		return defaultLog;
	}

	/**
	 * Extendable implementation of logging of events.
	 * <p>
	 * Default implementation simply sends them to throttled logging in 
	 * {@link #spiLogMessage_Throttled(LogMessageType, Throwable, Object...)}
	 */
	protected void spiLogMessage(LogMessageType msg, @Nullable Throwable exception, Object... args)
		throws InterruptedException
	{
		spiLogMessage_Throttled(msg, exception, args);
	}
	
	/**
	 * Implementation of event logging that just logs the event to the logger
	 * without any additional processing or checking.
	 * <p>
	 * This method uses logger retrieved from {@link #spiGetLogger(LogMessageType, Throwable, Object...)}
	 */
	protected void spiLogMessage_Plain(LogMessageType msg, @Nullable Throwable exception, Object... args)
		throws InterruptedException
	{
		Logger theLog = spiGetLogger(msg, exception, args);
		
		spiLogMessage_Plain(theLog, msg, exception, args);
	}
	
	/**
	 * Implementation of event logging that just logs the event to the given logger
	 * without any additional processing or checking.
	 */
	protected void spiLogMessage_Plain(Logger theLog, LogMessageType msg, @Nullable Throwable exception, Object... args)
		throws InterruptedException
	{
		final LogMessageSeverity severity = getMessageSeverity(msg);
		
		switch(severity)
		{
			case TRACE:
				if (!theLog.isTraceEnabled())
					return;
				break;
			case DEBUG:
				if (!theLog.isDebugEnabled())
					return;
				break;
			case INFO:
			case EXTERNAL_INFO:
				if (!theLog.isInfoEnabled())
					return;
				break;
			case WARN:
			case EXTERNAL_WARN:
			case INVALID_USER_INPUT:
				if (!theLog.isWarnEnabled())
					return;
				break;
			case ERROR:
			case DATA_LOSS:
			case EXTERNAL_DATA_LOSS:
			case EXTERNAL_ERROR:
			case SECURITY_ERROR:
			case CRITICAL:
				if (!theLog.isErrorEnabled())
					return;
				break;
		}
		
		spiLogMessage_FinalFormatAndLogMessage(theLog, msg, exception, args);
	}

	/**
	 * This is invoked when the decision was made that message should be logged;
	 * it performs the final formatting of the message and the actually logs it.
	 */
	protected void spiLogMessage_FinalFormatAndLogMessage(
		Logger theLog, LogMessageType msg, @Nullable Throwable exception,
		Object... args)
		throws InterruptedException
	{
		String formattedMsg = spiLogMessage_FormatAndTrackMessage(theLog, msg, exception, args);
		
		spiLogMessage_FinalLogMessage(theLog, formattedMsg, msg, exception);
	}

	/**
	 * This is invoked to finally log the message (after message text was already
	 * formatted).
	 */
	protected void spiLogMessage_FinalLogMessage(Logger theLog,
		String formattedMsg, LogMessageType msg, @Nullable Throwable exception,
		@SuppressWarnings("unused") Object... args)
	{
		switch(getMessageSeverity(msg))
		{
			case TRACE:
				if (exception != null)
					theLog.trace(formattedMsg, exception);
				else
					theLog.trace(formattedMsg);
				break;
			case DEBUG:
				if (exception != null)
					theLog.debug(formattedMsg, exception);
				else
					theLog.debug(formattedMsg);
				break;
			case INFO:
			case EXTERNAL_INFO:
				if (exception != null)
					theLog.info(formattedMsg, exception);
				else
					theLog.info(formattedMsg);
				break;
			case WARN:
			case EXTERNAL_WARN:
			case INVALID_USER_INPUT:
				if (exception != null)
					theLog.warn(formattedMsg, exception);
				else
					theLog.warn(formattedMsg);
				break;
			case ERROR:
			case EXTERNAL_ERROR:
			case SECURITY_ERROR:
				if (exception != null)
					theLog.error(formattedMsg, exception);
				else
					theLog.error(formattedMsg);
				break;
			case DATA_LOSS:
			case EXTERNAL_DATA_LOSS:
				if (exception != null)
					theLog.error(datalossMarker, formattedMsg, exception);
				else
					theLog.error(datalossMarker, formattedMsg);
				break;
			case CRITICAL:
				if (exception != null)
					theLog.error(criticalMarker, formattedMsg, exception);
				else
					theLog.error(criticalMarker, formattedMsg);
				break;
		}
	}
	
	/**
	 * Formats a given message for the output
	 */
	@SuppressWarnings("unused")
	protected String spiLogMessage_FormatMessage(Logger log, LogMessageType msg, @Nullable Throwable exception, Object... args)
		throws InterruptedException
	{
		StringBuilder sb = new StringBuilder(100);
		sb.append(commonNamingPrefix);
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
	protected String spiLogMessage_FormatAndTrackMessage(Logger log, LogMessageType msg, @Nullable Throwable exception, Object... args)
		throws InterruptedException
	{
		String msgText = spiLogMessage_FormatMessage(log, msg, exception, args);
		
		getStats().lastLoggedTextMsgPerSeverityOrdinal[getMessageSeverity(msg).ordinal()].set(msgText);
		
		return msgText;
	}
	
	
	/**
	 * Used to track how many times specific message type has been logged already
	 * in order to decide when to throttle.
	 */
	@RequiredArgsConstructor
	@ToString
	protected static class LogMessageTypeLoggingCounter
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
	 * Used to track message stats via {@link LogMessageType#ordinal()}
	 */
	protected final AtomicReference<LogMessageTypeLoggingCounter>[] messageTypeCountersArray;
	
	/**
	 * Used to track message stats via classificators in {@link #logNonClassified(LogMessageSeverity, String, Throwable, Object...)}
	 */
	protected final ConcurrentHashMap<String, AtomicReference<LogMessageTypeLoggingCounter>> messageClassificatorCountersMap = 
		new ConcurrentHashMap<String, AtomicReference<LogMessageTypeLoggingCounter>>();
	
	/**
	 * Implementation of event logging that only logs messages so long as there
	 * weren't 'too many' messages of a particular kind logged already.
	 * <p>
	 * If message is not throttled, it is sent to {@link #spiLogMessage_Plain(LogMessageType, Throwable, Object...)}
	 * for actual logging.
	 */
	protected void spiLogMessage_Throttled(LogMessageType msg, @Nullable Throwable exception, Object... args)
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
		else if (!spiLogMessage_IsMessageCanBeThrottled(msg, exception, args))
		{
			// No throttling for messages that don't have throttling enabled
			logMessage = true;
			msgId = msg.toString();
		}
		else
		{
			final AtomicReference<LogMessageTypeLoggingCounter> reference;
			if (isThrottledByMessageOrdinal(msg))
			{
				reference = messageTypeCountersArray[getMessageOrdinal(msg)];
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
					throw new IllegalArgumentException("Non-standard message type [" + msg + "] must provide String classificator as first argument, got: " + Arrays.toString(args), e);
				}
				
				msgId = classificator + '_' + getMessageSeverity(msg); // append severity in case same classificator is used with different severities
				reference = messageClassificatorCountersMap.computeIfAbsent(msgId, k -> new AtomicReference<>(new LogMessageTypeLoggingCounter(0)));
			}
			
			LogMessageTypeLoggingCounter throttleData = reference.get();
			
			long now = timeNow();
			
			gap = timeGapVirtual(throttleData.getPeriodStartTime(), now);
			
			throttleIntervalDuration = config.getLogThrottleTimeInterval();
			
			if (gap > throttleIntervalDuration)
			{
				// Need to create a new counter data.
				LogMessageTypeLoggingCounter newThrottleData = new LogMessageTypeLoggingCounter(now);
				
				if (reference.compareAndSet(throttleData, newThrottleData))
				{
					// using our instance
					int oldCount = throttleData.getMessageCounter().get();
					if (oldCount > msgLimit)
					{
						// Log approximately how many messages were skipped
						logThrottledMessagesWereSkipped(msgId, (oldCount - msgLimit));
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
			spiLogMessage_Plain(msg, exception, args);
		}
		if (logNextMessagesMayBeSkipped)
		{
			// Log that further messages may be skipped for X ms
			logThrottledMessagesMayBeSkipped(msgId, (throttleIntervalDuration - gap));
		}
	}

	
	/**
	 * Determines whether the given message is a 'throttling message'.
	 * Throttling messages are handled differently, e.g. they don't participate
	 * in throttling and don't affect message tracking.
	 */
	@SuppressWarnings("unused")
	protected boolean spiLogMessage_IsMessageCanBeThrottled(LogMessageType msg, @Nullable Throwable exception, Object... args)
	{
		return isMessageCanBeThrottled(msg);
	}
	
	/**
	 * Invoked when there's message to log.
	 * <p>
	 * This method is used to provide a 'guard' against failures around the
	 * {@link #spiLogMessage(LogMessageType, Throwable, Object...)}
	 * method, so usually SHOULD NOT be overridden.
	 * <p>
	 * WARNING: this can (and should be able to) throw {@link InterruptedException},
	 * however since dealing with it in most places is not required (it should
	 * just propagate upwards to indicate potential thread termination), it uses
	 * {@link SneakyThrows}
	 */
	@SneakyThrows(InterruptedException.class) // see comment above
	public void log(@NonNull LogMessageType msg, @Nullable Throwable exception, Object... args)
	{
		try
		{
			if (isUpdateStatsForMessage(msg, exception, args))
			{
				// Record message timestamp
				LogMessageSeverity severity = getMessageSeverity(msg);
				getStats().lastTimestampMsgPerSeverityOrdinal[severity.ordinal()].set(timeNow());
				
				switch (severity)
				{
					case TRACE:
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
					case INVALID_USER_INPUT:
						getStats().msgInvalidUserInputCount.incrementAndGet();
						break;
					case EXTERNAL_ERROR:
						getStats().msgExternalErrorCount.incrementAndGet();
						break;
					case SECURITY_ERROR:
						getStats().msgSecurityErrorCount.incrementAndGet();
						break;
					case EXTERNAL_DATA_LOSS:
						getStats().msgExternalDataLossCount.incrementAndGet();
						break;
					case ERROR:
						getStats().msgErrorCount.incrementAndGet();
						break;
					case DATA_LOSS:
						getStats().msgDataLossCount.incrementAndGet();
						break;
					case CRITICAL:
						getStats().msgCriticalCount.incrementAndGet();
						break;
				}
			}
			
			spiLogMessage(msg, exception, args);
		} catch (Throwable loggingException)
		{
			if (loggingException instanceof ThreadDeath)
				throw loggingException;
			if (loggingException instanceof InterruptedException) // this may be used to indicate that thread should exit 
				throw loggingException;
			
			Throwable excToLog = loggingException;
			
			getStats().msgErrorCount.incrementAndGet();
			
			// Logging failed, try to log that fact, but it may well fail itself
			// TO-DO monitor
			boolean useFallbackLogging = false;
			String exStack = StackTrace.getShortExceptionStackTrace(excToLog);
			if (exStack.contains(".logMessageLoggingFailed("))
			{
				// This means we are in the recursive failure, abort.
				useFallbackLogging = true;
				excToLog = new IllegalStateException("Recursive failure of logMessageLoggingFailed() method: " + excToLog, excToLog);
			}
			
			if (!useFallbackLogging)
			{
				try
				{
					logMessageLoggingFailed(excToLog);
				} catch (Exception e2)
				{
					useFallbackLogging = true; 
				}
			}
			
			if (useFallbackLogging)
			{
				// We can't use standard logging mechanism here because it just failed
				// so just log it directly
				getStats().msgErrorCount.incrementAndGet();
				try
				{
					defaultLog.error("LOGGING FAILED for: " + msg + ": " + excToLog, excToLog);
				} catch (Exception e3)
				{
					// ignore this
				}
				
			}
		}
	}
	
	
	/**
	 * Invoked when there's message to log that doesn't fit any of the 'standard' 
	 * message categories -- usually this should not be used, however it is
	 * available in case it is needed.
	 * <p> 
	 * If this is to be used, subclass should override the method and make it
	 * public.
	 * 
	 * @param classifier non-null string used for message classification, e.g.
	 * 		for message throttling
	 */
	protected void logNonClassified(LogMessageSeverity severity, @NonNull String classifier, @Nullable Throwable exception, Object... args)
	{
		LogMessageType msg = getStandardMessageForNonStandardMessage(severity, classifier, exception, args);
		
		Object[] newArgs = new @Nonnull Object[args.length + 1];
		newArgs[0] = classifier;
		if (args.length > 0)
			System.arraycopy(args, 0, newArgs, 1, args.length);
		
		log(msg, exception, newArgs);
	}
	
	
	/**
	 * Overriding this method allows to change how logging utility processes time internally.
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
	 * Must return maximum message ordinal that will be used by this logging
	 * utility.
	 * <p>
	 * Ordinals are used for throttling and maximum value is required to
	 * pre-allocate throttling data array.
	 * <p>
	 * Only called once (during initialization).
	 */
	protected abstract int onceGetMaxMessageOrdinal();
	
	/**
	 * Returns message severity for the given log message.
	 */
	protected abstract LogMessageSeverity getMessageSeverity(LogMessageType msg);
	
	/**
	 * Returns message ordinal for the given log message.
	 * <p>
	 * Ordinals are used for throttling and the value must not exceed value in
	 * {@link #onceGetMaxMessageOrdinal()}
	 */
	protected abstract int getMessageOrdinal(LogMessageType msg);
	
	/**
	 * Returns whether the given message is throttled by the message ordinal (if your 
	 * implementation supports {@link #logNonClassified(LogMessageSeverity, String, Throwable, Object...)},
	 * then those messages likely will not be throttled by the message ordinal)
	 * <p>
	 * This affects how throttling is evaluated, for messages not throttled by
	 * ordinal it is based on classificator + severity instead.
	 */
	protected abstract boolean isThrottledByMessageOrdinal(LogMessageType msg);
	
	/**
	 * Returns whether the given message can be throttled.
	 * <p>
	 * Throttling-related messages MUST NOT return true here, otherwise it'll
	 * result in an infinite loop!
	 */
	protected abstract boolean isMessageCanBeThrottled(LogMessageType msg);
	
	/**
	 * Invoked after some messages were skipped due to throttling, expected to
	 * log this information.
	 * <p>
	 * If it logs via another message type it absolutely MUST NOT be throttle-able
	 * itself via {@link #isMessageCanBeThrottled(Object)} as this can result
	 * in infinite loops.
	 * 
	 * @param msgId string identification of the message, it can be LogMessageType.toString()
	 * 		or combination or classificator & log message severity for non-standard
	 * 		messages (if supported)
	 */
	protected abstract void logThrottledMessagesWereSkipped(String msgId, int approximateCount);
	
	/**
	 * Invoked when throttling limit was reached for the given message type and
	 * further messages (if any) may be skipped in the future.
	 * <p>
	 * If it logs via another message type it absolutely MUST NOT be throttle-able
	 * itself via {@link #isMessageCanBeThrottled(Object)} as this can result
	 * in infinite loops.
	 * 
	 * @param msgId string identification of the message, it can be LogMessageType.toString()
	 * 		or combination or classificator & log message severity for non-standard
	 * 		messages (if supported)
	 * @param timeIntervalMs for how long the messages may be skipped in the future
	 */
	protected abstract void logThrottledMessagesMayBeSkipped(String msgId, long timeIntervalMs);
	
	/**
	 * Invoked when logging message has failed for some reason (it shouldn't
	 * really happen).
	 * <p>
	 * If this logs another message, that message probably should not be
	 * throttle-able to avoid potential looping issues due to throttling issues.
	 */
	protected abstract void logMessageLoggingFailed(Throwable loggingException);
	
	/**
	 * Returns whether stats should be updated based on processing the given
	 * message.
	 * <p>
	 * Typically you do not want to update stats when processing log-throttling
	 * messages that may be generated in {@link #logThrottledMessagesMayBeSkipped(String, long)}
	 * and {@link #logThrottledMessagesWereSkipped(String, int)}
	 */
	protected abstract boolean isUpdateStatsForMessage(LogMessageType msg, @Nullable Throwable exception, Object... args);

	/**
	 * If you're supporting {@link #logNonClassified(LogMessageSeverity, String, Throwable, Object...)},
	 * then this should return an appropriate standard message type that will
	 * be used to log the particular non-standard message.
	 */
	protected abstract LogMessageType getStandardMessageForNonStandardMessage(LogMessageSeverity severity, String classifier, @Nullable Throwable exception, Object... args);
	
	
	/**
	 * Gets logging status (e.g. for monitoring).
	 * 
	 * @param maximum age for the retrieved status -- previously calculated status
	 * 		is cached so it can be re-used if it is not older that the given
	 * 		maximum age; this age is in 'virtual' ms, i.e. affected by {@link #timeFactor()};
	 * 		use 0 to disable caching
	 * 
	 * @throws IllegalArgumentException if maximum age is negative
	 */
	public LoggingStatus getStatus(final long maximumAgeMsVirtual)
		throws IllegalArgumentException
	{
		if (maximumAgeMsVirtual < 0)
			throw new IllegalArgumentException("Maximum age must be non-negative, got: " + maximumAgeMsVirtual);
		
		final long now = timeNow();
		if (maximumAgeMsVirtual > 0)
		{
			LoggingStatus status = cachedStatus;
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
		LogStats cacheStats = getStats();
		synchronized (cacheStats)
		{
			// Double-check if status was updated concurrently and is maybe valid now
			if (maximumAgeMsVirtual > 0)
			{
				LoggingStatus status = cachedStatus;
				if (status != null)
				{
					long validUntil = timeAddVirtualIntervalToRealWorldTime(
						status.getStatusCreatedAt(), maximumAgeMsVirtual);
					
					if (validUntil >= now)
						return status; 
				}
			}
			
			// Need to build a new status
			long msgWarnCount = cacheStats.msgWarnCount.get();
			long msgExternalWarnCount = cacheStats.msgExternalWarnCount.get();
			long msgInvalidUserInputCount = cacheStats.msgInvalidUserInputCount.get();
			long msgExternalErrorCount = cacheStats.msgExternalErrorCount.get();
			long msgExternalDataLossCount = cacheStats.msgExternalDataLossCount.get();
			long msgDataLossCount = cacheStats.msgDataLossCount.get();
			long msgErrorCount = cacheStats.msgErrorCount.get();
			long msgSecurityErrorCount = cacheStats.msgSecurityErrorCount.get();
			long msgCriticalCount = cacheStats.msgCriticalCount.get();
			
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
			long lastDataLossMsgTimestamp = 0;
			@Nullable String lastDataLossLoggedMsgText = null;
			long lastCriticalMsgTimestamp = 0;
			@Nullable String lastCriticalLoggedMsgText = null;
			for (LogMessageSeverity severity : LogMessageSeverity.values())
			{
				int index = severity.ordinal();
				switch (severity)
				{
					case TRACE:
					case DEBUG:
					case EXTERNAL_INFO:
					case INFO:
						continue;
					case EXTERNAL_WARN:
					case WARN:
					case INVALID_USER_INPUT:
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
					case EXTERNAL_ERROR:
					case SECURITY_ERROR:
						{
							long ts = lastTimestampMsgPerSeverityOrdinal[index];
							if (ts > lastErrorMsgTimestamp)
							{
								lastErrorMsgTimestamp = ts;
								lastErrorLoggedMsgText = lastLoggedTextMsgPerSeverityOrdinal[index];
							}
						}
						continue;
					case DATA_LOSS:
					case EXTERNAL_DATA_LOSS:
					{
						long ts = lastTimestampMsgPerSeverityOrdinal[index];
						if (ts > lastDataLossMsgTimestamp)
						{
							lastDataLossMsgTimestamp = ts;
							lastDataLossLoggedMsgText = lastLoggedTextMsgPerSeverityOrdinal[index];
						}
					}
					continue;
					case CRITICAL:
						{
							long ts = lastTimestampMsgPerSeverityOrdinal[index];
							if (ts > lastCriticalMsgTimestamp)
							{
								lastCriticalMsgTimestamp = ts;
								lastCriticalLoggedMsgText = lastLoggedTextMsgPerSeverityOrdinal[index];
							}
						}
						continue;
				}
				
				throw new AssertionException("code should not be reachable");
			}
			
			final long totalDataLossOrHigherCount = msgExternalDataLossCount + msgDataLossCount + msgCriticalCount;
			final long totalErrorOrHigherCount = totalDataLossOrHigherCount + msgExternalErrorCount + msgErrorCount + msgSecurityErrorCount;
			final long totalWarnOrHigherCount = totalErrorOrHigherCount + msgWarnCount + msgExternalWarnCount + msgInvalidUserInputCount; 
			LoggingStatus status = LoggingStatusBuilder
				.statusCreatedAt(now)
				.loggedWarnCount(msgWarnCount)
				.loggedExternalWarnCount(msgExternalWarnCount)
				.loggedInvalidUserInputCount(msgInvalidUserInputCount)
				.loggedExternalErrorCount(msgExternalErrorCount)
				.loggedExternalDataLossCount(msgExternalDataLossCount)
				.loggedSecurityErrorCount(msgSecurityErrorCount)
				.loggedErrorCount(msgErrorCount)
				.loggedDataLossCount(msgDataLossCount)
				.loggedCriticalCount(msgCriticalCount)
				.loggedTotalWarnOrHigherCount(totalWarnOrHigherCount)
				.loggedTotalErrorOrHigherCount(totalErrorOrHigherCount)
				.loggedTotalDataLossOrHigherCount(totalDataLossOrHigherCount)
				
				.lastLoggedTimestampPerSeverityOrdinal(lastTimestampMsgPerSeverityOrdinal)
				.lastLoggedTextPerSeverityOrdinal(lastLoggedTextMsgPerSeverityOrdinal)
				.lastLoggedWarnTimestamp(lastWarnMsgTimestamp)
				.lastLoggedWarnText(lastWarnLoggedMsgText)
				.lastLoggedErrorTimestamp(lastErrorMsgTimestamp)
				.lastLoggedErrorText(lastErrorLoggedMsgText)
				.lastLoggedDataLossTimestamp(lastDataLossMsgTimestamp)
				.lastLoggedDataLossText(lastDataLossLoggedMsgText)
				.lastLoggedCriticalTimestamp(lastCriticalMsgTimestamp)
				.lastLoggedCriticalText(lastCriticalLoggedMsgText)
				
				.buildLoggingStatus()
				;
			
			cachedStatus = status; // cache status
			return status;
		}
	}
}
