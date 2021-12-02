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
package io.github.solf.extra2.thread;

import static io.github.solf.extra2.testutil.AssertExtra.assertContains;
import static io.github.solf.extra2.testutil.AssertExtra.assertFailsWithSubstring;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.testng.annotations.Test;

/**
 * Tests for {@link InterruptHandlingExitableThread}
 * 
 * @author Sergey Olefir
 */
@NonNullByDefault
public class TestInterruptHandlingExitableThread
{
	/**
	 * Tests for handling unexpected {@link InterruptedException}
	 */
	@Test
	public void testInterruptExceptionHandling()
	{
		final AtomicInteger executed = new AtomicInteger(0);
		final AtomicInteger iHandler = new AtomicInteger(0);
		
		InterruptHandlingExitableThread thread = new InterruptHandlingExitableThread()
		{
			final AtomicInteger counter = new AtomicInteger(0);
			
			@Override
			protected void run1(boolean reentry)
				throws InterruptedException
			{
				executed.incrementAndGet();
				
				int cnt = counter.incrementAndGet();
				
				assertEquals(reentry, (cnt > 1));
				
				throw new InterruptedException("intended InterruptedException");
			}
			
			@Override
			protected boolean handleUnexpectedInterruptedException(
				InterruptedException e)
			{
				iHandler.incrementAndGet();
				
				int cnt = counter.get();
				
				assertContains(e, "intended InterruptedException");
				
				return cnt < 3;
			}
		};
		
		thread.run(); // should exit normally
		
		assertEquals(executed.get(), 3);
		assertEquals(iHandler.get(), 3);
		assertEquals(thread.getUnexpectedInterruptedExceptionsCount(), 3);
		assertEquals(thread.getRuntimeExceptionsCount(), 0);
	}
	
	/**
	 * Tests for proper thread exit.
	 */
	@Test
	public void testThreadExit() throws InterruptedException
	{
		final AtomicInteger executed = new AtomicInteger(0);
		final AtomicInteger failures = new AtomicInteger(0);
		
		InterruptHandlingExitableThread thread = new InterruptHandlingExitableThread()
		{
			@Override
			protected void run1(boolean reentry)
				throws InterruptedException
			{
				executed.incrementAndGet();
				
				while (true)
				{
					Thread.sleep(100);
				}
			}
			
			@Override
			protected boolean handleUnexpectedInterruptedException(
				InterruptedException e)
			{
				failures.incrementAndGet();
				
				return false;
			}
		};
		
		thread.start(); // should run normally
		Thread.sleep(100);
		
		assertEquals(executed.get(), 1);
		assertEquals(failures.get(), 0);
		assertTrue(thread.isAlive());
		
		thread.exitAsap();
		Thread.sleep(100);
		
		assertEquals(executed.get(), 1);
		assertEquals(failures.get(), 0);
		assertFalse(thread.isAlive());
		assertEquals(thread.getUnexpectedInterruptedExceptionsCount(), 0);
		assertEquals(thread.getRuntimeExceptionsCount(), 0);
	}
	
	
	/**
	 * Tests for exception thrown by {@link InterruptedException} handler.
	 */
	@Test
	public void testInterruptHandlerException()
	{
		final AtomicInteger executed = new AtomicInteger(0);
		final AtomicInteger iHandler = new AtomicInteger(0);
		
		InterruptHandlingExitableThread thread = new InterruptHandlingExitableThread()
		{
			@Override
			protected void run1(boolean reentry)
				throws InterruptedException
			{
				executed.incrementAndGet();
				
				throw new InterruptedException("intended InterruptedException");
			}
			
			@Override
			protected boolean handleUnexpectedInterruptedException(
				InterruptedException e)
			{
				iHandler.incrementAndGet();
				
				assertContains(e, "intended InterruptedException");
				
				throw new IllegalStateException("iHandler fail");
			}
		};
		
		assertFailsWithSubstring(() -> thread.run(), "iHandler fail");
		
		assertEquals(executed.get(), 1);
		assertEquals(iHandler.get(), 1);
		assertEquals(thread.getUnexpectedInterruptedExceptionsCount(), 1);
		assertEquals(thread.getRuntimeExceptionsCount(), 0);
	}
	
	/**
	 * Tests for handling thrown {@link RuntimeException}
	 */
	@Test
	public void testRuntimeExceptionHandling()
	{
		final AtomicInteger executed = new AtomicInteger(0);
		final AtomicInteger iHandler = new AtomicInteger(0);
		final AtomicInteger reHandler = new AtomicInteger(0);
		
		InterruptHandlingExitableThread thread = new InterruptHandlingExitableThread()
		{
			final AtomicInteger counter = new AtomicInteger(0);
			
			@Override
			protected void run1(boolean reentry)
				throws InterruptedException
			{
				executed.incrementAndGet();
				
				int cnt = counter.incrementAndGet();
				
				assertEquals(reentry, (cnt > 1));
				
				throw new IllegalStateException("intended IllegalStateException");
			}
			
			@Override
			protected boolean handleUnexpectedInterruptedException(
				InterruptedException e)
			{
				iHandler.incrementAndGet();
				
				return true;
			}

			@Override
			protected boolean handleRuntimeException(RuntimeException e)
			{
				reHandler.incrementAndGet();
				
				int cnt = counter.get();
				
				assertContains(e, "intended IllegalStateException");
				
				return cnt < 3;
			}
		};
		
		thread.run(); // should exit normally
		
		assertEquals(executed.get(), 3);
		assertEquals(iHandler.get(), 0);
		assertEquals(reHandler.get(), 3);
		assertEquals(thread.getUnexpectedInterruptedExceptionsCount(), 0);
		assertEquals(thread.getRuntimeExceptionsCount(), 3);
	}
	
	
	/**
	 * Tests for exception thrown {@link RuntimeException} handler.
	 */
	@Test
	public void testRuntimeExceptionHandlingException()
	{
		final AtomicInteger executed = new AtomicInteger(0);
		final AtomicInteger iHandler = new AtomicInteger(0);
		final AtomicInteger reHandler = new AtomicInteger(0);
		
		InterruptHandlingExitableThread thread = new InterruptHandlingExitableThread()
		{
			@Override
			protected void run1(boolean reentry)
				throws InterruptedException
			{
				executed.incrementAndGet();
				
				throw new IllegalStateException("intended IllegalStateException");
			}
			
			@Override
			protected boolean handleUnexpectedInterruptedException(
				InterruptedException e)
			{
				iHandler.incrementAndGet();
				
				return true;
			}

			@Override
			protected boolean handleRuntimeException(RuntimeException e)
			{
				reHandler.incrementAndGet();
				
				assertContains(e, "intended IllegalStateException");
				
				throw new IllegalArgumentException("reHandler fail");
			}
		};
		
		assertFailsWithSubstring(() -> thread.run(), "reHandler fail");
		
		assertEquals(executed.get(), 1);
		assertEquals(iHandler.get(), 0);
		assertEquals(reHandler.get(), 1);
		assertEquals(thread.getUnexpectedInterruptedExceptionsCount(), 0);
		assertEquals(thread.getRuntimeExceptionsCount(), 1);
	}
}
