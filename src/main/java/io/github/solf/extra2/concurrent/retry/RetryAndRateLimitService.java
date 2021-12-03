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
package io.github.solf.extra2.concurrent.retry;

import static io.github.solf.extra2.util.NullUtil.fakeNonNull;
import static io.github.solf.extra2.util.NullUtil.nn;
import static io.github.solf.extra2.util.NullUtil.nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.javatuples.Pair;

import io.github.solf.extra2.concurrent.InterruptableConsumer;
import io.github.solf.extra2.concurrent.InterruptableRunnable;
import io.github.solf.extra2.concurrent.InterruptableSupplier;
import io.github.solf.extra2.concurrent.WAThreadPoolExecutor;
import io.github.solf.extra2.concurrent.exception.InterruptedRuntimeException;
import io.github.solf.extra2.lambda.SimpleLongCounter;
import io.github.solf.extra2.nullable.NonNullOptional;
import io.github.solf.extra2.nullable.NullableOptional;
import io.github.solf.extra2.thread.ExitableThread;
import io.github.solf.extra2.thread.InterruptHandlingExitableThread;
import io.github.solf.extra2.util.TypeUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;

/**
 * aaa needs status thingy to at least check requests count and if all threads are alive
 * 
 * zzz - class description
 * 
 * zzz - note about after* methods
 * zzz - note about spi* methods
 * 
 * zzz - add cancel option
 * zzz -- spool/flush
 * zzz -- shutdown (option to ignore tickets?)
 * 
 * zzz comments about what exception is a timeout exception
 * 
 * zzz add synopsis to readme
 *
 * @author Sergey Olefir
 */
//Exclude TYPE_ARGUMENT as we will allow null return values.
@NonNullByDefault({DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE, DefaultLocation.FIELD, DefaultLocation.TYPE_BOUND, DefaultLocation.ARRAY_CONTENTS})
public abstract class RetryAndRateLimitService<@Nonnull Input, Output>
{
	/**
	 * Config for this service.
	 */
	protected final RRLConfig config;
	
	/**
	 * Pre-prepared common naming prefix, e.g. "RRLService[cacheName]"
	 * zzz fix comment
	 */
	protected final String commonNamingPrefix;
	
	/**
	 * Number of requests currently in the processing pipeline.
	 * zzz make sure items count is tested
	 */
	protected final AtomicInteger processingRequestsCount = new AtomicInteger(0);
	
	
	/**
	 * Event listener used by this instance, never null.
	 * 
	 * @deprecated should not be accessed directly, see {@link #guardedEventListenerInvocation(InterruptableConsumer, RRLEntry)}
	 */
	@Deprecated
	protected /*non-final to allow subclasses to override*/ RRLEventListener<Input, Output> eventListener;
	
	
	
	/**
	 * Main requests processing queue.
	 * 
	 * TO-DO is this the best choice for the queue here?
	 */
	protected final LinkedBlockingQueue<RRLEntry<Input, Output>> mainQueue = new LinkedBlockingQueue<>();
	
	/**
	 * Delay queue(s).
	 * <p>
	 * This always contains at least one item UNLESS it was overridden in
	 * {@link #createDelayQueues(RRLConfig, String, ThreadGroup)} and in
	 * {@link #delayEntry(RRLEntry, long)}
	 */
	protected final List<RRLDelayQueueData> delayQueues;
	
	

	/**
	 * Thread group used for this cache.
	 */
	protected final ThreadGroup threadGroup;
	
	/**
	 * Thread for processing main queue.
	 */
	protected final ExitableThread mainQueueProcessingThread;
	
	/**
	 * Executor service that is used for processing requests (i.e. potentially
	 * acts as a thread pool).
	 */
	@Nullable // nullable to allow for implementations that don't use dedicated executor service
	protected final ExecutorService requestsExecutorService;
	
	/**
	 * Rate limiter used by this instance.
	 */
	protected final RRLRateLimiter<?> rateLimiter;
	
	
	/**
	 * Possible service control statuses (not_started, running, shutdown...)
	 */
	public static enum RRLServiceControlState
	{
		/**
		 * Cache hasn't been started yet.
		 */
		NOT_STARTED,
		/**
		 * Cache is running (can handle access operations).
		 */
		RUNNING,
		/**
		 * Cache is flushing -- this is mainly useful for tests, tries to flush
		 * all data from cache as soon as possible (similar to shutdown) but does
		 * not shut down cache at the end.
		 * <p>
		 * Standard cache operations (read/write) are NOT allowed during flushing
		 * as it will interfere with flushing itself.
		 * zzz check this is used?
		 */
		FLUSHING,
		/**
		 * Cache shutdown is in progress.
		 */
		SHUTDOWN_IN_PROGRESS,
		/**
		 * Cache has been fully shutdown.
		 */
		SHUTDOWN_COMPLETED,
		;
	}
	
	/**
	 * Decision for the item processed from the delay queue.
	 */
	public static enum RRLDelayQueueProcessingDecision
	{
		/**
		 * Item goes to main queue (i.e. delay has expired or the remaining 
		 * delay is shorter than the current delay queue can provide).
		 */
		MAIN_QUEUE,
		
		/**
		 * Remaining delay is greater than the current queue's, so delay item
		 * again.
		 */
		DELAY_AGAIN,
		;
	}
	
	/**
	 * Decision for the item being processed by the main queue.
	 */
	public static enum RRLMainQueueProcessingDecision
	{
		//zzz comments
		DELAY,
		PROCEED,
		TIMEOUT,
		/**
		 * NOTE: using this will result in {@link RRLFinalFailureDecisionException}
		 * registered in {@link RRLFuture} (as there's no other exception
		 * available).
		 */
		FINAL_FAILURE,
		;
	}
	
	/**
	 * Decision for the item after request attempt has failed.
	 */
	public static enum RRLAfterRequestAttemptFailedDecision
	{
		RETRY,
		TIMEOUT,
		FINAL_FAILURE,
		;
	}
	
	/**
	 * Single request entry handled by this service.
	 * zzz probably needs 'custom client/impl object'
	 * zzz probably needs reference to service (for e.g. event listener and what not)
	 */
	@ToString
	protected static class RRLEntry<@Nonnull Input, Output>
	{
		/**
		 * Parent {@link RetryAndRateLimitService}.
		 */
		@Getter
		private final RetryAndRateLimitService<Input, Output> service;
		
		/**
		 * Input data.
		 */
		@Getter
		private final Input input;
		
		/**
		 * Time when this entry was created (when task was added to RRL service).
		 */
		@Getter
		private final long createdAt;
		
		/**
		 * Every request has some limit as to how long it is valid; after this
		 * duration expires, request should NOT attempt processing anymore.
		 */
		@Getter
		private final long requestValidityDuration;
		
		/**
		 * Completable future used to indicate processing results / request
		 * cancellation.
		 */
		@Getter
		private final RRLCompletableFuture<Input, Output> future; 
		
		/**
		 * Time when entry was placed in delay queue, negative values indicate
		 * it's not in the delay queue.
		 */
		@Getter @Setter
		private long inDelayQueueSince = -1;
		
		/**
		 * Processing of this item should be delayed, this is delay anchor 
		 * (timestamp) used with {@link #getDelayFor()} to process the
		 * actual delay.
		 * <p>
		 * Negative value is used to indicate 'no delay required'.
		 * <p>
		 * 'anchor' + 'for' are used because of potential time modifications
		 * via {@link RetryAndRateLimitService#timeFactor()} 
		 */
		@Getter @Setter
		private long delayAnchor = -1;
		
		/**
		 * Processing of this item should be delayed for this number of virtual
		 * milliseconds, calculated from {@link #getDelayAnchor()}
		 * <p>
		 * Negative value is used to indicate 'no delay required'.
		 * <p>
		 * 'anchor' + 'for' are used because of potential time modifications
		 * via {@link RetryAndRateLimitService#timeFactor()}
		 */
		@Getter @Setter
		private long delayFor = -1;
		
		
		/**
		 * Together with {@link #getEarliestProcessingTimeDelay()} defines
		 * 'earliest processing time' for this item -- item typically should not
		 * be processed before this time.
		 * <p>
		 * Negative value is used to indicate 'no restriction'.
		 * <p>
		 * 'anchor' + 'delay' are used because of potential time modifications
		 * via {@link RetryAndRateLimitService#timeFactor()} 
		 */
		@Getter
		private long earliestProcessingTimeAnchor = -1;
		
		/**
		 * 'Earliest processing time' for this item is defined as
		 * {@link #getEarliestProcessingTimeAnchor()} plus the value of this
		 * field (in virtual milliseconds) -- item typically should not
		 * be processed before this time.
		 * <p>
		 * Negative value is used to indicate 'no restriction'.
		 * <p>
		 * 'anchor' + 'delay' are used because of potential time modifications
		 * via {@link RetryAndRateLimitService#timeFactor()} 
		 */
		@Getter
		private long earliestProcessingTimeDelay = -1;
		
		/**
		 * Sets 'earliest processing' time delay for this entry calculating
		 * given minimum (virtual) ms from the given time anchor.
		 * <p> 
		 * Give -1 value to both arguments to disable the delay.
		 */
		public void setEarliestProcessingTimeDelay(long anchor, long delayVirtualMs)
		{
			this.earliestProcessingTimeAnchor = anchor;
			this.earliestProcessingTimeDelay = delayVirtualMs;
		}
		
		
		/**
		 * Number of times this entry attempted processing and failed (via
		 * exception).
		 */
		@Getter @Setter
		private int numberOfFailedAttempts = 0;
		
		/**
		 * Total time taken to process this request (in virtual ms) -- this is
		 * set once processing is completed (either successfully or not).
		 * <p>
		 * The value of -1 indicates that the request hasn't been completed yet.
		 */
		@Getter @Setter
		private long totalProcessingTime = -1;
		
		/**
		 * Constructor.
		 */
		public RRLEntry(RetryAndRateLimitService<Input, Output> service,
			Input input, long createdAt, long requestValidityDuration)
		{
			this.service = service;
			
			this.input = input;
			this.createdAt = createdAt;
			this.requestValidityDuration = requestValidityDuration;
			
			this.future = new RRLCompletableFuture<>(this); 
		}
	}
	
	/**
	 * Data for a single delay queue.
	 */
	@RequiredArgsConstructor
	protected class RRLDelayQueueData
	{
		/**
		 * Delay provided by this queue.
		 */
		@Getter
		private final long delayMs;
		
		/**
		 * Processing thread.
		 */
		@Getter
		private final ExitableThread processingThread;
		
		/**
		 * Queue itself.
		 */
		private final LinkedBlockingQueue<RRLEntry<Input, Output>> queue;
		
		/**
		 * Adds item to this delay queue (to be delayed).
		 * <p>
		 * Takes care of properly setting {@link RRLEntry#setInDelayQueueSince(long)}
		 */
		public void addToQueue(RRLEntry<Input, Output> entry)
		{
			long now = timeNow();
			
			entry.setInDelayQueueSince(now);
			
			queue.add(entry);
		}
	}
	
	/**
	 * Constructor.
	 */
	public RetryAndRateLimitService(RRLConfig config)
	{
		this.config = config;
		this.commonNamingPrefix = "RRLService[" + config.getServiceName() + "]";
		
		this.threadGroup = new ThreadGroup(commonNamingPrefix + " Thread Group");
		
		this.eventListener = spiCreateEventListener(config, commonNamingPrefix, threadGroup);
		
		this.mainQueueProcessingThread = createMainQueueProcessor(config, commonNamingPrefix, threadGroup);
		this.delayQueues = createDelayQueues(config, commonNamingPrefix, threadGroup);
		
		this.requestsExecutorService = spiCreateExecutorService(config, commonNamingPrefix, threadGroup);
		
		this.rateLimiter = spiCreateRateLimiter(config, commonNamingPrefix, threadGroup);
	}
	
	
	/**
	 * Overriding this method allows to change how cache processes time internally.
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
	 * zzz note that time stuff should be thread-safe
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
	 * <p>
	 * This never returns zero unless given virtual interval is zero.
	 */
	protected long timeRealWorldInterval(long virtualInterval)
	{
		long now = timeNow();
		long targetTime = timeAddVirtualIntervalToRealWorldTime(now, virtualInterval);
		
		return targetTime - now;
	}
	
	/**
	 * Decrements the 'currently processing' requests count, makes sure it's
	 * not negative.
	 * 
	 * @deprecated should not be used directly, instead use one of the handle*
	 * 		methods
	 */
	@Deprecated
	protected void decrementRequestsCount()
	{
		int now = processingRequestsCount.decrementAndGet();
		if (now < 0)
		{
			processingRequestsCount.set(0);
			logAssertionError(null, "Got into negative processing requests count, corrected to zero; was: " + now);
		}
	}
	
	/**
	 * Decrements the 'currently processing' requests count, makes sure it's
	 * not negative AND records 'total processing time' in the entry.
	 * 
	 * @return total processing time for the entry (in virtual ms)
	 * 
	 * @deprecated should not be used directly, instead use one of the handle*
	 * 		methods
	 */
	@Deprecated
	protected long decrementRequestsCountAndSetTotalProcessingTime(RRLEntry<Input, Output> entry)
	{
		decrementRequestsCount();
		
		final long totalRequestProcessingTime = timeGapVirtual(entry.getCreatedAt(), timeNow());
		
		entry.setTotalProcessingTime(totalRequestProcessingTime);
		
		return totalRequestProcessingTime;
	}
	
	/**
	 * Handles final failure of entry processing.
	 * <p>
	 * Takes care of updating request size counters, updates future, fires
	 * events.
	 */
	protected void handleFinalFailure(RRLEntry<Input, Output> entry, @Nullable Throwable t)
		throws InterruptedException
	{
		decrementRequestsCountAndSetTotalProcessingTime(entry); // item is removed from processing
		
		Throwable failCause = t == null ? 
			new RRLFinalFailureDecisionException(RRLMainQueueProcessingDecision.FINAL_FAILURE.toString() + " decision was made") 
			: t;
		
		entry.getFuture().completeExceptionally(failCause);
		
		guardedEventListenerInvocation(evListener -> evListener.requestFinalFailure(entry, t));
		guardedEventListenerInvocation(evListener -> evListener.requestRemoved(entry));
		
		guardedSpiInvocationNoResult(() -> afterRequestFinalFailure(entry, t), entry);
	}
	
	/**
	 * Handles timeout of entry processing.
	 * <p>
	 * Takes care of updating request size counters, updates future, fires
	 * events.
	 */
	protected void handleTimeout(RRLEntry<Input, Output> entry, long remainingValidityTime)
		throws InterruptedException
	{
		long totalProcessingTime = decrementRequestsCountAndSetTotalProcessingTime(entry); // item is removed from processing
		
		entry.getFuture().completeExceptionally(new RRLTimeoutException(totalProcessingTime));
		
		guardedEventListenerInvocation(evListener -> evListener.requestFinalTimeout(entry, remainingValidityTime));
		guardedEventListenerInvocation(evListener -> evListener.requestRemoved(entry));
		
		guardedSpiInvocationNoResult(() -> afterRequestFinalTimeout(entry, remainingValidityTime), entry);
	}
	
	/**
	 * Handles successful entry processing.
	 * <p>
	 * Takes care of updating request size counters, updates future, fires
	 * events.
	 */
	protected void handleSuccess(RRLEntry<Input, Output> entry, Output result, int attemptNumber, long requestDuration)
		throws InterruptedException
	{
		decrementRequestsCountAndSetTotalProcessingTime(entry); // item is removed from processing
		
		RRLCompletableFuture<Input, Output> future = entry.getFuture();
		future.setSuccessful(true);
		if (!future.complete(result))
			logAssertionError(entry, "Failed to transition Future to success.");
		
		guardedEventListenerInvocation(evListener -> evListener.requestSuccess(entry, result, attemptNumber, requestDuration));
		guardedEventListenerInvocation(evListener -> evListener.requestRemoved(entry));
		
		guardedSpiInvocationNoResult(() -> afterRequestSuccess(entry, result, attemptNumber, requestDuration), entry);
	}
	
	/**
	 * Logs assertion error in the code.
	 * @throws InterruptedException 
	 */
	// this can (and should be able to) throw InterruptedException, however
	// there are places where it's inconvenient and counter-productive to handle
	// those (such as SPI decision methods that should complete fast and therefore 
	// are not allowed to throw InterruptedExceptions (which typically mean waiting
	// on something)); thus use cheating and opt-out of requiring catching those
	@SneakyThrows(InterruptedException.class) 
	protected void logAssertionError(@Nullable RRLEntry<Input, Output> entry, String message)
	{
		//zzz add some counter/error tracking too?
		guardedEventListenerInvocation(evListener -> evListener.errorAssertionError(entry, message));
		
	}
	
	/**
	 * Logs spi method exception in the code.
	 * @throws InterruptedException 
	 */
	protected void logSpiMethodException(@Nullable RRLEntry<Input, Output> entry, Throwable t) throws InterruptedException
	{
		//zzz add some counter/error tracking too?
		guardedEventListenerInvocation(evListener -> evListener.errorSpiMethodException(entry, t));
	}
	
	/**
	 * Creates main queue processor thread.
	 */
	@SuppressWarnings("hiding")
	protected ExitableThread createMainQueueProcessor(RRLConfig config, String commonNamingPrefix, ThreadGroup threadGroup)
	{
		InterruptHandlingExitableThread thread = new InterruptHandlingExitableThread(
			threadGroup, commonNamingPrefix + " Main Queue Processor")
		{
			@Override
			protected void run1(boolean reentry)
				throws InterruptedException
			{
				runnableMainQueueProcessor();
			}

			@Override
			protected boolean handleUnexpectedInterruptedException(
				InterruptedException e) throws InterruptedException
			{
				guardedEventListenerInvocation(evListener -> 
					evListener.errorUnexpectedInterruptedException(e, "Unexpected InterruptedException in main queue processor"));
				
				// Decision via SPI method
				return guardedSpiInvocation(() -> spiMainQueueUnexpectedInterruptedExceptionDecision(
					e, getUnexpectedInterruptedExceptionsCount(), getRuntimeExceptionsCount()), 
					false/*exit in case of spi exception*/, null);
			}

			@Override
			protected boolean handleRuntimeException(RuntimeException e)
				throws InterruptedException
			{
				guardedEventListenerInvocation(evListener -> 
					evListener.errorUnexpectedRuntimeException(e, "RuntimeException in main queue processor"));
			
				// Decision via SPI method
				return guardedSpiInvocation(() -> spiMainQueueRuntimeExceptionDecision(
					e, getUnexpectedInterruptedExceptionsCount(), getRuntimeExceptionsCount()), 
					false/*exit in case of spi exception*/, null);
			}
		};
		
		thread.setPriority(config.getMainQueueProcessingThreadPriority());
		
		return thread;
	}
	


	//zzz comment
	protected static final Pair<RRLMainQueueProcessingDecision, Long> MQ_FINAL_FAILURE_DECISION =
		new Pair<>(RRLMainQueueProcessingDecision.FINAL_FAILURE, -1L);
	
	/**
	 * This instance is used by request processing threads to signal that they
	 * are ready for work.
	 */
	protected final RRLEntry<Input, Output> READY_TO_WORK_OBJECT = new RRLEntry<>(this, fakeNonNull(), -1, -1); 
	
	/**
	 * Code executed by {@link #mainQueueProcessingThread}
	 */
	protected void runnableMainQueueProcessor() throws InterruptedException
	{
		RRLEntry<Input, Output> inflightEntry = null;
		long inflightEntrySince = -1;
		/** 
		 * Used to store future representing thread that will be used to process request;
		 * must be cleared if thread is actually processing the request, otherwise
		 * it will be cancelled!
		 */
		Future<Void> readyForProcessingThreadFuture = null;
		/**
		 * Ticket that is ready for using for request;
		 * must be cleared if it is actually used for request, otherwise
		 * it will be returned!
		 */
		Object readyToUseTicket = null;
		try
		{
			
			mainLoop:
			while (true)
			{
				if (readyForProcessingThreadFuture != null)
				{
					// clean up thread that was obtained for processing 
					readyForProcessingThreadFuture.cancel(true);
					readyForProcessingThreadFuture = null;
				}
				if (inflightEntry != null)
				{
					if (readyToUseTicket != null)
					{
						// return ticket that was unused
						final RRLEntry<@Nonnull Input, Output> entry = inflightEntry;
						final Object ticket = readyToUseTicket;
						guardedSpiInvocationNoResult(() -> spiReturnUnusedTicket(entry, ticket), entry);
					}
					
					// Log item processing duration if needed
					final RRLEntry<@Nonnull Input, Output> entry = inflightEntry;
					final long since = inflightEntrySince;
					final long duration = timeGapVirtual(since, timeNow());
					guardedEventListenerInvocation(evListener -> 
						evListener.mainQueueProcessingCompleted(entry, since, duration));
				}
				
				inflightEntry = null; // if we are here, then previous in-flight entry has been processed
				inflightEntry = mainQueue.take(); // in-flight entry externally available in exception handling etc
				final long itemProcessingSince = timeNow();
				final RRLEntry<@Nonnull Input, Output> entry = inflightEntry;
				inflightEntrySince = itemProcessingSince;
				
				final SynchronousQueue<RRLEntry<Input, Output>> commQueue = new SynchronousQueue<>();
				while (true) // the code is iterated until all required resources are obtained
				{
					NonNullOptional<@Nonnull Pair<RRLMainQueueProcessingDecision, Long>> decisionOptional = 
						guardedSpiInvocationAsOptional(() -> spiMainQueueProcessingDecision(entry, false, false), entry);
					
					Pair<RRLMainQueueProcessingDecision, Long> decision = 
						decisionOptional.getOrElse(MQ_FINAL_FAILURE_DECISION);
					long millisFromDecision = decision.getValue1(); 
					Throwable t = decisionOptional.getExceptionOrNull();
					
					guardedEventListenerInvocation(evListener -> evListener.mainQueueProcessingDecision(entry, decision, itemProcessingSince));
					
					switch (decision.getValue0())
					{
						case FINAL_FAILURE:
							handleFinalFailure(entry, t);
							continue mainLoop; // go to next element
							
						case TIMEOUT:
							handleTimeout(entry, millisFromDecision);
							continue mainLoop; // go to next element
							
						case DELAY:
							delayEntry(entry, millisFromDecision);
							continue mainLoop; // go to next element
							
						case PROCEED:
							break; // actual processing is below this switch
					}
					
					long remainingValidityRealMs;
					if (millisFromDecision <= 0)
					{
						logAssertionError(entry, "Non-positive remaining validity time received from non-timeout main queue decision: " + millisFromDecision);
						remainingValidityRealMs = 1;
					}
					else
						remainingValidityRealMs = timeRealWorldInterval(millisFromDecision) + 1; // +1 to hopefully avoid some boundary issues
					
					
					// Need to ensure we have all the appropriate resources for request
					if (readyForProcessingThreadFuture == null)
					{
						final long before = timeNow();
						
						NonNullOptional<@Nonnull Future<Void>> result = guardedSpiInvocationAsOptional(
							() -> spiStartRequestProcessingThread(entry,
								() -> runnableRequestProcessor(commQueue)), 
							entry);
						
						if (result.isEmpty())
						{
							// must be an error -- and it should've been logged already in guarded* method
							mainQueue.add(entry); // try again later
							continue mainLoop; // go to next element
						}
						
						readyForProcessingThreadFuture = result.get();
						
						// Wait for thread to be ready.
						//zzz use maxsleeptime
						RRLEntry<@Nonnull Input, Output> ready = 
							commQueue.poll(remainingValidityRealMs, TimeUnit.MILLISECONDS);
						
						if (ready == null)
						{
							// Thread wait expired, clear it, will need to try again.
							readyForProcessingThreadFuture.cancel(true);
							readyForProcessingThreadFuture = null;
						}
						else
						{
							if (ready != READY_TO_WORK_OBJECT)
							{
								logAssertionError(entry, "Object received from processing thread isn't READY_TO_WORK_OBJECT: " + ready);
								
								mainQueue.add(entry); // try again later
								continue mainLoop; // go to next element; thread future will be cancelled in the loop's beginning
							}
						}
						
						// Thread attempt is done.
						final long after = timeNow();
						final long duration = timeGapVirtual(before, after);
						
						boolean threadObtained = (readyForProcessingThreadFuture != null);
						guardedEventListenerInvocation(evListener -> 
							evListener.mainQueueThreadObtainAttempt(entry, itemProcessingSince, threadObtained, duration));
						
					} 
					else if (readyToUseTicket == null)
					{
						final long before = timeNow();
						
						//zzz use maxsleeptime
						NullableOptional<@Nullable Object> result = guardedSpiInvocationAsNullableOptional(
							() -> spiObtainTicket(entry, remainingValidityRealMs), entry);
						
						if (result.isEmpty())
						{
							// must be an error -- and it should've been logged already in guarded* method
							mainQueue.add(entry); // try again later
							continue mainLoop; // go to next element
						}
						
						readyToUseTicket = result.get(); // can still result in null
						// if ticket is still null (wasn't obtained), we still need
						// to loop to make a new decision (same as if it was obtained)
						
						final long after = timeNow();
						final long duration = timeGapVirtual(before, after);
						
						boolean ticketObtained = (readyToUseTicket != null);
						guardedEventListenerInvocation(evListener -> 
							evListener.mainQueueTicketObtainAttempt(entry, itemProcessingSince, ticketObtained, duration));
					}
					else
					{
						// Here we have both thread and a ticket -- should proceed with request
						boolean submitted = commQueue.offer(entry, 
							config.getMainQueueMaxRequestHandoverWaitTime(), /*not virtualizing these ms as it is not expected to happen anyway*/ 
							TimeUnit.MILLISECONDS);
						
						if (!submitted)
						{
							// something is really wrong
							logAssertionError(entry, "Failed to hand over request to processing thread, max wait time is: " + config.getMainQueueMaxRequestHandoverWaitTime());
							mainQueue.add(entry); // try again later
							continue mainLoop; // go to next element
						}
						
						readyForProcessingThreadFuture = null; // clear thread reference so it is not cancelled
						readyToUseTicket = null; // clear ticket reference so it is not returned
						
						guardedEventListenerInvocation(evListener -> 
							evListener.requestExecuting(entry, entry.getNumberOfFailedAttempts() + 1, millisFromDecision));
						
						continue mainLoop; // go to next element in the queue 
					}
				} // end resource collection loop
			} // end main loop
		} finally
		{
			if (inflightEntry != null)
			{
				// Put entry back into main queue to avoid data loss.
				mainQueue.add(inflightEntry);
				
				if (readyToUseTicket != null)
				{
					// return ticket that was unused
					final RRLEntry<@Nonnull Input, Output> entry = inflightEntry;
					final Object ticket = readyToUseTicket;
					guardedSpiInvocationNoResult(() -> spiReturnUnusedTicket(entry, ticket), entry);
				}
			}
			
			if (readyForProcessingThreadFuture != null)
			{
				// clean up thread that was obtained for processing 
				readyForProcessingThreadFuture.cancel(true);
			}
			
		}
		
	}
	
	
	/**
	 * Creates delay queue processors.
	 * <p>
	 * Note that custom implementations may decide to override this and they
	 * are allowed to return empty list if they need special handling; however
	 * they then MUST properly override {@link #delayEntry(RRLEntry, long)}
	 * method to be compatible!
	 */
	@SuppressWarnings("hiding")
	protected List<RRLDelayQueueData> createDelayQueues(RRLConfig config, String commonNamingPrefix, ThreadGroup threadGroup)
	{
		List<@Nonnull Long> delaysList = config.getDelayQueues();
		
		ArrayList<RRLDelayQueueData> queues = new ArrayList<>(delaysList.size());
		long maxDelay = -1;
		for (long delay : delaysList)
		{
			if (delay <= maxDelay)
				throw new IllegalArgumentException("delayQueues are not specified in ascending order: " + delaysList);
			
			LinkedBlockingQueue<RRLEntry<Input, Output>> delayQueue = new LinkedBlockingQueue<>();
			
			ExitableThread thread = createDelayQueueProcessor(config, commonNamingPrefix, threadGroup, delay, delayQueue);
			
			queues.add(new RRLDelayQueueData(delay, thread, delayQueue));
		}
		
		return queues;
	}
	
	
	/**
	 * Creates delay queue processor thread.
	 */
	@SuppressWarnings("hiding")
	protected ExitableThread createDelayQueueProcessor(
		RRLConfig config, String commonNamingPrefix, ThreadGroup threadGroup,
		final long queueDelayMs, final LinkedBlockingQueue<RRLEntry<Input, Output>> delayQueue)
	{
		InterruptHandlingExitableThread thread = new InterruptHandlingExitableThread(
			threadGroup, commonNamingPrefix + " Delay Queue Processor (" + queueDelayMs + " ms)")
		{
			@Override
			protected void run1(boolean reentry)
				throws InterruptedException
			{
				runnableDelayQueueProcessor(queueDelayMs, delayQueue);
			}

			@Override
			protected boolean handleUnexpectedInterruptedException(
				InterruptedException e) throws InterruptedException
			{
				guardedEventListenerInvocation(evListener -> 
					evListener.errorUnexpectedInterruptedException(e, "Unexpected InterruptedException in delay queue processor (" + queueDelayMs + " ms)"));
				
				// Decision via SPI method
				return guardedSpiInvocation(() -> spiDelayQueueUnexpectedInterruptedExceptionDecision(
					queueDelayMs, delayQueue, e, 
					getUnexpectedInterruptedExceptionsCount(), getRuntimeExceptionsCount()), 
					false/*exit in case of spi exception*/, null);
			}

			@Override
			protected boolean handleRuntimeException(RuntimeException e)
				throws InterruptedException
			{
				guardedEventListenerInvocation(evListener -> 
					evListener.errorUnexpectedRuntimeException(e, "RuntimeException in delay queue processor (" + queueDelayMs + " ms)"));
			
				// Decision via SPI method
				return guardedSpiInvocation(() -> spiDelayQueueRuntimeExceptionDecision(
					queueDelayMs, delayQueue, e, 
					getUnexpectedInterruptedExceptionsCount(), getRuntimeExceptionsCount()), 
					false/*exit in case of spi exception*/, null);
			}
		};
		
		thread.setPriority(config.getDelayQueueProcessingThreadPriority());
		
		return thread;
	}
	

	/**
	 * Code executed by {@link #delayQueueProcessingThread}
	 */
	protected void runnableDelayQueueProcessor(
		final long queueDelayMs, final LinkedBlockingQueue<RRLEntry<Input, Output>> delayQueue) 
			throws InterruptedException
	{
		RRLEntry<Input, Output> inflightEntry = null;
		try
		{
			inflightEntry = delayQueue.take(); // in-flight entry externally available in exception handling etc 
			final RRLEntry<@Nonnull Input, Output> entry = inflightEntry;
			
			long now = timeNow();
			
			long inDelayQueueSince = entry.getInDelayQueueSince();
			long delayAnchor = entry.getDelayAnchor();
			long delayFor = entry.getDelayFor();
			
			// zzz make use of flush state too?
			// Using counter here because variable needs to be final for use in closures. 
			final SimpleLongCounter remainingDelay = new SimpleLongCounter(-1);
			if (inDelayQueueSince < 0)
				logAssertionError(entry, "Delay queue (" + queueDelayMs + " ms) processing encountered item with non-specified inDelayQueueSince.");
			else if ((delayAnchor < 0) || (delayFor < 0))
				logAssertionError(entry, "Delay queue (" + queueDelayMs + " ms) processing encountered item with non-specified delay.");
			else
			{
				long timePassed = timeGapVirtual(delayAnchor, now);
				remainingDelay.set(delayFor - timePassed);
			}
			
			// log event
			guardedEventListenerInvocation(evListener -> 
				evListener.delayQueueItemBeforeDelayStep(entry, queueDelayMs, remainingDelay.get()));
			
			final long sleptFor;
			{
				long toSleep = 0;
				if (remainingDelay.get() > 0)
				{
					// Can't sleep longer that this because other items in queue may exceed their delay by then
					long allowedDelay = queueDelayMs - timeGapVirtual(inDelayQueueSince, now);
					if (allowedDelay > 0)
					{
						toSleep = Math.min(remainingDelay.get(), allowedDelay);
						long realSleep = timeRealWorldInterval(toSleep);
						Thread.sleep(realSleep); // zzz needs some kind of maxsleep
					}
				}
				
				sleptFor = toSleep;
			}
			remainingDelay.decrementByAndGet(sleptFor); 
			
			RRLDelayQueueProcessingDecision decision;
			decision = guardedSpiInvocation(
				() -> spiDelayQueueAfterDelayStepDecision(queueDelayMs, entry, remainingDelay.get()), 
				RRLDelayQueueProcessingDecision.MAIN_QUEUE, entry);

			// log decision event
			guardedEventListenerInvocation(evListener -> 
				evListener.delayQueueDecisionAfterDelayStep(entry, queueDelayMs, decision, sleptFor, remainingDelay.get()));
			
			switch(decision)
			{
				case DELAY_AGAIN:
					entry.setInDelayQueueSince(timeNow());
					delayQueue.add(entry);
					break;
				case MAIN_QUEUE:
					mainQueue.add(entry);
					break;
			}
			inflightEntry = null; // clear 'in-flight' entry RIGHT AFTER it is re-queued! IMPORTANT FOR CONSISTENCY!
			
		} finally
		{
			if (inflightEntry != null)
			{
				// Put entry back into main queue to avoid data loss.
				mainQueue.add(inflightEntry);
			}
		}
	}
	
	/**
	 * Delays given entry via putting it into appropriate delay queue; note
	 * that the minimal resolution here is the delay of the shortest delay 
	 * queue.
	 */
	protected void delayEntry(RRLEntry<Input, Output> entry, long delayFor)
	{
		entry.setDelayAnchor(timeNow());
		entry.setDelayFor(delayFor);
		
		RRLDelayQueueData queue = delayQueues.get(0); // always has at least one item
		for (RRLDelayQueueData dq : delayQueues)
		{
			if (dq.getDelayMs() > delayFor)
				break; // this queue is too long, use previous one
			
			queue = dq; // this queue is a candidate
		}
		
		queue.addToQueue(entry);
	}
	
	//zzz comments
	protected Void runnableRequestProcessor(SynchronousQueue<RRLEntry<Input, Output>> commQueue)
		throws InterruptedException
	{
		commQueue.put(READY_TO_WORK_OBJECT);

		boolean finished = false;
		
		// Retrieve entry-to-process outside try-catch as this place can be
		// interrupted and it is not really 'unexpected'
		RRLEntry<Input, Output> inflightEntry = commQueue.take();
		
		try
		{
			final RRLEntry<@Nonnull Input, Output> entry = inflightEntry;
			final long start = timeNow();
			final int attemptNumber = entry.getNumberOfFailedAttempts() + 1;
			
			Exception exception; // used to indicate whether request completed correctly
			final Output result;
			{
				Output tmpResult = fakeNonNull(); // to make compiler happy, this value is overwritten by actual value or is never used
				Exception tmpException = null;
				try
				{
					tmpResult = spiProcessRequest(entry, attemptNumber);
				} catch (Exception e)
				{
					tmpException = e;
				}
				result = tmpResult;
				exception = tmpException;
			}
			
			final long requestDuration = timeGapVirtual(start, timeNow());
			
			if (exception == null)
			{
				inflightEntry = null; // request was successful, make sure item is not re-added
				handleSuccess(entry, result, attemptNumber, requestDuration);
			}
			else
			{
				entry.setNumberOfFailedAttempts(attemptNumber);
				
				guardedEventListenerInvocation(evListener -> evListener.requestAttemptFailed(entry, exception, attemptNumber, requestDuration));
				guardedSpiInvocationNoResult(() -> afterRequestAttemptFailed(entry, exception, attemptNumber, requestDuration), entry);
				
				NonNullOptional<@Nonnull Pair<RRLAfterRequestAttemptFailedDecision, Long>> decisionOptional = guardedSpiInvocationAsOptional( 
					() -> spiAfterRequestAttemptFailedDecision(entry, exception, attemptNumber, requestDuration), entry);
				
				
				Pair<RRLAfterRequestAttemptFailedDecision, Long> decision = 
					decisionOptional.getOrElse(AR_FINAL_FAILURE_DECISION);
				long millisFromDecision = decision.getValue1(); 
				Throwable t = decisionOptional.getExceptionOrNull();
				
				guardedEventListenerInvocation(evListener -> evListener.requestAttemptFailedDecision(entry, decision));
				
				switch (decision.getValue0())
				{
					case FINAL_FAILURE:
						inflightEntry = null; // not going to retry this
						handleFinalFailure(entry, t);
						break;
						
					case TIMEOUT:
						inflightEntry = null; // not going to retry this
						handleTimeout(entry, millisFromDecision);
						break;
						
					case RETRY:
						entry.setEarliestProcessingTimeDelay(timeNow(), millisFromDecision);
						
						//DO NOT clear inflightEntry so that it will get re-added to main queue in finally block
						
						break;
				}
			}
			
			finished = true;
			
			return fakeNonNull(); // Void cannot be instantiated
		} catch (InterruptedException e)
		{
			finished = true; // let's not log interrupts as assertion errors

			// but we do not really expect these to happen here, so log it
			guardedEventListenerInvocation(evListener -> evListener.errorUnexpectedInterruptedException(e, "Unexpected interrupt in request processing thread"));
			
			throw e;
		} finally
		{
			if (inflightEntry != null)
				mainQueue.add(inflightEntry); // re-add item unless it was cleared
			
			if (!finished)
				logAssertionError(inflightEntry, "Request processing thread didn't finish correctly!");
		}
	}

	/**
	 * Invokes given SPI code and returns the result.
	 * <p>
	 * If underlying SPI code throws an exception, then the exception is logged
	 * via {@link #logSpiMethodException(RRLEntry, Throwable)} and
	 * valueInCaseOfException is returned.
	 * <p>
	 * NOTE: {@link InterruptedException} and {@link ThreadDeath} subclasses
	 * are not logged and are re-thrown as those can be used to stop thread
	 */
	protected <@Nonnull RV> RV guardedSpiInvocation(InterruptableSupplier<RV> callable,
		RV valueInCaseOfException, @Nullable RRLEntry<Input, Output> entryForLoggingInCaseOfException) 
			throws InterruptedException
	{
		NonNullOptional<@Nonnull RV> result = guardedSpiInvocationAsOptional(callable, entryForLoggingInCaseOfException);
		
		if (result.isEmpty())
			return valueInCaseOfException;
		
		return result.get();
	}

	/** Used in {@link #guardedSpiInvocationNoResult(InterruptableRunnable, RRLEntry)} */
	private static final Object someObject = new Object();
	
	/**
	 * Invokes given SPI code that doesn't produce any result.
	 * <p>
	 * If underlying SPI code throws an exception, then the exception is logged
	 * via {@link #logSpiMethodException(RRLEntry, Throwable)}.
	 * <p>
	 * NOTE: {@link InterruptedException} and {@link ThreadDeath} subclasses
	 * are not logged and are re-thrown as those can be used to stop thread
	 */
	protected void guardedSpiInvocationNoResult(InterruptableRunnable runnable,
		@Nullable RRLEntry<Input, Output> entryForLoggingInCaseOfException) 
			throws InterruptedException
	{
		guardedSpiInvocationAsOptional(() -> {runnable.run(); return someObject;}, entryForLoggingInCaseOfException);
	}

	/**
	 * Invokes given SPI code and returns the result.
	 * <p>
	 * If underlying SPI code throws an exception, then the exception is logged
	 * via {@link #logSpiMethodException(RRLEntry, Throwable)} and
	 * valueInCaseOfException is returned.
	 * <p>
	 * NOTE: {@link InterruptedException} and {@link ThreadDeath} subclasses
	 * are not logged and are re-thrown as those can be used to stop thread
	 */
	protected <@Nonnull RV> NonNullOptional<RV> guardedSpiInvocationAsOptional(InterruptableSupplier<RV> callable,
		@Nullable RRLEntry<Input, Output> entryForLoggingInCaseOfException) 
			throws InterruptedException
	{
		try
		{
			@SuppressWarnings("null") NonNullOptional<@Nonnull RV> result = NonNullOptional.fromNullableOptionalIfNonNull(
				guardedSpiInvocationAsNullableOptional(callable, entryForLoggingInCaseOfException)); 
			
			return result;
		} catch (IllegalArgumentException e)
		{
			// in case of null return value
			return NonNullOptional.emptyWithException(e);
		}
	}


	/**
	 * Invokes given SPI code and returns the result.
	 * <p>
	 * If underlying SPI code throws an exception, then the exception is logged
	 * via {@link #logSpiMethodException(RRLEntry, Throwable)} and
	 * valueInCaseOfException is returned.
	 * <p>
	 * NOTE: {@link InterruptedException} and {@link ThreadDeath} subclasses
	 * are not logged and are re-thrown as those can be used to stop thread
	 */
	protected <@Nullable RV> NullableOptional<RV> guardedSpiInvocationAsNullableOptional(InterruptableSupplier<RV> callable,
		@Nullable RRLEntry<Input, Output> entryForLoggingInCaseOfException) 
			throws InterruptedException
	{
		try
		{
			return NullableOptional.of(callable.get());
		} catch (Throwable e)
		{
			if (e instanceof ThreadDeath)
				throw e;
			if (e instanceof InterruptedException) // this may be used to indicate that thread should exit 
				throw e;
			
			logSpiMethodException(entryForLoggingInCaseOfException, e);
			
			return NullableOptional.emptyWithException(e);
		}
	}

	/**
	 * Sneaky throws InterruptedException (instead of normally as checked); to
	 * be used with care.
	 * <p>
	 * Invokes given SPI code and returns the result.
	 * <p>
	 * If underlying SPI code throws an exception, then the exception is logged
	 * via {@link #logSpiMethodException(RRLEntry, Throwable)} and
	 * valueInCaseOfException is returned.
	 * <p>
	 * NOTE: {@link InterruptedException} and {@link ThreadDeath} subclasses
	 * are not logged and are re-thrown as those can be used to stop thread
	 */
	@SneakyThrows(InterruptedException.class)
	protected void sneakyGuardedEventListenerInvocation(InterruptableConsumer<RRLEventListener<Input, Output>> evListener) 
	{
		guardedEventListenerInvocation(evListener);
	}
	
	
	/**
	 * Invokes given SPI code and returns the result.
	 * <p>
	 * If underlying SPI code throws an exception, then the exception is logged
	 * via {@link #logSpiMethodException(RRLEntry, Throwable)} and
	 * valueInCaseOfException is returned.
	 * <p>
	 * NOTE: {@link InterruptedException} and {@link ThreadDeath} subclasses
	 * are not logged and are re-thrown as those can be used to stop thread
	 */
	protected void guardedEventListenerInvocation(InterruptableConsumer<RRLEventListener<Input, Output>> evListener) 
			throws InterruptedException
	{
		try
		{
			evListener.accept(eventListener);
		} catch (Throwable e)
		{
			if (e instanceof ThreadDeath)
				throw e;
			if (e instanceof InterruptedException) // this may be used to indicate that thread should exit 
				throw e;
			
			try
			{
				eventListener.errorEventListenerMethodException(e);
			} catch (Throwable e2)
			{
				if (e2 instanceof ThreadDeath)
					throw e2;
				if (e2 instanceof InterruptedException) // this may be used to indicate that thread should exit 
					throw e2;
			}
		}
	}
	
	/**
	 * Creates thread pool / executor for processing requests.
	 * <p>
	 * Custom implementations are allowed to return null value in case they
	 * don't use a dedicated thread pool / executor -- but in that case they
	 * MUST properly override {@link #spiStartRequestProcessingThread(RRLEntry, Callable)}
	 * method! zzz also mention about status? probably status should be spi* method too
	 * <p> 
	 * Default implementation creates {@link WAThreadPoolExecutor} with
	 * min/max sizes from {@link RRLConfig#requestProcessingThreadPoolConfig()}
	 * and using daemon threads with priority
	 * {@link RRLConfig#getRequestProcessingThreadPriority()}
	 */
	@SuppressWarnings("hiding")
	@Nullable
	protected ExecutorService spiCreateExecutorService(RRLConfig config, String commonNamingPrefix, ThreadGroup threadGroup)
	{
		List<@Nonnull Integer> sizeCfg = config.getRequestProcessingThreadPoolConfig();
		if (sizeCfg.size() != 2)
			throw new IllegalStateException("requestProcessingThreadPoolConfig must specify two values, got: " + sizeCfg);
		
		int minSize = sizeCfg.get(0);
		int maxSize = sizeCfg.get(1);
		if (maxSize < minSize)
			throw new IllegalStateException("In requestProcessingThreadPoolConfig max size (2nd arg) must not be less than min size (1st arg), got: " + sizeCfg);
		if (maxSize < 1)
			throw new IllegalStateException("In requestProcessingThreadPoolConfig max size (2nd arg) must be at least 1, got: " + sizeCfg);
		
		return new WAThreadPoolExecutor(minSize, maxSize, 
			commonNamingPrefix + " Requests Executor",
			true/*is daemon*/, 
			config.getRequestProcessingThreadPriority(), 
			threadGroup);
	}
	
	
	/**
	 * Creates rate limiter for this instance.
	 * <p>
	 * Default implementation uses {@link RRLBucket4jFlatRateLimiter} with
	 * configuration options from {@link RRLConfig};
	 * NOTE: will return {@link RRLUnlimitedRateLimiter} if bucket size is 0
	 */
	@SuppressWarnings({"hiding", "unused"})
	protected RRLRateLimiter<?> spiCreateRateLimiter(RRLConfig config, String commonNamingPrefix, ThreadGroup threadGroup)
	{
		int bucketSize = config.getRateLimiterBucketSize();
		if (bucketSize == 0)
			return new RRLUnlimitedRateLimiter();
		
		return new RRLBucket4jFlatRateLimiter(
			bucketSize, 
			config.getRateLimiterRefillRate(), 
			config.getRateLimiterRefillInterval(), 
			0 /*initial tokens*/);
	}
	
	/**
	 * Creates event listener for this instance.
	 * <p>
	 * Default implementation zzz
	 */
	@SuppressWarnings({"hiding", "unused"})
	protected RRLEventListener<Input, Output> spiCreateEventListener(RRLConfig config, String commonNamingPrefix, ThreadGroup threadGroup)
	{
		return new DefaultRRLEventListener<>(config.getServiceName());
	}
	
	/**
	 * Makes decision for the delay queue entry processing AFTER single delay
	 * step (i.e. delay no longer than queueDelayMs) has been performed.
	 * <p>
	 * Default implementation returns {@link RRLDelayQueueProcessingDecision#DELAY_AGAIN}
	 * only if remaining time exceeds queueDelayMs.
	 * <p>
	 * Custom implementations may consider grace periods for improving performance.
	 */
	protected RRLDelayQueueProcessingDecision spiDelayQueueAfterDelayStepDecision(
		long queueDelayMs, @SuppressWarnings("unused") RRLEntry<Input, Output> entry, long remainingDelay)
	{
		if (remainingDelay < queueDelayMs)
			return RRLDelayQueueProcessingDecision.MAIN_QUEUE;
		else
			return RRLDelayQueueProcessingDecision.DELAY_AGAIN;
	}
	
	
	/**
	 * Makes decision in case unexpected {@link InterruptedException} happens
	 * in any of the delay queue processing threads.
	 * <p>
	 * Default implementation returns true (continue/restart queue processing)
	 * until number of exceptions exceeds {@link RRLConfig#getDelayQueueUnexpectedInterruptedExceptionLimit()}
	 * <p>
	 * Throwing exception in this method will abort queue processing (same-ish
	 * as returning false), thus making service inoperable.
	 * 
	 * @return true if delay queue processing should restart/continue; false
	 * 		to abort thread (thus rendering service inoperable)
	 */
	@SuppressWarnings("unused")
	protected boolean spiDelayQueueUnexpectedInterruptedExceptionDecision(
		final long queueDelayMs, final LinkedBlockingQueue<RRLEntry<Input, Output>> delayQueue,
		InterruptedException e, 
		int unexpectedInterruptedExceptionsCount, int runtimeExceptionsCount)
			throws InterruptedException
	{
		return unexpectedInterruptedExceptionsCount <= config.getDelayQueueUnexpectedInterruptedExceptionLimit();
	}
	
	/**
	 * Makes decision in case unhandled {@link RuntimeException} happens
	 * in any of the delay queue processing threads.
	 * <p>
	 * Default implementation returns true (continue/restart queue processing)
	 * until number of exceptions exceeds {@link RRLConfig#getDelayQueueRuntimeExceptionLimit()}
	 * <p>
	 * Throwing exception in this method will abort queue processing (same-ish
	 * as returning false), thus making service inoperable.
	 * 
	 * @return true if delay queue processing should restart/continue; false
	 * 		to abort thread (thus rendering service inoperable)
	 */
	@SuppressWarnings("unused")
	protected boolean spiDelayQueueRuntimeExceptionDecision(
		final long queueDelayMs, final LinkedBlockingQueue<RRLEntry<Input, Output>> delayQueue,
		RuntimeException e, 
		int unexpectedInterruptedExceptionsCount, int runtimeExceptionsCount)
			throws InterruptedException
	{
		return runtimeExceptionsCount <= config.getDelayQueueRuntimeExceptionLimit();
	}
	
	
	/**
	 * Makes decision in case unexpected {@link InterruptedException} happens
	 * in main queue processing thread.
	 * <p>
	 * Default implementation returns true (continue/restart queue processing)
	 * until number of exceptions exceeds {@link RRLConfig#getMainQueueUnexpectedInterruptedExceptionLimit()}
	 * <p>
	 * Throwing exception in this method will abort queue processing (same-ish
	 * as returning false), thus making service inoperable.
	 * 
	 * @return true if main queue processing should restart/continue; false
	 * 		to abort thread (thus rendering service inoperable)
	 */
	@SuppressWarnings("unused")
	protected boolean spiMainQueueUnexpectedInterruptedExceptionDecision(
		InterruptedException e, 
		int unexpectedInterruptedExceptionsCount, int runtimeExceptionsCount)
			throws InterruptedException
	{
		return unexpectedInterruptedExceptionsCount <= config.getMainQueueUnexpectedInterruptedExceptionLimit();
	}
	
	/**
	 * Makes decision in case unhandled {@link RuntimeException} happens
	 * in main queue processing thread.
	 * <p>
	 * Default implementation returns true (continue/restart queue processing)
	 * until number of exceptions exceeds {@link RRLConfig#getMainQueueRuntimeExceptionLimit()}
	 * <p>
	 * Throwing exception in this method will abort queue processing (same-ish
	 * as returning false), thus making service inoperable.
	 * 
	 * @return true if main queue processing should restart/continue; false
	 * 		to abort thread (thus rendering service inoperable)
	 */
	@SuppressWarnings("unused")
	protected boolean spiMainQueueRuntimeExceptionDecision(
		RuntimeException e, 
		int unexpectedInterruptedExceptionsCount, int runtimeExceptionsCount)
			throws InterruptedException
	{
		return runtimeExceptionsCount <= config.getMainQueueRuntimeExceptionLimit();
	}
	
	/**
	 * Makes decision for an item being processed from the main queue -- multiple
	 * invocations of this method happen for each 'processing cycle' as there
	 * are multiple delays involved in processing and time passing can have
	 * an impact on the decision.
	 * 
	 * @param hasThread whether processing has already obtained/reserved the
	 * 		thread required for processing the request; this is done 
	 * 		before obtaining ticket
	 * @param hasTicket whether processing has already obtained ticket required
	 * 		for request processing (required to implement requests rate limiting);
	 *		this is done after thread has been already obtained so there's no
	 *		additional waiting after ticket is obtained
	 *
	 * @return decision and second milliseconds argument; the second argument
	 * 		indicates (virtual) time allotted -- for delay it is the length
	 * 		of delay to be made; for timeout & proceed it indicates the remaining
	 * 		validity time for the request (can be negative in case of timeout)
	 */
	protected Pair<RRLMainQueueProcessingDecision, Long> spiMainQueueProcessingDecision(
		final RRLEntry<Input, Output> entry, boolean hasThread, boolean hasTicket
		)
	{
		long remainingValidityTime = spiMainQueueCalculateRemainingValidityTime(entry, hasThread, hasTicket);
		if (remainingValidityTime <= 0)
			return new Pair<>(RRLMainQueueProcessingDecision.TIMEOUT, remainingValidityTime);
		
		long remainingDelay = spiMainQueueCalculateRemainingDelay(entry, hasThread, hasTicket);
		if (remainingDelay > 0)
			return new Pair<>(RRLMainQueueProcessingDecision.DELAY, remainingDelay);
		else
		{
			// clear earliest processing time if we are 'past it'
			entry.setEarliestProcessingTimeDelay(-1, -1);
		}
		
		// May proceed with the given time limit
		return new Pair<>(RRLMainQueueProcessingDecision.PROCEED, remainingValidityTime);
	}
	
	
	/**
	 * Calculates remaining request validity time for the given entry; if
	 * remaining request validity time is 0 or less, then requests time-outs;
	 * otherwise it may be processed further.
	 */
	protected long spiMainQueueCalculateRemainingValidityTime(
		final RRLEntry<Input, Output> entry, @SuppressWarnings("unused") boolean hasThread, @SuppressWarnings("unused") boolean hasTicket)
	{
		long timePassed = timeGapVirtual(entry.getCreatedAt(), timeNow());
		long timeRemaining = entry.getRequestValidityDuration() - timePassed;
		
		return timeRemaining;
	}
	
	/**
	 * Calculates remaining delay (time before the request can be processed)
	 * for the given entry; if the returned value is 0 or less, then request
	 * can be processed without delay.
	 * <p>
	 * NOTE: this implementation ignores (sets to zero) delays less than
	 * {@link RRLConfig#getRequestEarlyProcessingGracePeriod()}
	 * 
	 * @see #spiCalculateRemainingDelayExact(RRLEntry, boolean, boolean)
	 */
	protected long spiMainQueueCalculateRemainingDelay(
		final RRLEntry<Input, Output> entry, boolean hasThread, boolean hasTicket)
	{
		long timeDelay = spiCalculateRemainingDelayExact(entry, hasThread, hasTicket);
		
		if (timeDelay <= config.getRequestEarlyProcessingGracePeriod())
			timeDelay = 0;
		
		return timeDelay;
	}
	
	//zzz comment
	protected static final Pair<RRLAfterRequestAttemptFailedDecision, Long> AR_FINAL_FAILURE_DECISION =
		new Pair<>(RRLAfterRequestAttemptFailedDecision.FINAL_FAILURE, -1L);
	
	/**
	 * Makes decision for an item after request attempt has failed (with an
	 * exception).
	 * 
	 * zzz clarify meaning of second return arg
	 */
	protected Pair<RRLAfterRequestAttemptFailedDecision, Long> spiAfterRequestAttemptFailedDecision(
		final RRLEntry<Input, Output> entry, Exception exception, 
		int attemptNumber, long requestDuration)
	{
		long remainingAttempts = spiCalculateRemainingAttempts(entry, attemptNumber);
		if (remainingAttempts <= 0)
			return AR_FINAL_FAILURE_DECISION;
		
		long remainingValidityTime = spiMainQueueCalculateRemainingValidityTime(entry, false, false);
		if (remainingValidityTime <= 0)
			return new Pair<>(RRLAfterRequestAttemptFailedDecision.TIMEOUT, remainingValidityTime);
		
		// May retry
		return new Pair<>(RRLAfterRequestAttemptFailedDecision.RETRY, 
			spiCalculateDelayAfterFailedRequestAttempt(entry, exception, attemptNumber, requestDuration));
	}
	
	/**
	 * Calculates number of remaining attempts for the given item (this happens
	 * after request attempt has failed).
	 * <p>
	 * Default implementation simply subtracts used attempts from {@link RRLConfig#getMaxAttempts()}
	 * 
	 * @return number of remaining attempts, 0 or negative number means that
	 * 		no attempts are remaining (request should 'final fail')
	 */
	protected int spiCalculateRemainingAttempts(
		@SuppressWarnings("unused") final RRLEntry<Input, Output> entry, int usedAttempts)
	{
		return config.getMaxAttempts() - usedAttempts;
	}
	
	/**
	 * Calculates a delay (in virtual ms) required after failed request attempt
	 * before it is allowed to try again.
	 * 
	 * @return delay (in virtual ms); 0 or negative value means no delay is
	 * 		required
	 */
	protected long spiCalculateDelayAfterFailedRequestAttempt(
		@SuppressWarnings("unused") final RRLEntry<Input, Output> entry, @SuppressWarnings("unused") Exception exception, 
		int failedAttemptNumber, @SuppressWarnings("unused") long requestDuration)
	{
		List<@Nonnull Long> delaysList = config.getDelaysAfterFailure();
		
		int index = Math.min(failedAttemptNumber, delaysList.size()) - 1; //zzz test this
		
		return delaysList.get(index);
	}
	
	/**
	 * Calculates EXACT remaining delay (time before the request can be processed)
	 * for the given entry; if the returned value is 0 or less, then request
	 * can be processed without delay.
	 * <p>
	 * NOTE: there's grace support on top of this, see {@link #spiMainQueueCalculateRemainingDelay(RRLEntry, boolean, boolean)}
	 * 
	 * @see #spiMainQueueCalculateRemainingDelay(RRLEntry, boolean, boolean)
	 */
	protected long spiCalculateRemainingDelayExact(
		final RRLEntry<Input, Output> entry, @SuppressWarnings("unused") boolean hasThread, @SuppressWarnings("unused") boolean hasTicket)
	{
		long earliestProcessingTimeAnchor = entry.getEarliestProcessingTimeAnchor();
		if (earliestProcessingTimeAnchor < 0)
			return 0; // no delay required
		
		long earliestProcessingTimeDelay = entry.getEarliestProcessingTimeDelay();
		if (earliestProcessingTimeDelay <= 0)
		{
			logAssertionError(entry, "Non-positive earliestProcessingTimeDelay [" + earliestProcessingTimeDelay + "] together with non-negative earliestProcessingTimeAnchor [" + earliestProcessingTimeAnchor + "]");
			
			// And fix the entry for the future
			entry.setEarliestProcessingTimeDelay(-1, -1);
			
			return 0; // no delay required
		}
		
		long timePassed = timeGapVirtual(earliestProcessingTimeAnchor, timeNow());
		long timeRemaining = earliestProcessingTimeDelay - timePassed;
		
		return timeRemaining;
	}
	
	
	/**
	 * Used to obtain/start thread that will be used to process request (via
	 * the given task).
	 * <p>
	 * Default implementation simply submits the task to {@link #requestsExecutorService}
	 */
	protected Future<Void> spiStartRequestProcessingThread(
		@SuppressWarnings("unused") RRLEntry<Input, Output> entry, Callable<Void> task)
	{
		return nn(requestsExecutorService).submit(task);
	}
	
	/**
	 * Used to obtain ticked needed for request processing.
	 * <p>
	 * Default implementation tries to obtain ticket from {@link #rateLimiter}
	 */
	@Nullable
	protected Object spiObtainTicket(@SuppressWarnings("unused") RRLEntry<Input, Output> entry, long maxWaitRealMs)
		throws InterruptedException
	{
		return rateLimiter.obtainTicket(maxWaitRealMs);
	}
	
	/**
	 * Used to return unused ticked.
	 * <p>
	 * Default implementation returns ticket to {@link #rateLimiter}
	 */
	protected void spiReturnUnusedTicket(@SuppressWarnings("unused") RRLEntry<Input, Output> entry, Object unusedTicket)
	{
		rateLimiter.returnUnusedTicket(TypeUtil.coerce(unusedTicket));
	}
	
	/**
	 * Determines whether service may accept a new incoming request.
	 * <p>
	 * Default implementation compares currently processing request count against
	 * {@link RRLConfig#getMaxPendingRequests()}
	 * 
	 * @return null if request may be accepted; string error message in case
	 * 		request should be rejected with {@link RejectedExecutionException}
	 */
	@SuppressWarnings("unused")
	@Nullable
	protected String spiMayAcceptRequest(Input incomingInput, long timeLimitMs)
	{
		int count = processingRequestsCount.get();
		if (count < config.getMaxPendingRequests())
			return null;
		else
			return "Too many already-processing requests, current count is: " + count;
	}
	
	
	/**
	 * Invoked after final failure of request processing (request never 
	 * completed without errors).
	 * <p>
	 * This is somewhat duplicated by {@link RRLEventListener#requestFinalFailure(RRLEntry, Throwable)},
	 * but it also present here for potentially better separation of concerns.
	 * 
	 * @param t if not null, then last attempt failure was caused by this
	 * 		{@link Throwable}; if null, then {@link RRLMainQueueProcessingDecision#FINAL_FAILURE}
	 * 		was returned by {@link #spiMainQueueProcessingDecision(RRLEntry, boolean, boolean)}
	 */
	@SuppressWarnings("unused")
	protected void afterRequestFinalFailure(RRLEntry<Input, Output> entry, @Nullable Throwable t)
	{
		// blank
	}
	
	/**
	 * Invoked after final timeout of request processing (request never completed 
	 * without errors before and now it is too late to try).
	 * <p>
	 * This is somewhat duplicated by {@link RRLEventListener#requestFinalTimeout(RRLEntry, long)},
	 * but it also present here for potentially better separation of concerns.
	 * 
	 * @param remainingValidityTime how long is remaining until request is still
	 * 		valid; in case of timeout this is often expected to be negative
	 * 		or zero/close-to-zero (negative means request is past validity time)
	 */
	@SuppressWarnings("unused")
	protected void afterRequestFinalTimeout(RRLEntry<Input, Output> entry, long remainingValidityTime)
	{
		// blank
	}
	
	/**
	 * Invoked after request succeeded.
	 * <p>
	 * This is somewhat duplicated by {@link RRLEventListener#requestSuccess(RRLEntry, Object, int, long)},
	 * but it also present here for potentially better separation of concerns.
	 */
	@SuppressWarnings("unused")
	protected void afterRequestSuccess(RRLEntry<Input, Output> entry, Output result, int attemptNumber, long requestDuration)
	{
		// blank
	}
	
	/**
	 * Invoked after request attempt failed.
	 * <p>
	 * This is somewhat duplicated by {@link RRLEventListener#requestAttemptFailed(RRLEntry, Exception, int, long)},
	 * but it also present here for potentially better separation of concerns.
	 */
	@SuppressWarnings("unused")
	protected void afterRequestAttemptFailed(RRLEntry<Input, Output> entry, Exception exception, int attemptNumber, long requestDuration)
	{
		// blank
	}
	
	//zzz comment
	protected Output spiProcessRequest(RRLEntry<Input, Output> entry, int attemptNumber) throws InterruptedException, Exception
	{
		return processRequest(entry.getInput(), attemptNumber);
	}
	
	//zzz comment
	//zzz this is allowed to return null
	protected abstract Output processRequest(Input input, int attemptNumber) throws InterruptedException, Exception;
	
	
	//zzz must take into account current state!
	public <T extends RetryAndRateLimitService<Input, Output>> T start()
	{
		mainQueueProcessingThread.start();
		
		for (RRLDelayQueueData dq : delayQueues)
			dq.getProcessingThread().start();
		
		return TypeUtil.coerce(this);
	}
	
	
	/**
	 * Submits request for execution and sets limit for how long this request
	 * may be processed until timing out (in [virtual] ms).
	 * 
	 * @param request must be not null
	 * @param timeLimitMs must be positive value
	 * @param delayBeforeFirstAttempMs (in virtual ms) delay before first
	 * 		actual request attempt is made; 0 or negative in case there needs
	 * 		not be any delay
	 * 
	 * @return future that can be used to interact with the request and obtain
	 * 		the results
	 */
	protected RRLEntry<Input, Output> internalSubmit(Input request, long timeLimitMs, long delayBeforeFirstAttempMs)
		throws IllegalArgumentException, RejectedExecutionException
	{
		//zzz must also check cache state (started/shutdown) before accepting
		
		if (nullable(request) == null)
			throw new IllegalArgumentException("request is null");
		if (timeLimitMs <= 0)
			throw new IllegalArgumentException("timeLimitMs must be positive, got: " + timeLimitMs);
		
		{
			String errMsg;
			try
			{
				errMsg = spiMayAcceptRequest(request, timeLimitMs);
			} catch (RuntimeException e)
			{
				try
				{
					// log error in spi method
					logSpiMethodException(null, e);
				} catch (InterruptedException e2)
				{
					throw new InterruptedRuntimeException(e2);
				}
				throw e;
			}
			if (errMsg != null)
			{
				sneakyGuardedEventListenerInvocation(evListener -> 
					evListener.errorRequestRejected(request, timeLimitMs, delayBeforeFirstAttempMs, errMsg));
				
				throw new RejectedExecutionException(errMsg);
			}
		}
		
		long now = timeNow();
		RRLEntry<@Nonnull Input, Output> entry = new RRLEntry<Input, Output>(this, request, now, timeLimitMs);
		
		if (delayBeforeFirstAttempMs > 0)
		{
			entry.setEarliestProcessingTimeDelay(now, delayBeforeFirstAttempMs);
		}
		
		mainQueue.add(entry);
		processingRequestsCount.incrementAndGet();
		
		sneakyGuardedEventListenerInvocation(evListener -> evListener.requestAdded(entry));
		
		return entry;
	}
	
	
	/**
	 * Submits request for execution and sets limit for how long this request
	 * may be processed until timing out (in [virtual] ms).
	 * 
	 * @param request must be not null
	 * @param timeLimitMs must be positive value
	 * 
	 * @return future that can be used to interact with the request and obtain
	 * 		the results
	 */
	public RRLFuture<Input, Output> submitFor(Input request, long timeLimitMs)
		throws IllegalArgumentException, RejectedExecutionException
	{
		return internalSubmit(request, timeLimitMs, -1).getFuture();
	}
	
	/**
	 * Submits request for execution and sets limit for how long this request
	 * may be processed until timing out (in [virtual] ms).
	 * <p>
	 * Request should not be attempted until given delay expires
	 * 
	 * @param request must be not null
	 * @param timeLimitMs must be positive value
	 * @param delayFor must be positive value, must be less than timeLimitMs
	 * 
	 * @return future that can be used to interact with the request and obtain
	 * 		the results
	 */
	public RRLFuture<Input, Output> submitForWithDelayFor(Input request, long timeLimitMs, long delayFor)
		throws IllegalArgumentException, RejectedExecutionException
	{
		if (delayFor <= 0)
			throw new IllegalArgumentException("delayFor must be positive, got: " + delayFor);
		if (delayFor >= timeLimitMs)
			throw new IllegalArgumentException("delayFor [" + delayFor + "] must be less than timeLimitMs: " + timeLimitMs);
		
		return internalSubmit(request, timeLimitMs, delayFor).getFuture();
	}
	
	/**
	 * Submits request for execution and sets limit for how long (until what
	 * timestamp) this request may be processed (it will time-out after).
	 * 
	 * @param request must be not null
	 * @param untilTimestamp must be after current time
	 * 
	 * @return future that can be used to interact with the request and obtain
	 * 		the results
	 */
	public RRLFuture<Input, Output> submitUntil(Input request, long untilTimestamp)
		throws IllegalArgumentException, RejectedExecutionException
	{
		long timeLimitMs = timeGapVirtual(timeNow(), untilTimestamp);
		if (timeLimitMs < 0)
			throw new IllegalArgumentException("untilTimestamp [" + untilTimestamp + "] is not after 'now'");
		
		return internalSubmit(request, timeLimitMs, -1).getFuture();
	}
	
	/**
	 * Submits request for execution and sets limit for how long (until what
	 * timestamp) this request may be processed (it will time-out after).
	 * <p>
	 * Request should not be attempted until the given delayUntil timestamp
	 * 
	 * @param request must be not null
	 * @param untilTimestamp must be after current time
	 * @param delayUntilTimestamp must be after current time; must be before untilTimestamp
	 * 
	 * @return future that can be used to interact with the request and obtain
	 * 		the results
	 */
	public RRLFuture<Input, Output> submitUntilWithDelayUntil(Input request, long untilTimestamp, long delayUntilTimestamp)
		throws IllegalArgumentException, RejectedExecutionException
	{
		if (delayUntilTimestamp <= 0)
			throw new IllegalArgumentException("delayUntilTimestamp must be positive, got: " + delayUntilTimestamp);
		if (delayUntilTimestamp >= untilTimestamp)
			throw new IllegalArgumentException("delayUntilTimestamp [" + delayUntilTimestamp + "] must be less than untilTimestamp: " + untilTimestamp);
		
		long now = timeNow();
		long timeLimitMs = timeGapVirtual(now, untilTimestamp);
		if (timeLimitMs < 0)
			throw new IllegalArgumentException("untilTimestamp [" + untilTimestamp + "] is not after 'now'");
		long delayFor = timeGapVirtual(now, delayUntilTimestamp);
		if (delayFor < 0)
			throw new IllegalArgumentException("delayUntilTimestamp [" + delayUntilTimestamp + "] is not after 'now'");
		
		return internalSubmit(request, timeLimitMs, delayFor).getFuture();
	}
}
