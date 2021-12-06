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
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.joda.time.LocalDateTime;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.github.solf.extra2.concurrent.retry.RRLConfig;
import io.github.solf.extra2.concurrent.retry.RRLEventListener;
import io.github.solf.extra2.concurrent.retry.RRLFuture;
import io.github.solf.extra2.concurrent.retry.RRLStatus;
import io.github.solf.extra2.concurrent.retry.RetryAndRateLimitService;
import io.github.solf.extra2.config.Configuration;
import io.github.solf.extra2.lambda.TriConsumer;
import io.github.solf.extra2.util.TypeUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tests for {@link RetryAndRateLimitService}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class TestRRL
{
	@BeforeClass
	public void beforeClass() throws InterruptedException
	{
		// Do a warm-up so that timings are more stable.
		
		LinkedBlockingQueue<String> dump = new LinkedBlockingQueue<>();
		
		RRLConfig config = new RRLConfig(Configuration.fromPropertiesFile("retry/simpleOneItemTest"));
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
	public void simpleOneItemTest() throws InterruptedException
	{
		final LinkedBlockingQueue<AttemptRecord<String, String>> attempts = new LinkedBlockingQueue<>();
		final LinkedBlockingQueue<EventListenerEvent> events = new LinkedBlockingQueue<>();
		
		final AtomicInteger failUntilAttempt = new AtomicInteger(3);
		
		RRLConfig config = new RRLConfig(Configuration.fromPropertiesFile("retry/simpleOneItemTest"));
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
			
			final long start = System.currentTimeMillis();
			
			RRLFuture<String, String> future = service.submitFor("request", 5000);
			
			assertEquals(future.getOrNull(2000, TimeUnit.MILLISECONDS), "success: request");
			
			checkAttempt(attempts.poll(), 1, "request", null, start, start + 100);
			checkAttempt(attempts.poll(), 2, "request", null, start + 120, start + 220);
			checkAttempt(attempts.poll(), 3, "request", "success: request", start + 940, start + 1140);
			
	//zzz		checkEvent(events.poll(), "requestAdded"); need to do checkEvent tests
			
			
			{
				// check status
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
		
		
		//zzz tests for 'not earlier than'
		//zzz tests for timeout
		//zzz tests for all attempts fail
		//zzz test grace
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
