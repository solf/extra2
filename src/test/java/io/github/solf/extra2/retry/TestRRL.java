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
package io.github.solf.extra2.retry;

import static io.github.solf.extra2.testutil.AssertExtra.assertBetweenInclusive;
import static io.github.solf.extra2.testutil.AssertExtra.assertContains;
import static io.github.solf.extra2.testutil.AssertExtra.assertFailsWithSubstring;
import static io.github.solf.extra2.util.NullUtil.fakeNonNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.joda.time.LocalDateTime;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.github.solf.extra2.concurrent.exception.ExecutionRuntimeException;
import io.github.solf.extra2.config.Configuration;
import io.github.solf.extra2.config.OverrideFlatConfiguration;
import io.github.solf.extra2.lambda.TriConsumer;
import io.github.solf.extra2.retry.RRLConfig;
import io.github.solf.extra2.retry.RRLEventListener;
import io.github.solf.extra2.retry.RRLFuture;
import io.github.solf.extra2.retry.RRLStatus;
import io.github.solf.extra2.retry.RRLTimeoutException;
import io.github.solf.extra2.retry.RetryAndRateLimitService;
import io.github.solf.extra2.util.TypeUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Tests for {@link RetryAndRateLimitService}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
@Slf4j
public class TestRRL
{
	@BeforeClass
	public void beforeClass() throws InterruptedException
	{
		// Do a warm-up so that timings are more stable.
		
		LinkedBlockingQueue<String> dump = new LinkedBlockingQueue<>();
		
		RRLConfig config = new RRLConfig(Configuration.fromPropertiesFile("retry/simpleCasesTest"));
		RetryAndRateLimitService<String, String> service = new RetryAndRateLimitService<String, String>(config)
		{
			@Override
			protected String processRequest(String input, int attemptNumber)
			{
				return "success";
			}

			@SuppressWarnings("hiding")
			@Override
			protected RRLEventListener<String, String> spiCreateEventListener(
				RRLConfig config, String commonNamingPrefix,
				ThreadGroup threadGroup)
			{
				return createEventListenerProxy(
					(proxy, method, methodArgs) -> {
						dump.add("" + new LocalDateTime() + " " + "[" + method.getName() + "]: " + Arrays.toString(methodArgs));
					});
			}
		}.start();
		
		service.submitFor("request", 1000);
		
		Thread.sleep(150);
		
		//zzz shutdown?
	}
	
	/**
	 * Record of processing attempt.
	 */
	@RequiredArgsConstructor
	private static class AttemptRecord<Input, Output>
	{
		@Getter
		private final long timestamp;
		
		@Getter
		private final int attemptNumber; 
		
		@Getter
		private final Input input;
		
		/**
		 * Null if unsuccessful.
		 */
		@Getter
		@Nullable
		private final Output output;
		
	}
	
	/**
	 * Record of {@link RRLEventListener} event (method invocation).
	 */
	private static class EventListenerEvent
	{
		@Getter
		private final Object listenerInstance;
		
		@Getter
		private final Method method;
		
		@Nullable
		private final Object @Nullable[] args;
		@Nullable
		public Object @Nullable[] getArgs() {return args;}
		
		/**
		 * Constructor.
		 */
		public EventListenerEvent(Object listenerInstance, Method method, @Nullable Object @Nullable[] args)
		{
			this.listenerInstance = listenerInstance;
			this.method = method;
			this.args = args;
		}
	}
	
	/**
	 * Checks that specific {@link AttemptRecord} matches.
	 */
	private <Input, Output> void checkAttempt(AttemptRecord<Input, Output> attempt,
		int attemptNumber, Input request, @Nullable Output result, long minTimestampInclusive, long maxTimestampInclusive)
	{
		assertEquals(attempt.getAttemptNumber(), attemptNumber, "attempt number");
		assertEquals(attempt.getInput(), request, "request");
		assertEquals(attempt.getOutput(), result, "result");
		assertBetweenInclusive(attempt.getTimestamp(), minTimestampInclusive, maxTimestampInclusive);
	}
	
	/**
	 * Checks that specific {@link EventListenerEvent} matches.
	 */
	@SuppressWarnings("unused")
	private void checkEvent(EventListenerEvent event, String methodName, Object... args)
	{
		assertEquals(event.getMethod().getName(), methodName, "method");
		
		@Nullable Object[] actualArgs = event.getArgs();
		int argSize = actualArgs == null ? 0 : actualArgs.length;
		assertEquals(argSize, args.length, "args length");
		
		if (actualArgs == null)
			return;
		
		int index = -1;
		for (Object arg : args)
		{
			index++;
			
			assertEquals(actualArgs[index], arg, "arg N" + (index + 1));
		}
	}
	
	@Test
	public void simpleCasesTest() throws InterruptedException
	{
		//zzz test all the different flavours of future#get
		
		final LinkedBlockingQueue<AttemptRecord<String, String>> attempts = new LinkedBlockingQueue<>();
		final LinkedBlockingQueue<EventListenerEvent> events = new LinkedBlockingQueue<>();
		
		final AtomicInteger failUntilAttempt = new AtomicInteger(3);
		
		RRLConfig config = new RRLConfig(Configuration.fromPropertiesFile("retry/simpleCasesTest"));
		RetryAndRateLimitService<String, String> service = new RetryAndRateLimitService<String, String>(config)
		{
			@Override
			protected String processRequest(String input, int attemptNumber) throws InterruptedException
			{
//				System.out.println("" + new Date() + " " + attemptNumber + ": " + System.currentTimeMillis());{}
				
				String result = null;
				if (attemptNumber >= failUntilAttempt.get())
					result = "success: " + input;
					
				Thread.sleep(20);
				
				attempts.add(new AttemptRecord<>(System.currentTimeMillis(), attemptNumber, input, result));
					
				if (result == null)
					throw new IllegalStateException("attempt: " + attemptNumber);
				
				return result;
			}

			@SuppressWarnings("hiding")
			@Override
			protected RRLEventListener<String, String> spiCreateEventListener(
				RRLConfig config, String commonNamingPrefix,
				ThreadGroup threadGroup)
			{
				return createEventListenerProxy(
					(proxy, method, methodArgs) -> {
						events.add(new EventListenerEvent(proxy, method, methodArgs));
//						System.out.println("" + new LocalDateTime() + " " + "[" + method.getName() + "]: " + Arrays.toString(methodArgs));{}
					});
			}
			
			
		};
		
		final RRLStatus beforeStartStatus = service.getStatus(0);
		{
			assertBetweenInclusive(beforeStartStatus.getStatusCreatedAt(), System.currentTimeMillis() - 15, System.currentTimeMillis());
			
//zzz			assertFalse(beforeStartStatus.isServiceAlive());
//zzz			assertFalse(beforeStartStatus.isServiceUsable());
			
			assertFalse(beforeStartStatus.isEverythingAlive());
			assertFalse(beforeStartStatus.isMainQueueProcessingThreadAlive());
			assertFalse(beforeStartStatus.isDelayQueueProcessingThreadsAreAlive());
			
			assertTrue(beforeStartStatus.isRequestsExecutorServiceAlive());
		}
		
		service.start();
		
		{
			// check status caching
			RRLStatus status = service.getStatus(2000);
			assertEquals(status, beforeStartStatus);
			
			//zzz			assertFalse(status.isServiceAlive());
			//zzz			assertFalse(status.isServiceUsable());
						
			assertFalse(status.isEverythingAlive());
			assertFalse(status.isMainQueueProcessingThreadAlive());
			assertFalse(status.isDelayQueueProcessingThreadsAreAlive());
			
			assertTrue(status.isRequestsExecutorServiceAlive());
		}
		
		{
			// check status non-caching
			Thread.sleep(10);
			RRLStatus status = service.getStatus(5);
			assertNotEquals(status, beforeStartStatus);
			assertBetweenInclusive(status.getStatusCreatedAt(), System.currentTimeMillis() - 15, System.currentTimeMillis());
			
			//zzz			assertTrue(status.isServiceAlive());
			//zzz			assertTrue(status.isServiceUsable());
						
			assertTrue(status.isEverythingAlive());
			assertTrue(status.isMainQueueProcessingThreadAlive());
			assertTrue(status.isDelayQueueProcessingThreadsAreAlive());
			
			assertTrue(status.isRequestsExecutorServiceAlive());
			
			assertEquals(status.getCurrentProcessingRequestsCount(), 0);
			assertEquals(status.getMainQueueSize(), 0);
			assertEquals(status.getRequestsExecutorServiceActiveThreads(), 0);
		}
		
		{
			// Check simple success case with two retries
			failUntilAttempt.set(3);
			attempts.clear();
			events.clear();
			
			final long start = System.currentTimeMillis();
			
			RRLFuture<String, String> future = service.submitFor("request", 5000);
			
			assertEquals(future.getOrNull(2000, TimeUnit.MILLISECONDS), "success: request");
			
			checkAttempt(attempts.poll(), 1, "request", null, start, start + 100);
			checkAttempt(attempts.poll(), 2, "request", null, start + 120, start + 220);
			checkAttempt(attempts.poll(), 3, "request", "success: request", start + 940, start + 1140);
			assertNull(attempts.poll());
			
	//zzz		checkEvent(events.poll(), "requestAdded"); need to do checkEvent tests
			
			
			{
				// check status
				Thread.sleep(100);
				RRLStatus status = service.getStatus(5);
				assertBetweenInclusive(status.getStatusCreatedAt(), System.currentTimeMillis() - 15, System.currentTimeMillis());
				
				//zzz			assertTrue(status.isServiceAlive());
				//zzz			assertTrue(status.isServiceUsable());
							
				assertTrue(status.isEverythingAlive());
				assertTrue(status.isMainQueueProcessingThreadAlive());
				assertTrue(status.isDelayQueueProcessingThreadsAreAlive());
				
				assertTrue(status.isRequestsExecutorServiceAlive());
				
				assertEquals(status.getCurrentProcessingRequestsCount(), 0);
				assertEquals(status.getMainQueueSize(), 0);
				assertEquals(status.getRequestsExecutorServiceActiveThreads(), 0);
			}
		}
		
		{
			// tests 'not earlier than' processing
			failUntilAttempt.set(0);
			attempts.clear();
			events.clear();
			
			final long now = System.currentTimeMillis();
			final long delayFor = 300;
			final long later = now + delayFor;
			
			RRLFuture<String, String> fDelayUntil = service.submitUntilWithDelayUntil("delayUntil", now + 2000, later);
			Thread.sleep(100);
			RRLFuture<String, String> fDelayFor = service.submitForWithDelayFor("delayFor", 2000, delayFor);
			
			assertEquals(fDelayUntil.getOrNull(2000, TimeUnit.MILLISECONDS), "success: delayUntil");
			assertEquals(fDelayFor.getOrNull(2000, TimeUnit.MILLISECONDS), "success: delayFor");
			
			checkAttempt(attempts.poll(), 1, "delayUntil", "success: delayUntil", later, later + 100);
			checkAttempt(attempts.poll(), 1, "delayFor", "success: delayFor", later + 100, later + 200);
			assertNull(attempts.poll());
		}

		
		{
			// tests for timeout
			failUntilAttempt.set(3);
			attempts.clear();
			events.clear();
			
			final long start = System.currentTimeMillis();
			try
			{
				service.submitFor("timeout", 300).getOrNull(2000);
				fail("should not be reacheable");
			} catch (RRLTimeoutException e)
			{
				assertTrue(e.getCause() instanceof RRLTimeoutException);
				assertContains(e, "Request timed out after");
			}
			final long duration = System.currentTimeMillis() - start;
			assertBetweenInclusive(duration, 300L, 400L);
			
			checkAttempt(attempts.poll(), 1, "timeout", null, start, start + 100);
			checkAttempt(attempts.poll(), 2, "timeout", null, start + 120, start + 220);
			assertNull(attempts.poll());
		}

		
		{
			// tests for all attempts fail
			failUntilAttempt.set(300);
			attempts.clear();
			events.clear();
			
			final long start = System.currentTimeMillis();
			try
			{
				service.submitFor("failure", 5000).getOrNull(2000);
				fail("should not be reacheable");
			} catch (ExecutionRuntimeException e)
			{
				assertTrue(e.getCause() instanceof IllegalStateException);
				assertContains(e, "java.lang.IllegalStateException: attempt: 3");
			}
			final long duration = System.currentTimeMillis() - start;
			assertBetweenInclusive(duration, 940L, 1200L);
			
			checkAttempt(attempts.poll(), 1, "failure", null, start, start + 100);
			checkAttempt(attempts.poll(), 2, "failure", null, start + 120, start + 220);
			checkAttempt(attempts.poll(), 3, "failure", null, start + 940, start + 1140);
			assertNull(attempts.poll());
		}
		
		{
			// check status
			Thread.sleep(100);
			RRLStatus status = service.getStatus(0);
			assertBetweenInclusive(status.getStatusCreatedAt(), System.currentTimeMillis() - 15, System.currentTimeMillis());
			
			//zzz			assertTrue(status.isServiceAlive());
			//zzz			assertTrue(status.isServiceUsable());
						
			assertTrue(status.isEverythingAlive());
			assertTrue(status.isMainQueueProcessingThreadAlive());
			assertTrue(status.isDelayQueueProcessingThreadsAreAlive());
			
			assertTrue(status.isRequestsExecutorServiceAlive());
			
			assertEquals(status.getCurrentProcessingRequestsCount(), 0);
			assertEquals(status.getMainQueueSize(), 0);
			assertEquals(status.getRequestsExecutorServiceActiveThreads(), 0);
		}
		
		// ================ DESTRUCTIVE TEST ==================
		
		{
			// check max pending requests limit
			failUntilAttempt.set(0);
			attempts.clear();
			events.clear();
			
			for (int i = 0; i < 100; i++)
				service.submitForWithDelayFor("request " + i, 1000, 100);
			
			assertFailsWithSubstring(() -> service.submitFor("request too many", 1000), 
				"java.util.concurrent.RejectedExecutionException: Too many already-processing requests");
		}
	}
	
	
	@Test
	public void testGrace() throws InterruptedException
	{
		final LinkedBlockingQueue<AttemptRecord<String, String>> attempts = new LinkedBlockingQueue<>();
		final LinkedBlockingQueue<EventListenerEvent> events = new LinkedBlockingQueue<>();
		
		final AtomicInteger failUntilAttempt = new AtomicInteger(0);
		
		OverrideFlatConfiguration overrideConfig = new OverrideFlatConfiguration("retry/simpleCasesTest");
		overrideConfig.override("serviceName", "testGrace");
		overrideConfig.override("requestEarlyProcessingGracePeriod", "50ms");
		
		RRLConfig config = new RRLConfig(overrideConfig);
		RetryAndRateLimitService<String, String> service = new RetryAndRateLimitService<String, String>(config)
		{
			@Override
			protected String processRequest(String input, int attemptNumber) throws InterruptedException
			{
//				System.out.println("" + new Date() + " " + attemptNumber + ": " + System.currentTimeMillis());{}
				
				String result = null;
				if (attemptNumber >= failUntilAttempt.get())
					result = "success: " + input;
					
				Thread.sleep(20);
				
				attempts.add(new AttemptRecord<>(System.currentTimeMillis(), attemptNumber, input, result));
					
				if (result == null)
					throw new IllegalStateException("attempt: " + attemptNumber);
				
				return result;
			}

			@SuppressWarnings("hiding")
			@Override
			protected RRLEventListener<String, String> spiCreateEventListener(
				RRLConfig config, String commonNamingPrefix,
				ThreadGroup threadGroup)
			{
				return createEventListenerProxy(
					(proxy, method, methodArgs) -> {
						events.add(new EventListenerEvent(proxy, method, methodArgs));
//						System.out.println("" + new LocalDateTime() + " " + "[" + method.getName() + "]: " + Arrays.toString(methodArgs));{}
					});
			}
			
			
		};
		service.start();
		
		
		{
			// Check grace functionality
			failUntilAttempt.set(0);
			attempts.clear();
			events.clear();
			
			final long start = System.currentTimeMillis();
			
			RRLFuture<String, String> fDelay70 = service.submitForWithDelayFor("delay70", 2000, 70);
			RRLFuture<String, String> fDelay40 = service.submitForWithDelayFor("delay40", 2000, 40);
			
			assertEquals(fDelay40.getOrNull(200, TimeUnit.MILLISECONDS), "success: delay40");
			assertEquals(fDelay70.getOrNull(200, TimeUnit.MILLISECONDS), "success: delay70");
			
			checkAttempt(attempts.poll(), 1, "delay40", "success: delay40", start, start + 80);
			checkAttempt(attempts.poll(), 1, "delay70", "success: delay70", start + 90, start + 180);
			assertNull(attempts.poll());
		}
	}
	
	@Test
	public void testThreadLimit() throws InterruptedException
	{
		final AtomicBoolean doDelay = new AtomicBoolean(true);
		
		RRLConfig config = new RRLConfig(Configuration.fromPropertiesFile("retry/threadLimit"));
		RetryAndRateLimitService<String, String> service = new RetryAndRateLimitService<String, String>(config)
		{
			@Override
			protected String processRequest(String input, int attemptNumber) throws InterruptedException
			{
				if (doDelay.get())
					Thread.sleep(500);
				
				return "success: " + input;
			}
		};
		service.start();
		
		ArrayList<RRLFuture<String, String>> futures = new ArrayList<>();
		for (int i = 0; i < 50; i++)
			futures.add(service.submitFor("request " + i, 5000));
		
		Thread.sleep(250);
		
		{
			// test threads usage
			RRLStatus status = service.getStatus(0);
			
			assertEquals(status.getRequestsExecutorServiceActiveThreads(), 10);
			assertEquals(status.getCurrentProcessingRequestsCount(), futures.size());
			assertEquals(status.getMainQueueSize(), futures.size() - 10 - 1);
		}
		
		doDelay.set(false);
		
		final long start = System.currentTimeMillis();
		
		for (RRLFuture<String, String> future : futures)
			assertEquals(future.getOrNull(500), "success: " + future.getTask(), future.getTask());
		
		assertBetweenInclusive(System.currentTimeMillis() - start, 200L, 500L);
		
		Thread.sleep(100);
		
		{
			// test requests / queue sizes
			RRLStatus status = service.getStatus(0);
			
			assertEquals(status.getCurrentProcessingRequestsCount(), 0);
			assertEquals(status.getMainQueueSize(), 0);
			assertEquals(status.getRequestsExecutorServiceActiveThreads(), 0);
		}
	}
	
	//zzz comment
	private static enum FullTestItemMode
	{
		NOT_EARLIER_THAN,
		
		NO_FAIL_SUCCESS,
		FAIL_1_SUCCESS,
		FAIL_ALWAYS,
		FAIL_TIMEOUT,
		
		TIMEOUT_WITHOUT_ATTEMPTS,
	}
	
	
	//zzz comment
	@RequiredArgsConstructor
	@ToString(doNotUseGetters = true)
	private static class FullTestItem
	{
		/**
		 * Special value to mark result as timeout.
		 */
		public static final String RESULT_TIMEOUT = "<<-TIMEOUT->>";
		/**
		 * Special value to mark result as fail.
		 */
		public static final String RESULT_FAIL = "<<-FAIL->>";
		
		
		@Getter
		private final FullTestItemMode mode;
		
		/**
		 * Request string that is used to generate result from.
		 */
		@Getter
		private final String request;
		
		@Getter @Setter
		private volatile RRLFuture<FullTestItem, String> future = fakeNonNull();
		
		@Getter
		private volatile LinkedBlockingQueue<AttemptRecord<FullTestItem, String>> attemptsQueue = new LinkedBlockingQueue<>();
		
		@Nullable
		private volatile ArrayList<AttemptRecord<FullTestItem, String>> attemptsList = null;
		/**
		 * Gets view of the attempts as the list.
		 * <p>
		 * AFTER CALLING THIS METHOD QUEUE BECOMES UNUSABLE!!!
		 */
		public ArrayList<AttemptRecord<FullTestItem, String>> getAttemptsList()
		{
			ArrayList<AttemptRecord<FullTestItem, String>> retVal = attemptsList;
			if (retVal == null)
			{
				retVal = new ArrayList<>(attemptsQueue);
				attemptsList = retVal;
				
				attemptsQueue = fakeNonNull(); // make queue unusable
			}
			
			return retVal;
		}
		
		/**
		 * When item processing was completed; initially -1
		 */
		@Getter
		private volatile long completedAt = -1;
		/**
		 * null -- timeout; true -- success; false -- failure
		 */
		@Getter
		private volatile String result;
		/**
		 * @param result -- resulting value or {@link #RESULT_TIMEOUT} or {@link #RESULT_FAIL}
		 */
		public void setCompletedAt(long completedAt, String result)
		{
			if (this.completedAt != -1)
				throw new IllegalStateException("Trying to change completedAt from " + this.completedAt + " to " + completedAt);
			
			this.completedAt = completedAt;
			this.result = result;
		}
		
	}
	
	
	
	@Test
	public void fullTest() throws InterruptedException
	{
		RRLConfig config = new RRLConfig(Configuration.fromPropertiesFile("retry/fullTest"));
		RetryAndRateLimitService<FullTestItem, String> service = new RetryAndRateLimitService<FullTestItem, String>(config)
		{
			@Override
			protected String processRequest(FullTestItem input, int attemptNumber) throws InterruptedException
			{
				String result = null;
				try
				{
					result = processRequest0(input, attemptNumber);
					return result;
				} finally
				{
					// record attempt
					input.getAttemptsQueue().add(new AttemptRecord<>(
						System.currentTimeMillis(), attemptNumber, input, result));
				}
			}
				
			protected String processRequest0(FullTestItem input, int attemptNumber)
			{
				switch (input.getMode())
				{
					case FAIL_1_SUCCESS:
						if (attemptNumber == 1)
							throw new IllegalStateException("attempt " + attemptNumber);
						break;
						
					case FAIL_ALWAYS:
					case FAIL_TIMEOUT:
						throw new IllegalStateException("attempt " + attemptNumber);
						
					case NOT_EARLIER_THAN:
					case NO_FAIL_SUCCESS:
					case TIMEOUT_WITHOUT_ATTEMPTS:
						break; // these complete normally
				}
				
				return "success: " + input.getRequest();
			}

			@Override
			protected void afterRequestFinalFailure(
				RRLEntry<FullTestItem, String> entry,
				@Nullable Throwable t)
			{
				entry.getInput().setCompletedAt(System.currentTimeMillis(), FullTestItem.RESULT_FAIL);
			}

			@Override
			protected void afterRequestFinalTimeout(
				RRLEntry<FullTestItem, String> entry,
				long remainingValidityTime)
			{
				entry.getInput().setCompletedAt(System.currentTimeMillis(), FullTestItem.RESULT_TIMEOUT);
			}

			@Override
			protected void afterRequestSuccess(
				RRLEntry<FullTestItem, String> entry, String result,
				int attemptNumber, long requestAttemptDuration)
			{
				entry.getInput().setCompletedAt(System.currentTimeMillis(), result);
			}
		};
		service.start();
		
		List<FullTestItem> firstList = new ArrayList<>(25);
		List<FullTestItem> mainList = new ArrayList<>(125);
		List<FullTestItem> lastList = new ArrayList<>(25);
		
		{
			int itemCount = 1;
			// 'not earlier than' go into first list at least in part
			for (int i = 0; i < 10; i++)
			{
				firstList.add(new FullTestItem(FullTestItemMode.NOT_EARLIER_THAN, "not-earlier-than-first " + (itemCount++)));
				mainList.add(new FullTestItem(FullTestItemMode.NOT_EARLIER_THAN, "not-earlier-than-main " + (itemCount++)));
			}
			// stuff that just goes into main list
			for (int i = 0; i < 20; i++)
			{
				mainList.add(new FullTestItem(FullTestItemMode.NO_FAIL_SUCCESS, "no-fail " + (itemCount++)));
				mainList.add(new FullTestItem(FullTestItemMode.FAIL_1_SUCCESS, "fail-1 " + (itemCount++)));
				mainList.add(new FullTestItem(FullTestItemMode.FAIL_ALWAYS, "fail-1 " + (itemCount++)));
				mainList.add(new FullTestItem(FullTestItemMode.FAIL_TIMEOUT, "fail-timeout " + (itemCount++)));
			}
			// stuff that goes into 'last list' -- i.e. timeouts w/o fail
			for (int i = 0; i < 20; i++)
			{
				lastList.add(new FullTestItem(FullTestItemMode.TIMEOUT_WITHOUT_ATTEMPTS, "timeout-no-attempt " + (itemCount++)));
			}
		}
		
		// Shuffle lists for better testing
		Collections.shuffle(firstList);
		Collections.shuffle(mainList);
		Collections.shuffle(lastList);
		
		// Submit everything
		final long start = System.currentTimeMillis();
		final long maxTimeLimit = 15_000;
		final long maxEndTime = start + maxTimeLimit;
		final long delayItemsDelayFor = 5_000;
		int itemsCount = 0;
		for (List<FullTestItem> list : List.of(firstList, mainList, lastList))
		{
			for (FullTestItem item : list)
			{
				itemsCount++;
				RRLFuture<FullTestItem, String> future = fakeNonNull(); // make compiler happy
				switch (item.getMode())
				{
					case FAIL_1_SUCCESS:
					case FAIL_ALWAYS:
					case NO_FAIL_SUCCESS:
						future = service.submitFor(item, maxTimeLimit);
						break;
					case FAIL_TIMEOUT:
						future = service.submitFor(item, 4000); // should timeout before the 3rd attempt
						break;
					case NOT_EARLIER_THAN:
						future = service.submitForWithDelayFor(item, maxTimeLimit, delayItemsDelayFor);
						break;
					case TIMEOUT_WITHOUT_ATTEMPTS:
						future = service.submitFor(item, 2000); // these go at the end, with 30/sec should timeout before getting to those
						break;
				}
				
				item.setFuture(future);
			}
		}
		
		{
			// test requests / queue sizes
			RRLStatus status = service.getStatus(0);
			
			assertBetweenInclusive(status.getCurrentProcessingRequestsCount(), itemsCount - 30 /*skipped items + rate limiter guess*/, itemsCount);
			assertBetweenInclusive(status.getMainQueueSize(), itemsCount - 30 /*skipped items + rate limiter guess*/, itemsCount);
		}
		
		// Make sure all items have completed.
		for (List<FullTestItem> list : List.of(firstList, mainList, lastList))
		{
			for (FullTestItem item : list)
			{
				String result;
				try
				{
					result = item.getFuture().getOrNull(
						Math.max(
							start + maxTimeLimit - System.currentTimeMillis(),
							1
						));
				} catch (RuntimeException e)
				{
					// this is okay, many of them should fail
					result = "had exception";
				}
				
				assertNotNull(result, "Null result for: " + item);
			}
		}
		
		final long end = System.currentTimeMillis();
		final long duration = end - start;
		
		log.info("Full test duration for all futures to complete: {} ms", duration);
		
		// Validate results.
		for (List<FullTestItem> list : List.of(firstList, mainList, lastList))
		{
			for (FullTestItem item : list)
			{
				ArrayList<AttemptRecord<FullTestItem, String>> attemptsList = item.getAttemptsList();
				
				{
					// Validate intervals between attempts (if any).
					long prev = start;
					int count = 0;
					for (AttemptRecord<FullTestItem, String> attempt : attemptsList)
					{
						count++;
						long time = attempt.getTimestamp();
						
						final long minInterval;
						switch (count)
						{
							case 1:
								minInterval = 0;
								break;
							case 2:
								minInterval = 1000;
								break;
							case 3:
								minInterval = 4000;
								break;
							default:
								fail("Too many attempts" + item);
								return; // for compiler happiness
						}
						
						assertBetweenInclusive(time, prev + minInterval, maxEndTime, "Attempt " + count + " in: " + item);
						
						prev = time;
					}
				}
				
				switch (item.getMode())
				{
					case FAIL_1_SUCCESS:
						assertEquals(attemptsList.size(), 2, item.toString());
						assertEquals(item.getResult(), "success: " + item.getRequest(), item.toString());
						assertEquals(item.getFuture().getOrNull(0), "success: " + item.getRequest(), item.toString());
						break;
					case FAIL_ALWAYS:
						assertEquals(attemptsList.size(), 3, item.toString());
						assertEquals(item.getResult(), FullTestItem.RESULT_FAIL, item.toString());
						assertFailsWithSubstring(() -> item.getFuture().getOrNull(0), "ExecutionRuntimeException"); 
						break;
					case FAIL_TIMEOUT:
						assertBetweenInclusive(attemptsList.size(), 1, 2, item.toString());
						assertEquals(item.getResult(), FullTestItem.RESULT_TIMEOUT, item.toString());
						assertFailsWithSubstring(() -> item.getFuture().getOrNull(0), "RRLTimeoutException"); 
						break;
					case NOT_EARLIER_THAN:
						assertEquals(attemptsList.size(), 1, item.toString());
						assertEquals(item.getResult(), "success: " + item.getRequest(), item.toString());
						assertEquals(item.getFuture().getOrNull(0), "success: " + item.getRequest(), item.toString());
						assertBetweenInclusive(item.getCompletedAt(), start + delayItemsDelayFor, maxEndTime, item.toString());
						break;
					case NO_FAIL_SUCCESS:
						assertEquals(attemptsList.size(), 1, item.toString());
						assertEquals(item.getResult(), "success: " + item.getRequest(), item.toString());
						assertEquals(item.getFuture().getOrNull(0), "success: " + item.getRequest(), item.toString());
						assertBetweenInclusive(item.getCompletedAt(), start, start + 4000 /*should be enough time to process queue*/, item.toString());
						break;
					case TIMEOUT_WITHOUT_ATTEMPTS:
						assertEquals(attemptsList.size(), 0, item.toString());
						assertEquals(item.getResult(), FullTestItem.RESULT_TIMEOUT, item.toString());
						assertFailsWithSubstring(() -> item.getFuture().getOrNull(0), "RRLTimeoutException");
						// hard to guess when main queue will be able to timeout items, but 6000 ought to cover it
						assertBetweenInclusive(item.getCompletedAt(), start, start + 6000, item.toString());
						break;
				}
			}
		}
		
		Thread.sleep(100);
		
		{
			// test requests / queue sizes
			RRLStatus status = service.getStatus(0);
			
			assertEquals(status.getCurrentProcessingRequestsCount(), 0);
			assertEquals(status.getMainQueueSize(), 0);
			assertEquals(status.getRequestsExecutorServiceActiveThreads(), 0);
		}
	}
	
	
	/**
	 * Creates event listener proxy.
	 */
	private static <I,O> RRLEventListener<@Nonnull I, O> createEventListenerProxy(TriConsumer<Object, Method, @Nullable Object @Nullable[]> handler)
	{
		return TypeUtil.coerce(Proxy.newProxyInstance(TestRRL.class.getClassLoader(), 
			new Class<?>[] {RRLEventListener.class},
			(proxy, method, methodArgs) -> {
				handler.accept(proxy, method, methodArgs);
				
				return null;
			}));
		
	}
}
