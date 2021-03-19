/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

import javax.annotation.Nonnull;

/**
 * A thread gate, that uses an {@link java.util.concurrent.locks.AbstractQueuedSynchronizer}.
 * <p>
 * This implementation allows you to create a latch with a default state (open or closed), and repeatedly open or close
 * the latch.
 *<p>
 * Based on: https://github.com/redisson/redisson/blob/master/redisson/src/main/java/org/redisson/misc/ReclosableLatch.java
 * and {@link CountDownLatch}
 */
public class Latch
{
	/**
	 * Sync.
	 */
	private final Sync sync;
	
	/**
	 * Actual latch control (inner class to hide unnecessary public methods).
	 */
	private static final class Sync extends AbstractQueuedSynchronizer
	{
		private static final long serialVersionUID = 1744280161777661777l;

		// the following states are used in the AQS.
		private static final int OPEN_STATE = 0, CLOSED_STATE = 1;
		
		/**
		 * Constructor.
		 */
		public Sync(boolean isOpen)
		{
			setState(isOpen ? OPEN_STATE : CLOSED_STATE);
		}

		@Override
		public int tryAcquireShared(int ignored)
		{
			// return 1 if we allow the requestor to proceed, -1 if we want the requestor to block.
			return getState() == OPEN_STATE ? 1 : -1;
		}

		@Override
		public boolean tryReleaseShared(int state)
		{
			// used as a mechanism to set the state of the Sync.
			setState(state);
			return true;
		}
		
		/**
		 * Whether this latch is currently opened
		 */
		public boolean isOpen()
		{
			return getState() == Sync.OPEN_STATE;
		}
		

		public void open()
		{
			// do not use setState() directly since this won't notify parked threads.
			releaseShared(Sync.OPEN_STATE);
		}

		public void close()
		{
			// do not use setState() directly since this won't notify parked threads.
			releaseShared(Sync.CLOSED_STATE);
		}

		// waiting for an open state
		public void await()
			throws InterruptedException
		{
			acquireSharedInterruptibly(1); // the 1 is a dummy value that is not used.
		}

		public boolean await(long time, TimeUnit unit) throws InterruptedException
		{
			return tryAcquireSharedNanos(1, unit.toNanos(time)); // the 1 is a dummy value that is not used.
		}
	}

	/**
	 * Constructor.
	 * 
	 * @param isOpen whether latch is initially opened
	 */
	public Latch(boolean isOpen)
	{
		this.sync = new Sync(isOpen);
	}

	/**
	 * Opens the latch.
	 */
	public void open()
	{
		sync.open();
	}

	/**
	 * Closes the latch.
	 */
	public void close()
	{
		sync.close();
	}

	/**
	 * Whether this latch is currently opened
	 */
	public boolean isOpen()
	{
		return sync.isOpen();
	}

    /**
     * Causes the current thread to wait until the latch is opened.
     *
     * <p>If latch is already opened, then this method returns immediately.
     *
     * <p>If latch is closed, then the current
     * thread becomes disabled for thread scheduling purposes and lies
     * dormant until one of two things happen:
     * <ul>
     * <li>The latch is opened; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
	public void await() throws InterruptedException
	{
		sync.await();
	}


    /**
     * Causes the current thread to wait until the latch is opened,
     * unless the thread is {@linkplain Thread#interrupt interrupted},
     * or the specified waiting time elapses.
     *
     * <p>If latch is already opened, then this method returns immediately
     * with the value {@code true}.
     *
     * <p>If latch is closed, then the current
     * thread becomes disabled for thread scheduling purposes and lies
     * dormant until one of three things happen:
     * <ul>
     * <li>The latch is opened; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>The specified waiting time elapses.
     * </ul>
     *
     * <p>If the latch is opened, then the method returns with the
     * value {@code true}.
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if latch is opened and {@code false}
     *         if the waiting time elapsed before the latch is opened
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
	public boolean await(long time, TimeUnit unit) throws InterruptedException
	{
		return sync.await(time, unit); // the 1 is a dummy value that is not used.
	}
	
	/**
	 * A version of {@link #await()} that doesn't throw {@link InterruptedException}
	 * <p>
	 * Instead it will return false if interrupted. Thread's interrupted flag
	 * is set in case of interrupt.
	 * 
	 * @see #await()
	 * 
	 * @return true if latch is open, false if interrupted
	 */
	public boolean awaitNoException()
	{
		try
		{
			await();
			return true;
		} catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			return false;
		}
	}
	
	/**
	 * A version of {@link #await(long, TimeUnit)} that doesn't throw
	 * {@link InterruptedException}
	 * <p>
	 * Instead it will return false if interrupted (same as when waiting period
	 * has expired). Thread's interrupted flag is set in case of interrupt.
	 * 
	 * @see #await(long, TimeUnit)
	 * 
	 * @return true if latch is open, false if waiting period expired or thread
	 * 		is interrupted
	 */
	public boolean awaitNoException(long time, TimeUnit unit)
	{
		try
		{
			return await(time, unit);
		} catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			return false;
		}
	}
	
	
	/**
	 * Causes the current thread to wait until latch is opened.
	 * 
	 * <p>Any interrupts are ignored; however if there's been any interrupts
	 * (or thread was already in interrupted state), then the Thread's interrupt
	 * flag will be set when returning.
	 * 
	 * @see #await()
	 * 
	 * @deprecated on the reflection, this method seems a very bad idea as interruption
	 * 		is the only way to terminate 'stray' threads
	 */
	@Deprecated
	protected void awaitUninterruptibly()
	{
		boolean interrupted = false;
		while(true)
		{
			try
			{
				await();
				if (interrupted)
					Thread.currentThread().interrupt();
				return;
			} catch( InterruptedException e )
			{
				interrupted = true;
				Thread.interrupted(); // reset interrupt flag
			}
		}
	}

	@Override
	public @Nonnull String toString()
	{
		String q = sync.hasQueuedThreads() ? "non" : "";
		return getClass().getSimpleName() + '@' + Integer.toHexString(hashCode()) + "[State=" + (sync.isOpen() ? "open" : "closed" ) + ", " + q + "empty queue]";
	}
}
