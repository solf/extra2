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
package io.github.solf.extra2.concurrent;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.testng.annotations.Test;

import io.github.solf.extra2.concurrent.Latch;

/**
 * Test(s) for {@link Latch}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class ExtraLatchTest
{
	/**
	 * How long to wait for async stuff to happen
	 */
	private static final long DEFAULT_ASYNC_WAIT = 250;
	
	/**
	 * Thread for testing latches. 
	 */
	private static class TestLatchThread extends Thread
	{
		/**
		 * Latch.
		 */
		public final Latch latch;
		
		/**
		 * Whether thread should self-interrupt before accessing latch.
		 */
		public final boolean selfInterrupt;
		
		/**
		 * Whether thread should wait using awaitUninterruptibly
		 */
		public final boolean awaitUninterruptibly;
		
		/**
		 * How long to await the latch in millis (if zero, then indefinitely)
		 */
		public final long awaitTime;
		
		/**
		 * Whether to use 'noException' methods to await
		 */
		public final boolean noException;
		
		/**
		 * Whether thread has finished without exception.
		 */
		public volatile boolean finishedWithoutException = false;
		
		/**
		 * Whether thread got interrupt exception
		 */
		public volatile boolean gotInterruptException = false;
		
		/**
		 * Whether interrupt flag was set after Latch await
		 */
		public volatile boolean interruptFlagWasSet = false;
		
		/**
		 * Result of await -- in case of timed or 'noException' 
		 * (null if no result, true/false otherwise).
		 */
		@Nullable
		public volatile Boolean awaitResult = null;
		
		/**
		 * Constructor.
		 * (no self-interrupt)
		 */
		public TestLatchThread(Latch latch)
		{
			this(latch, false);
		}
		
		/**
		 * Constructor.
		 */
		public TestLatchThread(Latch latch, boolean selfInterrupt)
		{
			this(latch, selfInterrupt, false);
		}
		
		/**
		 * Constructor.
		 */
		public TestLatchThread(Latch latch, boolean selfInterrupt, boolean awaitUninterruptibly)
		{
			this.latch = latch;
			this.selfInterrupt = selfInterrupt;
			this.awaitUninterruptibly = awaitUninterruptibly;
			this.awaitTime = 0;
			this.noException = false;
			
			setDaemon(true);
		}
		
		/**
		 * Constructor.
		 */
		public TestLatchThread(Latch latch, boolean selfInterrupt, long awaitTime)
		{
			this(latch, selfInterrupt, awaitTime, false);
		}
		
		/**
		 * Constructor.
		 */
		public TestLatchThread(Latch latch, boolean selfInterrupt, long awaitTime, boolean noException)
		{
			this.latch = latch;
			this.selfInterrupt = selfInterrupt;
			this.awaitUninterruptibly = false;
			this.awaitTime = awaitTime;
			this.noException = noException;
			
			setDaemon(true);
		}

		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@SuppressWarnings("deprecation")
		@Override
		public void run()
		{
			try
			{
				if (selfInterrupt)
					Thread.currentThread().interrupt();
				
				if (awaitUninterruptibly)
					latch.awaitUninterruptibly();
				else
				{
					if (awaitTime == 0)
					{
						if (noException)
							awaitResult = latch.awaitNoException();
						else
							latch.await();
					}
					else
					{
						if (noException)
							awaitResult = latch.awaitNoException(awaitTime, TimeUnit.MILLISECONDS);
						else
							awaitResult = latch.await(awaitTime, TimeUnit.MILLISECONDS);
					}
				}
				finishedWithoutException = true;
				
				if (Thread.interrupted())
					interruptFlagWasSet = true;
			} catch (InterruptedException e)
			{
				gotInterruptException = true;
			}
		}
	}
	
	/**
	 * Tests open/close functionality.
	 */
	@Test
	public void testOpenClose() throws InterruptedException
	{
		{
			Latch latch = new Latch(false);
			assert !latch.isOpen();
			System.out.println(latch);
		}
		
		{
			Latch latch = new Latch(true);
			assert latch.isOpen();
			System.out.println(latch);
		}
		
		{
			Latch latch = new Latch(true);
			TestLatchThread t = new TestLatchThread(latch);
			t.start();
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert t.finishedWithoutException;
		}
		
		{
			Latch latch = new Latch(true);
			ArrayList<TestLatchThread> ts = new ArrayList<>();
			for (int i = 0; i < 50; i++)
			{
				TestLatchThread t = new TestLatchThread(latch);
				ts.add(t);
				t.start();
			}
				
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			for (@Nonnull TestLatchThread t : ts)
				assert t.finishedWithoutException;
		}
		
		{
			Latch latch = new Latch(false); // closed latch, check that threads don't pass
			TestLatchThread t = new TestLatchThread(latch);
			t.start();
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert !t.finishedWithoutException;
			System.out.println(latch);
			
			latch.open(); // open latch and check that waiting thread 'falls through'
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert t.finishedWithoutException;
			System.out.println(latch);
			
			t = new TestLatchThread(latch);
			t.start(); // check that new thread passes open latch
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert t.finishedWithoutException;
			
			latch.close(); // close latch and test that threads don't pass
			t = new TestLatchThread(latch);
			t.start();
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert !t.finishedWithoutException;
			
			latch.open(); // open latch again and check 'fall through'
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert t.finishedWithoutException;
			
			t = new TestLatchThread(latch);
			t.start(); // check again that threads pass open latch
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert t.finishedWithoutException;
		}
		
		{
			Latch latch = new Latch(false); // closed latch, check that threads don't pass
			ArrayList<TestLatchThread> ts = new ArrayList<>();
			for (int i = 0; i < 50; i++)
			{
				TestLatchThread t = new TestLatchThread(latch);
				ts.add(t);
				t.start();
			}
				
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			for (@Nonnull TestLatchThread t : ts)
				assert !t.finishedWithoutException;
			
			latch.open(); // open latch and check that waiting threads 'fall through'
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			for (@Nonnull TestLatchThread t : ts)
				assert t.finishedWithoutException;
			
			ts.clear();  // check that new threads pass open latch
			for (int i = 0; i < 50; i++)
			{
				TestLatchThread t = new TestLatchThread(latch);
				ts.add(t);
				t.start();
			}
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			for (@Nonnull TestLatchThread t : ts)
				assert t.finishedWithoutException;
			
			latch.close(); // close latch and test that threads don't pass
			ts.clear();
			for (int i = 0; i < 50; i++)
			{
				TestLatchThread t = new TestLatchThread(latch);
				ts.add(t);
				t.start();
			}
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			for (@Nonnull TestLatchThread t : ts)
				assert !t.finishedWithoutException;
			
			latch.open(); // open latch again and check 'fall through'
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			for (@Nonnull TestLatchThread t : ts)
				assert t.finishedWithoutException;
			
			ts.clear(); // check again that threads pass open latch
			for (int i = 0; i < 50; i++)
			{
				TestLatchThread t = new TestLatchThread(latch);
				ts.add(t);
				t.start();
			}
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			for (@Nonnull TestLatchThread t : ts)
				assert t.finishedWithoutException;
		}
	}
	
	/**
	 * Tests (un)interruptability.
	 */
	@Test
	public void testInterruptability() throws InterruptedException
	{
		
		// Interrupt before await test
		{
			Latch latch = new Latch(false);
			TestLatchThread t = new TestLatchThread(latch, true);
			t.start();
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert t.gotInterruptException;
			assert !t.interruptFlagWasSet;
		}

		
		// Interrupt in await test
		{
			Latch latch = new Latch(false);
			TestLatchThread t = new TestLatchThread(latch);
			t.start();
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert !t.gotInterruptException;
			
			t.interrupt();
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert t.gotInterruptException;
			assert !t.interruptFlagWasSet;
		}
		
		
		// Interrupt before await test + awaitUninterruptibly
		{
			Latch latch = new Latch(false);
			TestLatchThread t = new TestLatchThread(latch, true, true);
			t.start();
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert !t.gotInterruptException;
			assert !t.finishedWithoutException;
			
			latch.open();
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert !t.gotInterruptException;
			assert t.finishedWithoutException;
			assert t.interruptFlagWasSet;
		}
		
		
		// Interrupt in await test + awaitUninterruptibly
		{
			Latch latch = new Latch(false);
			TestLatchThread t = new TestLatchThread(latch, false, true);
			t.start();
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert !t.gotInterruptException;
			assert !t.finishedWithoutException;
			
			t.interrupt();
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert !t.gotInterruptException;
			assert !t.finishedWithoutException;
			
			latch.open();
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert !t.gotInterruptException;
			assert t.finishedWithoutException;
			assert t.interruptFlagWasSet;
		}
		
		
		// Interrupt before await test + awaitNoException
		{
			Latch latch = new Latch(false);
			TestLatchThread t = new TestLatchThread(latch, true, 0, true);
			t.start();
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert !t.gotInterruptException;
			assert t.finishedWithoutException;
			assert t.interruptFlagWasSet;
			assert t.awaitResult == false;
		}
		
		
		// Interrupt in await test + awaitNoException
		{
			Latch latch = new Latch(false);
			TestLatchThread t = new TestLatchThread(latch, false, 0, true);
			t.start();
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert !t.gotInterruptException;
			assert !t.finishedWithoutException;
			
			t.interrupt();
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert !t.gotInterruptException;
			assert t.finishedWithoutException;
			assert t.interruptFlagWasSet;
			assert t.awaitResult == false;
		}
		
		// Interrupt before await test + awaitNoException + time limit
		{
			Latch latch = new Latch(false);
			TestLatchThread t = new TestLatchThread(latch, true, 3 * DEFAULT_ASYNC_WAIT, true);
			t.start();
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert !t.gotInterruptException;
			assert t.finishedWithoutException;
			assert t.interruptFlagWasSet;
			assert t.awaitResult == false;
		}
		
		
		// Interrupt in await test + awaitNoException + time limit
		{
			Latch latch = new Latch(false);
			TestLatchThread t = new TestLatchThread(latch, false, 3 * DEFAULT_ASYNC_WAIT, true);
			t.start();
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert !t.gotInterruptException;
			assert !t.finishedWithoutException;
			
			t.interrupt();
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert !t.gotInterruptException;
			assert t.finishedWithoutException;
			assert t.interruptFlagWasSet;
			assert t.awaitResult == false;
		}
		
	}
	
	/**
	 * Test await with time limit
	 */
	@Test
	public void testTimeLimit() throws InterruptedException
	{
		// Open latch
		{
			Latch latch = new Latch(true);
			TestLatchThread t = new TestLatchThread(latch, false, 1);
			t.start();
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert t.finishedWithoutException;
			assert true == t.awaitResult;
		}
		
		// Closed latch
		{
			Latch latch = new Latch(false);
			TestLatchThread t = new TestLatchThread(latch, false, 1);
			t.start();
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert t.finishedWithoutException;
			assert false == t.awaitResult;
		}
		
		// Closed latch with actual waiting
		{
			Latch latch = new Latch(false);
			TestLatchThread t = new TestLatchThread(latch, false, DEFAULT_ASYNC_WAIT * 2);
			t.start();
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert !t.finishedWithoutException;
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert t.finishedWithoutException;
			assert false == t.awaitResult;
		}
		
		// Closed latch that then opens
		{
			Latch latch = new Latch(false);
			TestLatchThread t = new TestLatchThread(latch, false, DEFAULT_ASYNC_WAIT * 3);
			t.start();
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert !t.finishedWithoutException;
			
			latch.open();
			
			Thread.sleep(DEFAULT_ASYNC_WAIT);
			assert t.finishedWithoutException;
			assert true == t.awaitResult;
		}
	}
}
