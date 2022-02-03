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
import static io.github.solf.extra2.testutil.AssertExtra.assertFails;
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
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.joda.time.LocalDateTime;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.github.solf.extra2.concurrent.ConsumerWithException;
import io.github.solf.extra2.concurrent.exception.ExecutionRuntimeException;
import io.github.solf.extra2.config.Configuration;
import io.github.solf.extra2.config.FlatConfiguration;
import io.github.solf.extra2.config.OverrideFlatConfiguration;
import io.github.solf.extra2.lambda.TriConsumer;
import io.github.solf.extra2.retry.RRLConfig;
import io.github.solf.extra2.retry.RRLEventListener;
import io.github.solf.extra2.retry.RRLFuture;
import io.github.solf.extra2.retry.RRLStatus;
import io.github.solf.extra2.retry.RRLTimeoutException;
import io.github.solf.extra2.retry.RetryAndRateLimitService;
import io.github.solf.extra2.testutil.TestUtil;
import io.github.solf.extra2.testutil.TestUtil.AsyncTestRunner;
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
		
		service.shutdownFor(500, true, true);
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
		final LinkedBlockingQueue<AttemptRecord<String, String>> attempts = new LinkedBlockingQueue<>();
		final LinkedBlockingQueue<EventListenerEvent> events = new LinkedBlockingQueue<>();
		
		final AtomicInteger failUntilAttempt = new AtomicInteger(3);
		
		RRLConfig config = new RRLConfig(Configuration.fromPropertiesFile("retry/simpleCasesTest"));
		RetryAndRateLimitService<String, String> service = createBasicService(
			config, 20, failUntilAttempt, attempts, events);
		
		final RRLStatus beforeStartStatus = service.getStatus(0);
		{
			assertBetweenInclusive(beforeStartStatus.getStatusCreatedAt(), System.currentTimeMillis() - 15, System.currentTimeMillis());
			
			assertFalse(beforeStartStatus.isAcceptingRequests());
			assertEquals(beforeStartStatus.getCurrentProcessingRequestsCount(), 0);
			
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
			
			assertFalse(status.isAcceptingRequests());
			assertEquals(status.getCurrentProcessingRequestsCount(), 0);
						
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
			
			assertTrue(status.isAcceptingRequests());
			assertEquals(status.getCurrentProcessingRequestsCount(), 0);
						
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
			
	//TO-DO		checkEvent(events.poll(), "requestAdded"); need to do checkEvent tests
			
			
			{
				// check status
				Thread.sleep(100);
				RRLStatus status = service.getStatus(5);
				assertBetweenInclusive(status.getStatusCreatedAt(), System.currentTimeMillis() - 15, System.currentTimeMillis());
				
				assertTrue(status.isAcceptingRequests());
				assertEquals(status.getCurrentProcessingRequestsCount(), 0);
							
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
			
			assertTrue(status.isAcceptingRequests());
			assertEquals(status.getCurrentProcessingRequestsCount(), 0);
						
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

	/**
	 * Creates a basic RRL service for testing.
	 * 
	 * @param config configuration for service
	 * @param failUntilAttempt all attempts with lower number than this will fail
	 * @param attempts where to store attempts
	 * @param events where to store events
	 */
	private RetryAndRateLimitService<String, String> createBasicService(
		RRLConfig config, final long processingDelay, final AtomicInteger failUntilAttempt,
		final LinkedBlockingQueue<AttemptRecord<String, String>> attempts,
		final LinkedBlockingQueue<EventListenerEvent> events)
	{
		RetryAndRateLimitService<String, String> service = new RetryAndRateLimitService<String, String>(config)
		{
			@Override
			protected String processRequest(String input, int attemptNumber) throws InterruptedException
			{
//				System.out.println("" + new Date() + " " + attemptNumber + ": " + System.currentTimeMillis());{}
				
				String result = null;
				if (attemptNumber >= failUntilAttempt.get())
					result = "success: " + input;
					
				if (processingDelay > 0)
					Thread.sleep(processingDelay);
				
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
		return service;
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
	
	/**
	 * Different processing options for full test items.
	 */
	private static enum FullTestItemMode
	{
		NOT_EARLIER_THAN,
		
		NO_FAIL_SUCCESS,
		FAIL_1_SUCCESS,
		FAIL_ALWAYS,
		FAIL_TIMEOUT,
		
		TIMEOUT_WITHOUT_ATTEMPTS,
	}
	
	
	/**
	 * Input class used for full test.
	 */
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
	
	
	/**
	 * Tests for the case when shutdown still respects delays and stuff doesn't 
	 * spool out because of the initial delays required.
	 * 
	 * @throws ExecutionException 
	 * @throws TimeoutException 
	 */
	@Test
	public void shutdownTestDelayRespected1() throws InterruptedException, TimeoutException, ExecutionException
	{
		final LinkedBlockingQueue<AttemptRecord<String, String>> attempts = new LinkedBlockingQueue<>();
		final LinkedBlockingQueue<EventListenerEvent> events = new LinkedBlockingQueue<>();
		
		final AtomicInteger failUntilAttempt = new AtomicInteger(0);
		
		final FlatConfiguration baseConfig = Configuration.fromPropertiesFile("retry/shutdownTest");
		
		RRLConfig config = new RRLConfig(baseConfig);
		RetryAndRateLimitService<String, String> service = createBasicService(
			config, 0, failUntilAttempt, attempts, events);
		{
			RRLStatus status = service.getStatus(0);
			assertEquals(status.getServiceControlStateDescription(), "NOT_STARTED");
			assertFalse(status.isAcceptingRequests());
			assertFalse(status.isEverythingAlive());
			assertFalse(status.isDelayQueueProcessingThreadsAreAlive());
			assertFalse(status.isMainQueueProcessingThreadAlive());
			assertTrue(status.isRequestsExecutorServiceAlive());
		}
		
		assertFailsWithSubstring(() -> service.submitFor("r", 123), "Service has not been started yet");
		service.start();
		assertFailsWithSubstring(() -> service.start(), "Unable to start service which is not in NON_STARTED state");
		{
			RRLStatus status = service.getStatus(0);
			assertEquals(status.getServiceControlStateDescription(), "RUNNING");
			assertTrue(status.isAcceptingRequests());
			assertTrue(status.isEverythingAlive());
			assertTrue(status.isDelayQueueProcessingThreadsAreAlive());
			assertTrue(status.isMainQueueProcessingThreadAlive());
			assertTrue(status.isRequestsExecutorServiceAlive());
		}
		
		RRLFuture<String, String> f1 = service.submitForWithDelayFor("1", 10000, 1000);
		RRLFuture<String, String> f2 = service.submitForWithDelayFor("2", 10000, 1000);
		
		AsyncTestRunner<Integer> sf = TestUtil.callAsynchronously(() -> service.shutdownFor(500, false, false));
		Thread.sleep(100); // async shutdown must initiate
		{
			RRLStatus status = service.getStatus(0);
			assertEquals(status.getServiceControlStateDescription(), "SHUTDOWN_IN_PROGRESS");
			assertFalse(status.isAcceptingRequests());
			assertTrue(status.isEverythingAlive());
			assertTrue(status.isDelayQueueProcessingThreadsAreAlive());
			assertTrue(status.isMainQueueProcessingThreadAlive());
			assertTrue(status.isRequestsExecutorServiceAlive());
		}

		assertFailsWithSubstring(() -> service.submitFor("r", 123), "Service is being shut down");
		assertFailsWithSubstring(() -> service.start(), "Unable to start service which is not in NON_STARTED state");
		
		assertEquals((int)sf.getResult(700), 2);
		
		assertNull(f1.getOrNull(0));
		assertFailsWithSubstring(() -> f2.get(0), "java.util.concurrent.TimeoutException");
		
		assertFailsWithSubstring(() -> service.submitFor("r", 123), "Service has been shut down");
		assertFailsWithSubstring(() -> service.start(), "Unable to start service which is not in NON_STARTED state");
		
		{
			RRLStatus status = service.getStatus(0);
			assertEquals(status.getServiceControlStateDescription(), "SHUTDOWN");
			assertFalse(status.isAcceptingRequests());
			assertFalse(status.isEverythingAlive());
			assertFalse(status.isDelayQueueProcessingThreadsAreAlive());
			assertFalse(status.isMainQueueProcessingThreadAlive());
			assertFalse(status.isRequestsExecutorServiceAlive());
		}
	}
	
	/**
	 * Tests for the case when shutdown still respects delays and stuff doesn't 
	 * spool out because of the retry delays required.
	 * 
	 * @throws ExecutionException 
	 * @throws TimeoutException 
	 */
	@Test
	public void shutdownTestDelayRespected2() throws InterruptedException
	{
		final LinkedBlockingQueue<AttemptRecord<String, String>> attempts = new LinkedBlockingQueue<>();
		final LinkedBlockingQueue<EventListenerEvent> events = new LinkedBlockingQueue<>();
		
		final AtomicInteger failUntilAttempt = new AtomicInteger(2);
		
		final FlatConfiguration baseConfig = Configuration.fromPropertiesFile("retry/shutdownTest");
		
		RRLConfig config = new RRLConfig(baseConfig);
		RetryAndRateLimitService<String, String> service = createBasicService(
			config, 0, failUntilAttempt, attempts, events);
		service.start();
		
		RRLFuture<String, String> f1 = service.submitForWithDelayFor("1", 10000, 1000);
		RRLFuture<String, String> f2 = service.submitForWithDelayFor("2", 10000, 1000);
		Thread.sleep(50); // let initial failures happen
		
		long startShutdown = System.currentTimeMillis();
		assertEquals(service.shutdownFor(500, false, false), 2);
		assertBetweenInclusive(System.currentTimeMillis() - startShutdown, 400L, 600L);
		
		assertNull(f1.getOrNull(0));
		assertFailsWithSubstring(() -> f2.get(0), "TimeoutException");
	}
	
	/**
	 * Tests for the case when shutdown doesn't respects delays.
	 * 
	 * @throws ExecutionException 
	 * @throws TimeoutException 
	 */
	@Test
	public void shutdownTestNoDelaySuccess() throws InterruptedException
	{
		final LinkedBlockingQueue<AttemptRecord<String, String>> attempts = new LinkedBlockingQueue<>();
		final LinkedBlockingQueue<EventListenerEvent> events = new LinkedBlockingQueue<>();
		
		final AtomicInteger failUntilAttempt = new AtomicInteger(2);
		
		final FlatConfiguration baseConfig = Configuration.fromPropertiesFile("retry/shutdownTest");
		
		RRLConfig config = new RRLConfig(baseConfig);
		RetryAndRateLimitService<String, String> service = createBasicService(
			config, 0, failUntilAttempt, attempts, events);
		service.start();
		
		RRLFuture<String, String> f1 = service.submitFor("1", 10000);
		RRLFuture<String, String> f2 = service.submitFor("2", 10000);
		Thread.sleep(50); // let initial failures happen
		
		long startShutdown = System.currentTimeMillis();
		assertEquals(service.shutdownFor(500, true, false), 0);
		assertBetweenInclusive(System.currentTimeMillis() - startShutdown, 0L, 150L);
		
		assertEquals(f1.getOrNull(0), "success: 1");
		assertEquals(f2.getOrNull(0), "success: 2");
	}
	
	/**
	 * Tests for the case when shutdown doesn't respects delays but requests
	 * still fail.
	 * 
	 * @throws ExecutionException 
	 * @throws TimeoutException 
	 */
	@Test
	public void shutdownTestNoDelayFailAfterAttempts() throws InterruptedException
	{
		final LinkedBlockingQueue<AttemptRecord<String, String>> attempts = new LinkedBlockingQueue<>();
		final LinkedBlockingQueue<EventListenerEvent> events = new LinkedBlockingQueue<>();
		
		final AtomicInteger failUntilAttempt = new AtomicInteger(3);
		
		final FlatConfiguration baseConfig = Configuration.fromPropertiesFile("retry/shutdownTest");
		
		RRLConfig config = new RRLConfig(baseConfig);
		RetryAndRateLimitService<String, String> service = createBasicService(
			config, 0, failUntilAttempt, attempts, events);
		service.start();
		
		RRLFuture<String, String> f1 = service.submitFor("1", 10000);
		RRLFuture<String, String> f2 = service.submitFor("2", 10000);
		Thread.sleep(50); // let initial failures happen
		
		long startShutdown = System.currentTimeMillis();
		assertEquals(service.shutdownFor(500, true, false), 0);
		assertBetweenInclusive(System.currentTimeMillis() - startShutdown, 0L, 150L);
		
		{
			RRLTimeoutException to = assertFails(() -> f1.getOrNull(0));
			assertBetweenInclusive(to.getTotalProcessingTime(), 50L, 200L);
		}
		{
			RRLTimeoutException to = assertFails(() -> f2.getOrNull(0));
			assertBetweenInclusive(to.getTotalProcessingTime(), 50L, 200L);
		}
	}
	
	/**
	 * Tests for the case when shutdown respects tokens and there's not enough.
	 * 
	 * @throws ExecutionException 
	 * @throws TimeoutException 
	 */
	@Test
	public void shutdownTestOutOfTokens() throws InterruptedException
	{
		final LinkedBlockingQueue<AttemptRecord<String, String>> attempts = new LinkedBlockingQueue<>();
		final LinkedBlockingQueue<EventListenerEvent> events = new LinkedBlockingQueue<>();
		
		final AtomicInteger failUntilAttempt = new AtomicInteger(2);
		
		final FlatConfiguration baseConfig = Configuration.fromPropertiesFile("retry/shutdownTest");
		
		OverrideFlatConfiguration oConfig = new OverrideFlatConfiguration(baseConfig);
		oConfig.override("rateLimiterRefillInterval", "1000s");
		oConfig.override("rateLimiterRefillRate", "1");
		
		RRLConfig config = new RRLConfig(oConfig);
		RetryAndRateLimitService<String, String> service = createBasicService(
			config, 0, failUntilAttempt, attempts, events);
		service.start();
		
		RRLFuture<String, String> f1 = service.submitFor("1", 10000);
		RRLFuture<String, String> f2 = service.submitFor("2", 10000);
		Thread.sleep(50); // let initial failures happen
		
		long startShutdown = System.currentTimeMillis();
		assertEquals(service.shutdownFor(500, true, false), 0);
		assertBetweenInclusive(System.currentTimeMillis() - startShutdown, 400L, 600L);
		
		{
			RRLTimeoutException to = assertFails(() -> f1.getOrNull(0));
			assertBetweenInclusive(to.getTotalProcessingTime(), 200L, 400L);
		}
		{
			RRLTimeoutException to = assertFails(() -> f2.getOrNull(0));
			assertBetweenInclusive(to.getTotalProcessingTime(), 400L, 600L);
		}
	}
	
	/**
	 * Tests for the case when shutdown ignores tokens (even though there are
	 * not enough) but the requests fail, so they are timed-out.
	 * 
	 * @throws ExecutionException 
	 * @throws TimeoutException 
	 */
	@Test
	public void shutdownTestIgnoreTokensFailAfterAttempts() throws InterruptedException
	{
		final LinkedBlockingQueue<AttemptRecord<String, String>> attempts = new LinkedBlockingQueue<>();
		final LinkedBlockingQueue<EventListenerEvent> events = new LinkedBlockingQueue<>();
		
		final AtomicInteger failUntilAttempt = new AtomicInteger(2);
		
		final FlatConfiguration baseConfig = Configuration.fromPropertiesFile("retry/shutdownTest");
		
		OverrideFlatConfiguration oConfig = new OverrideFlatConfiguration(baseConfig);
		oConfig.override("rateLimiterRefillInterval", "1000s");
		oConfig.override("rateLimiterRefillRate", "1");
		
		RRLConfig config = new RRLConfig(oConfig);
		RetryAndRateLimitService<String, String> service = createBasicService(
			config, 0, failUntilAttempt, attempts, events);
		service.start();
		
		RRLFuture<String, String> f1 = service.submitFor("1", 10000);
		RRLFuture<String, String> f2 = service.submitFor("2", 10000);
		Thread.sleep(50); // let initial failures happen
		
		long startShutdown = System.currentTimeMillis();
		assertEquals(service.shutdownFor(500, true, true), 0);
		assertBetweenInclusive(System.currentTimeMillis() - startShutdown, 0L, 100L);
		
		{
			RRLTimeoutException to = assertFails(() -> f1.getOrNull(0));
			assertBetweenInclusive(to.getTotalProcessingTime(), 50L, 200L);
		}
		{
			RRLTimeoutException to = assertFails(() -> f2.getOrNull(0));
			assertBetweenInclusive(to.getTotalProcessingTime(), 50L, 200L);
		}
	}
	
	/**
	 * Tests for the case when shutdown ignores tokens (even though there are
	 * not enough) -- and the requests succeed.
	 * 
	 * @throws ExecutionException 
	 * @throws TimeoutException 
	 */
	@Test
	public void shutdownTestIgnoreTokensSuccess() throws InterruptedException
	{
		final LinkedBlockingQueue<AttemptRecord<String, String>> attempts = new LinkedBlockingQueue<>();
		final LinkedBlockingQueue<EventListenerEvent> events = new LinkedBlockingQueue<>();
		
		final AtomicInteger failUntilAttempt = new AtomicInteger(1);
		
		final FlatConfiguration baseConfig = Configuration.fromPropertiesFile("retry/shutdownTest");
		
		OverrideFlatConfiguration oConfig = new OverrideFlatConfiguration(baseConfig);
		oConfig.override("rateLimiterRefillInterval", "1000s");
		oConfig.override("rateLimiterRefillRate", "1");
		
		RRLConfig config = new RRLConfig(oConfig);
		RetryAndRateLimitService<String, String> service = createBasicService(
			config, 0, failUntilAttempt, attempts, events);
		service.start();
		
		RRLFuture<String, String> f1 = service.submitFor("1", 10000);
		RRLFuture<String, String> f2 = service.submitFor("2", 10000);
		Thread.sleep(50); // let initial failures happen
		
		assertNull(f1.getOrNull(0));
		assertNull(f2.getOrNull(0));
		
		long startShutdown = System.currentTimeMillis();
		assertEquals(service.shutdownFor(500, true, true), 0);
		assertBetweenInclusive(System.currentTimeMillis() - startShutdown, 0L, 100L);
		
		assertEquals(f1.getOrNull(0), "success: 1");
		assertEquals(f2.getOrNull(0), "success: 2");
	}
	
	/**
	 * Tests for the case when shutdown doesn't wait for tokens (only uses 
	 * immediately available) -- and the requests timeout.
	 * 
	 * @throws ExecutionException 
	 * @throws TimeoutException 
	 */
	@Test
	public void shutdownTestDontWaitForTokensTimeouts() throws InterruptedException
	{
		final LinkedBlockingQueue<AttemptRecord<String, String>> attempts = new LinkedBlockingQueue<>();
		final LinkedBlockingQueue<EventListenerEvent> events = new LinkedBlockingQueue<>();
		
		final AtomicInteger failUntilAttempt = new AtomicInteger(1);
		
		final FlatConfiguration baseConfig = Configuration.fromPropertiesFile("retry/shutdownTest");
		
		OverrideFlatConfiguration oConfig = new OverrideFlatConfiguration(baseConfig);
		oConfig.override("rateLimiterRefillInterval", "1000s");
		oConfig.override("rateLimiterRefillRate", "1");
		
		RRLConfig config = new RRLConfig(oConfig);
		RetryAndRateLimitService<String, String> service = createBasicService(
			config, 0, failUntilAttempt, attempts, events);
		service.start();
		
		RRLFuture<String, String> f1 = service.submitFor("1", 10000);
		RRLFuture<String, String> f2 = service.submitFor("2", 10000);
		Thread.sleep(50); // let initial failures happen
		
		assertNull(f1.getOrNull(0));
		assertNull(f2.getOrNull(0));
		
		long startShutdown = System.currentTimeMillis();
		assertEquals(service.shutdownFor(500, 
			RRLControlState.SHUTDOWN_IN_PROGRESS.withWaitForTickets(false)), 0);
		assertBetweenInclusive(System.currentTimeMillis() - startShutdown, 0L, 100L);
		
		{
			RRLTimeoutException to = assertFails(() -> f1.getOrNull(0));
			assertBetweenInclusive(to.getTotalProcessingTime(), 50L, 200L);
		}
		{
			RRLTimeoutException to = assertFails(() -> f2.getOrNull(0));
			assertBetweenInclusive(to.getTotalProcessingTime(), 50L, 200L);
		}
	}
	
	/**
	 * Tests for the case when there's not enough threads available during shutdown.
	 * 
	 * @throws ExecutionException 
	 * @throws TimeoutException 
	 */
	@Test
	public void shutdownTestOutOfThreads() throws InterruptedException
	{
		final LinkedBlockingQueue<AttemptRecord<String, String>> attempts = new LinkedBlockingQueue<>();
		final LinkedBlockingQueue<EventListenerEvent> events = new LinkedBlockingQueue<>();
		
		final AtomicInteger failUntilAttempt = new AtomicInteger(2);
		
		final FlatConfiguration baseConfig = Configuration.fromPropertiesFile("retry/shutdownTest");
		
		OverrideFlatConfiguration oConfig = new OverrideFlatConfiguration(baseConfig);
		oConfig.override("requestProcessingThreadPoolConfig", "1,1");
		
		RRLConfig config = new RRLConfig(oConfig);
		RetryAndRateLimitService<String, String> service = createBasicService(
			config, 999999999/*thread is stuck 'forever'*/, failUntilAttempt, attempts, events);
		service.start();
		
		RRLFuture<String, String> f1 = service.submitFor("1", 10000);
		RRLFuture<String, String> f2 = service.submitFor("2", 10000);
		RRLFuture<String, String> f3 = service.submitFor("3", 10000);
		Thread.sleep(50); // let initial processing happen
		
		long startShutdown = System.currentTimeMillis();
		assertEquals(service.shutdownFor(500, true, false), 1);
		assertBetweenInclusive(System.currentTimeMillis() - startShutdown, 400L, 600L);
		
		// this may fail in different ways depending on specifics, e.g. thread can be interrupted by shutdown
		assertFails(() -> f1.getOrNull(0));
		
		{
			RRLTimeoutException to = assertFails(() -> f2.getOrNull(0));
			assertBetweenInclusive(to.getTotalProcessingTime(), 200L, 400L);
		}
		{
			RRLTimeoutException to = assertFails(() -> f3.getOrNull(0));
			assertBetweenInclusive(to.getTotalProcessingTime(), 400L, 600L);
		}
	}
	
	/**
	 * Tests for the case when shutdown timeouts all pending requests immediately.
	 * 
	 * @throws ExecutionException 
	 * @throws TimeoutException 
	 */
	@Test
	public void shutdownTestTimeoutAllImmediately() throws InterruptedException
	{
		final LinkedBlockingQueue<AttemptRecord<String, String>> attempts = new LinkedBlockingQueue<>();
		final LinkedBlockingQueue<EventListenerEvent> events = new LinkedBlockingQueue<>();
		
		final AtomicInteger failUntilAttempt = new AtomicInteger(2);
		
		final FlatConfiguration baseConfig = Configuration.fromPropertiesFile("retry/shutdownTest");
		
		OverrideFlatConfiguration oConfig = new OverrideFlatConfiguration(baseConfig);
		oConfig.override("rateLimiterRefillInterval", "1000s");
		oConfig.override("rateLimiterRefillRate", "1");
		
		RRLConfig config = new RRLConfig(oConfig);
		RetryAndRateLimitService<String, String> service = createBasicService(
			config, 0, failUntilAttempt, attempts, events);
		service.start();
		
		RRLFuture<String, String> f1 = service.submitFor("1", 10000);
		RRLFuture<String, String> f2 = service.submitFor("2", 10000);
		Thread.sleep(50); // let initial failures happen
		
		long startShutdown = System.currentTimeMillis();
		assertEquals(service.shutdownFor(500, RRLControlStateBuilder
			.description("SHUTDOWN_IN_PROGRESS")
			.rejectRequestsString("Service is being shut down.")
			.ignoreDelays(true)
			.timeoutAllPendingRequests(true) // this is what we test
			.timeoutRequestsAfterFailedAttempt(true)
			.spooldownTargetTimestamp(-1)
			.limitWaitingForProcessingThread(true)
			.limitWaitingForTicket(true)
			.waitForTickets(true)
			.buildRRLControlState()
			), 0);
		assertBetweenInclusive(System.currentTimeMillis() - startShutdown, 0L, 100L);
		
		{
			RRLTimeoutException to = assertFails(() -> f1.getOrNull(0));
			assertBetweenInclusive(to.getTotalProcessingTime(), 50L, 200L);
		}
		{
			RRLTimeoutException to = assertFails(() -> f2.getOrNull(0));
			assertBetweenInclusive(to.getTotalProcessingTime(), 50L, 200L);
		}
	}
	
	
	/**
	 * Tests for all the different flavors of the {@link RRLFuture} access.
	 * @throws ExecutionException 
	 * @throws TimeoutException 
	 */
	@Test
	public void futureTests() throws InterruptedException, TimeoutException, ExecutionException
	{
		final LinkedBlockingQueue<AttemptRecord<String, String>> attempts = new LinkedBlockingQueue<>();
		final LinkedBlockingQueue<EventListenerEvent> events = new LinkedBlockingQueue<>();
		
		final AtomicInteger failUntilAttempt = new AtomicInteger(0);
		
		final FlatConfiguration baseConfig = Configuration.fromPropertiesFile("retry/futureTest");
		
		{
			// Test 'wait for result' timeouts (request hasn't completed yet by the time wait time expired)
			failUntilAttempt.set(0);
			OverrideFlatConfiguration oConfig = new OverrideFlatConfiguration(baseConfig);
			
			RRLConfig config = new RRLConfig(oConfig);
			RetryAndRateLimitService<String, String> service = createBasicService(
				config, 999999999/*forever*/, failUntilAttempt, attempts, events);
			service.start();
			
			RRLFuture<String, String> f1 = service.submitFor("1", 10000);
			
			AsyncTestRunner<String> a1 = TestUtil.callAsynchronously(() -> f1.get());
			AsyncTestRunner<String> a2 = TestUtil.callAsynchronously(() -> f1.get(150));
			AsyncTestRunner<String> a3 = TestUtil.callAsynchronously(() -> f1.get(150, TimeUnit.MILLISECONDS));
			AsyncTestRunner<@Nullable String> a4 = TestUtil.callAsynchronously(() -> f1.getOrNull(150));
			AsyncTestRunner<@Nullable String> a5 = TestUtil.callAsynchronously(() -> f1.getOrNull(150, TimeUnit.MILLISECONDS));
			
			Thread.sleep(250); // longer that async timeouts
			
			assertFailsWithSubstring(() -> a1.getResult(0, false), "Asyncronous execution didn't finish");
			{
				ExecutionException e = assertFails(() -> a2.getResult(0));
				assertTrue(e.getCause() instanceof TimeoutException, e.toString()); 
			}
			{
				ExecutionException e = assertFails(() -> a3.getResult(0));
				assertTrue(e.getCause() instanceof TimeoutException, e.toString()); 
			}
			assertNull(a4.getResult(0));
			assertNull(a5.getResult(0));
			
			assertFalse(f1.isCancelled());
			assertFalse(f1.isDone());
			assertFalse(f1.isSuccessful());
			
			service.shutdownFor(50, true, true);
		}
		
		{
			// Test 'processing successful' stuff.
			failUntilAttempt.set(0);
			OverrideFlatConfiguration oConfig = new OverrideFlatConfiguration(baseConfig);
			
			RRLConfig config = new RRLConfig(oConfig);
			RetryAndRateLimitService<String, String> service = createBasicService(
				config, 0, failUntilAttempt, attempts, events);
			service.start();
			
			{
				RRLFuture<String, String> f1 = service.submitFor("1", 10000);
			
				final long start = System.currentTimeMillis();
				assertEquals(f1.get(), "success: 1");
				assertBetweenInclusive(System.currentTimeMillis() - start, 0L, 150L);
				assertFalse(f1.isCancelled());
				assertTrue(f1.isDone());
				assertTrue(f1.isSuccessful());
			}
			{
				RRLFuture<String, String> f1 = service.submitFor("1", 10000);
				assertEquals(f1.get(150), "success: 1");
				assertFalse(f1.isCancelled());
				assertTrue(f1.isDone());
				assertTrue(f1.isSuccessful());
			}
			{
				RRLFuture<String, String> f1 = service.submitFor("1", 10000);
				assertEquals(f1.get(150, TimeUnit.MILLISECONDS), "success: 1");
				assertFalse(f1.isCancelled());
				assertTrue(f1.isDone());
				assertTrue(f1.isSuccessful());
			}
			{
				RRLFuture<String, String> f1 = service.submitFor("1", 10000);
				assertEquals(f1.getOrNull(150), "success: 1");
				assertFalse(f1.isCancelled());
				assertTrue(f1.isDone());
				assertTrue(f1.isSuccessful());
			}
			{
				RRLFuture<String, String> f1 = service.submitFor("1", 10000);
				assertEquals(f1.getOrNull(150, TimeUnit.MILLISECONDS), "success: 1");
				assertFalse(f1.isCancelled());
				assertTrue(f1.isDone());
				assertTrue(f1.isSuccessful());
			}
			
			service.shutdownFor(50, true, true);
		}
		
		{
			// Test 'processing failed' stuff.
			failUntilAttempt.set(10);
			OverrideFlatConfiguration oConfig = new OverrideFlatConfiguration(baseConfig);
			
			RRLConfig config = new RRLConfig(oConfig);
			RetryAndRateLimitService<String, String> service = createBasicService(
				config, 0, failUntilAttempt, attempts, events);
			service.start();
			
			Consumer<ConsumerWithException<RRLFuture<String, String>>> executor = new Consumer<ConsumerWithException<RRLFuture<String, String>>>()
			{
				@Override
				public void accept(ConsumerWithException<RRLFuture<String, String>> f)
				{
					RRLFuture<String, String> f1 = service.submitFor("1", 10000);
					assertFailsWithSubstring(() -> f.accept(f1), "ExecutionRuntimeException: java.lang.IllegalStateException: attempt: 1");
					assertFalse(f1.isCancelled());
					assertTrue(f1.isDone());
					assertFalse(f1.isSuccessful());
				}
			};
			
			{
				final long start = System.currentTimeMillis();
				executor.accept(f -> f.get());
				assertBetweenInclusive(System.currentTimeMillis() - start, 0L, 150L);
			}
			executor.accept(f -> f.get(150));
			executor.accept(f -> f.get(150, TimeUnit.MILLISECONDS));
			executor.accept(f -> f.getOrNull(150));
			executor.accept(f -> f.getOrNull(150, TimeUnit.MILLISECONDS));
			
			service.shutdownFor(50, true, true);
		}
		
		{
			// Test underlying request timing out (expired w/o success)
			failUntilAttempt.set(10);
			OverrideFlatConfiguration oConfig = new OverrideFlatConfiguration(baseConfig);
			oConfig.override("maxAttempts", "10");
			
			RRLConfig config = new RRLConfig(oConfig);
			RetryAndRateLimitService<String, String> service = createBasicService(
				config, 0, failUntilAttempt, attempts, events);
			service.start();
			
			RRLFuture<String, String> f1 = service.submitFor("1", 50/*very short validity*/);
			
			Consumer<ConsumerWithException<RRLFuture<String, String>>> executor = new Consumer<ConsumerWithException<RRLFuture<String, String>>>()
			{
				@Override
				public void accept(ConsumerWithException<RRLFuture<String, String>> f)
				{
					RRLTimeoutException e = assertFails(() -> f.accept(f1));
					assertBetweenInclusive(e.getTotalProcessingTime(), 50L, 125L);
					assertFalse(f1.isCancelled());
					assertTrue(f1.isDone());
					assertFalse(f1.isSuccessful());
				}
			};
			
			{
				final long start = System.currentTimeMillis();
				executor.accept(f -> f.get());
				assertBetweenInclusive(System.currentTimeMillis() - start, 0L, 150L);
			}
			executor.accept(f -> f.get(150));
			executor.accept(f -> f.get(150, TimeUnit.MILLISECONDS));
			executor.accept(f -> f.getOrNull(150));
			executor.accept(f -> f.getOrNull(150, TimeUnit.MILLISECONDS));
			
			service.shutdownFor(50, true, true);
		}
		
		{
			// Test underlying request cancellation
			failUntilAttempt.set(10);
			OverrideFlatConfiguration oConfig = new OverrideFlatConfiguration(baseConfig);
			oConfig.override("maxAttempts", "10");
			oConfig.override("delaysAfterFailure", "25ms");
			
			RRLConfig config = new RRLConfig(oConfig);
			RetryAndRateLimitService<String, String> service = createBasicService(
				config, 0, failUntilAttempt, attempts, events);
			service.start();
			
			
			Consumer<ConsumerWithException<RRLFuture<String, String>>> executor = new Consumer<ConsumerWithException<RRLFuture<String, String>>>()
			{
				@Override
				public void accept(ConsumerWithException<RRLFuture<String, String>> f)
				{
					final long start = System.currentTimeMillis();
					
					RRLFuture<String, String> f1 = service.submitFor("1", 10000);
					try
					{
						Thread.sleep(25);
					} catch( InterruptedException e1 )
					{
						fail();
					}
					f1.requestCancellation();
					
					CancellationException e = assertFails(() -> f.accept(f1));
					assertContains(e.toString(), "java.util.concurrent.CancellationException");
					assertTrue(f1.isCancelled());
					assertTrue(f1.isDone());
					assertFalse(f1.isSuccessful());
					
					assertBetweenInclusive(System.currentTimeMillis() - start, 25L, 150L);
				}
			};
			
			executor.accept(f -> f.get());
			executor.accept(f -> f.get(150));
			executor.accept(f -> f.get(150, TimeUnit.MILLISECONDS));
			executor.accept(f -> f.getOrNull(150));
			executor.accept(f -> f.getOrNull(150, TimeUnit.MILLISECONDS));
			
			service.shutdownFor(50, true, true);
		}
	}
	
}
