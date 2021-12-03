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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.joda.time.LocalDateTime;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.github.solf.extra2.concurrent.retry.RRLConfig;
import io.github.solf.extra2.concurrent.retry.RRLEventListener;
import io.github.solf.extra2.concurrent.retry.RRLFuture;
import io.github.solf.extra2.concurrent.retry.RetryAndRateLimitService;
import io.github.solf.extra2.config.Configuration;
import io.github.solf.extra2.lambda.TriConsumer;
import io.github.solf.extra2.util.TypeUtil;

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
	
	@Test
	public void simpleOneItemTest() throws InterruptedException
	{
		final LinkedBlockingQueue<Long> processingTimestamps = new LinkedBlockingQueue<>();
		
		RRLConfig config = new RRLConfig(Configuration.fromPropertiesFile("retry/simpleOneItemTest"));
		RetryAndRateLimitService<String, String> service = new RetryAndRateLimitService<String, String>(config)
		{
			@Override
			protected String processRequest(String input, int attemptNumber)
			{
				processingTimestamps.add(System.currentTimeMillis());
				System.out.println("" + new Date() + " " + attemptNumber + ": " + System.currentTimeMillis());//qqq
				
				if (attemptNumber < 3)
					throw new IllegalStateException("attempt: " + attemptNumber);
				
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
						System.out.println("" + new LocalDateTime() + " " + "[" + method.getName() + "]: " + Arrays.toString(methodArgs));//qqq
					});
			}
			
			
		}.start();
		
		final long start = System.currentTimeMillis();
		
		RRLFuture<String, String> future = service.submitFor("request", 5000_000/*qqq*/);
		
		assertEquals(future.getOrNull(2000_000/*qqq*/, TimeUnit.MILLISECONDS), "success");
		
		assertBetweenInclusive(processingTimestamps.poll(), start, start + 100);
		assertBetweenInclusive(processingTimestamps.poll(), start + 100, start + 200);
		assertBetweenInclusive(processingTimestamps.poll(), start + 900, start + 1100);
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
