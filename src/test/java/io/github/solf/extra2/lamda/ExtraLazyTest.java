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
package io.github.solf.extra2.lamda;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.testng.annotations.Test;

import io.github.solf.extra2.lambda.LocalLazy;
import io.github.solf.extra2.lambda.ParameterizedLocalLazy;
import io.github.solf.extra2.lambda.ParameterizedSafeLazy;
import io.github.solf.extra2.lambda.ParameterizedUnsafeLazy;
import io.github.solf.extra2.lambda.SafeLazy;
import io.github.solf.extra2.lambda.UnsafeLazy;

/**
 * Tests for lazy initializers.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class ExtraLazyTest
{
	/**
	 * Test for {@link UnsafeLazy}
	 */
	@Test
	public void testUnsafeLazy()
	{
		final AtomicInteger invocationCount = new AtomicInteger(0);
		
		UnsafeLazy<Integer> lazy = new UnsafeLazy<>(() -> {return invocationCount.incrementAndGet();});
		
		for (int i = 0; i < 10; i++)
		{
			assert lazy.get() == 1 : "Wrong value: " + lazy.get();
		}
		
		assert invocationCount.get() == 1 : invocationCount.get();
	}
	
	/**
	 * Test for {@link LocalLazy}
	 */
	@Test
	public void testLocalLazy()
	{
		final AtomicInteger invocationCount = new AtomicInteger(0);
		
		LocalLazy<Integer> lazy = new LocalLazy<>(() -> {return invocationCount.incrementAndGet();});
		
		for (int i = 0; i < 10; i++)
		{
			assert lazy.get() == 1 : "Wrong value: " + lazy.get();
		}
		
		assert invocationCount.get() == 1 : invocationCount.get();
	}
	
	/**
	 * Test for {@link ParameterizedUnsafeLazy}
	 */
	@Test
	public void testParameterizedUnsafeLazy()
	{
		final AtomicInteger invocationCount = new AtomicInteger(0);
		
		ParameterizedUnsafeLazy<Integer, String> lazy = new ParameterizedUnsafeLazy<>((arg) -> 
			{
				invocationCount.incrementAndGet();
				return "" + arg;
			});
		
		for (int i = 5; i < 15; i++)
		{
			assert "5".equals(lazy.get(i)) : "Wrong value: " + lazy.get(i);
		}
		
		assert invocationCount.get() == 1 : invocationCount.get();
	}
	
	/**
	 * Test for {@link ParameterizedLocalLazy}
	 */
	@Test
	public void testParameterizedLocalLazy()
	{
		final AtomicInteger invocationCount = new AtomicInteger(0);
		
		ParameterizedLocalLazy<Integer, String> lazy = new ParameterizedLocalLazy<>((arg) -> 
			{
				invocationCount.incrementAndGet();
				return "" + arg;
			});
		
		for (int i = 5; i < 15; i++)
		{
			assert "5".equals(lazy.get(i)) : "Wrong value: " + lazy.get(i);
		}
		
		assert invocationCount.get() == 1 : invocationCount.get();
	}
	
	/**
	 * Test for {@link SafeLazy}
	 */
	@Test
	public void testSafeLazy() throws InterruptedException
	{
		final AtomicInteger invocationCount = new AtomicInteger(0);
		final AtomicReference<String> factoryThreadName = new AtomicReference<String>("");
		final LinkedBlockingQueue<String> resultQueue = new LinkedBlockingQueue<>();
		
		final SafeLazy<String> lazy = new SafeLazy<>(() -> {
			invocationCount.incrementAndGet();
			factoryThreadName.set(Thread.currentThread().getName());
			return Thread.currentThread().getName();
		});
		
		final AtomicBoolean mayGo = new AtomicBoolean(false);

		final int limit = 10;
		for (int i = 0; i < limit; i++)
		{
			Thread thread = new Thread(() -> {
				try
				{
					// busy-loop wait for go signal
					while(!mayGo.get()) 
					{
						// Just wait until we may go.
					}
				
					resultQueue.add(lazy.get());
				} catch (Exception e)
				{
					resultQueue.add(e.toString());
				}
			});
			
			thread.setDaemon(true);
			thread.setName("SafeLazyTest-" + i);
			thread.start();
		}
		
		// Wait a bit so all threads wait on latch
		try
		{
			Thread.sleep(500);
		} catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
		
		// Let the threads run!
		mayGo.set(true);
		
		String actualFactoryThreadName = null;
		for (int i = 0; i < limit; i++)
		{
			@Nullable
			String result = resultQueue.poll(1, TimeUnit.SECONDS);
			if (actualFactoryThreadName == null)
				actualFactoryThreadName = factoryThreadName.get();
			else
				assert actualFactoryThreadName.equals(factoryThreadName.get()) : actualFactoryThreadName + " vs " + factoryThreadName.get();
			assert actualFactoryThreadName.equals(result) : actualFactoryThreadName + " vs " + result;
		}		
		
		assert invocationCount.get() == 1 : invocationCount.get();
	}
	
	/**
	 * Test for {@link ParameterizedSafeLazy}
	 */
	@Test
	public void testParameterizedSafeLazy() throws InterruptedException
	{
		final AtomicInteger invocationCount = new AtomicInteger(0);
		final AtomicReference<String> factoryThreadName = new AtomicReference<>("");
		final AtomicReference<Integer> factoryArgument = new AtomicReference<>(-1);
		final LinkedBlockingQueue<String> resultQueue = new LinkedBlockingQueue<>();
		
		final ParameterizedSafeLazy<Integer, String> lazy = new ParameterizedSafeLazy<>((arg) -> {
			invocationCount.incrementAndGet();
			factoryThreadName.set(Thread.currentThread().getName());
			factoryArgument.set(arg);
			return Thread.currentThread().getName();
		});
		
		final AtomicBoolean mayGo = new AtomicBoolean(false);

		final int limit = 10;
		for (int i = 0; i < limit; i++)
		{
			final int counter = i;
			Thread thread = new Thread(() -> {
				try
				{
					// busy-loop wait for go signal
					while(!mayGo.get()) 
					{
						// Just wait until we may go.
					}
				
					resultQueue.add(lazy.get(counter));
				} catch (Exception e)
				{
					resultQueue.add(e.toString());
				}
			});
			
			thread.setDaemon(true);
			thread.setName("SafeLazyTest-" + i);
			thread.start();
		}
		
		// Wait a bit so all threads wait on latch
		try
		{
			Thread.sleep(500);
		} catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
		
		// Let the threads run!
		mayGo.set(true);
		
		String actualFactoryThreadName = null;
		int actualFactoryArgument = -1;
		for (int i = 0; i < limit; i++)
		{
			@Nullable
			String result = resultQueue.poll(1, TimeUnit.SECONDS);
			if (actualFactoryThreadName == null)
			{
				actualFactoryThreadName = factoryThreadName.get();
				actualFactoryArgument = factoryArgument.get();
				
				assert actualFactoryThreadName.endsWith("-" + actualFactoryArgument) : actualFactoryThreadName + " vs " + actualFactoryArgument;
			}
			else
			{
				assert actualFactoryThreadName.equals(factoryThreadName.get()) : actualFactoryThreadName + " vs " + factoryThreadName.get();
				assert actualFactoryArgument == factoryArgument.get() : actualFactoryArgument + " vs " + factoryArgument.get();
			}
			assert actualFactoryThreadName.equals(result) : actualFactoryThreadName + " vs " + result;
		}		
		
		assert invocationCount.get() == 1 : invocationCount.get();
	}
}
