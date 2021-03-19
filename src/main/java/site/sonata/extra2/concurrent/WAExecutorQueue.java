/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.concurrent;

import static site.sonata.extra2.util.NullUtil.nnChecked;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Modification of {@link LinkedBlockingQueue} that allows addition of elements
 * only if there's some thread already waiting to consume them 
 * (as in {@link SynchronousQueue}), BUT also providing special 'force' method
 * to force additional element into the queue (even if there are no waiting
 * threads).
 * All this hackery is necessary to work around brain-dead design of 
 * {@link ThreadPoolExecutor} which doesn't provide capability of bounded
 * dynamic thread pool.
 * 
 * Specifically:
 * - all methods that add stuff behave as if underlying queue is {@link SynchronousQueue}
 * - EXCEPT for a 'force' method that will feed waiting thread (if available) or
 * 		will add to {@link LinkedBlockingQueue} for consumption later
 * - all 'retrieve' methods behave as you'd expect from {@link LinkedBlockingQueue} --
 * 		immediate return if something is available or optional wait if it's currently empty
 * - all other queue-related methods behave as for {@link LinkedBlockingQueue}
 *
 * @author Sergey Olefir
 */
public class WAExecutorQueue<E> extends LinkedBlockingQueue<E>
{
	/**
	 * Internal queue used for thread parking.
	 */
	private final SynchronousQueue<E> parkQueue = new SynchronousQueue<E>();
	
	/**
	 * Number of waiting threads.
	 */
	private final AtomicInteger waitingThreadsCount = new AtomicInteger(0);
	
	/**
	 * Latch -- whether thread may enter waiting state.
	 * Zero value means entry allowed, positive values mean not.
	 */
	private final AtomicInteger enterWaitingAllowed = new AtomicInteger(0);

	/**
	 * 
	 */
	public WAExecutorQueue()
	{
		super();
	}

	/**
	 * @param c
	 */
	public WAExecutorQueue(Collection<? extends E> c)
	{
		super(c);
	}

	/**
	 * @param capacity
	 */
	public WAExecutorQueue(int capacity)
	{
		super(capacity);
	}
	
	/**
	 * Retrieves first available element -- and waits if not immediately available.
	 * If unit is null and wait flag is set -- waits indefinitely.
	 * 
	 * @return element or null if wait time expired and no element was available
	 */
	@Nullable
	private E retrieveElementWithWait(long timeout, TimeUnit unit) throws InterruptedException
	{
		// Quick path if there's something available immediately.
		E result = super.poll();
		if (result != null)
			return result;
		
		// If we are going to enter waiting, we need to pass the latch and increment number of waiting threads.
		while(true)
		{
			while(enterWaitingAllowed.get() != 0)
			{
				// Wait until latch is open.
				Thread.yield(); // Significantly improves (at least in some cases) queue performance under very high pressure
			}
			waitingThreadsCount.incrementAndGet();
			// Make sure latch wasn't closed concurrently.
			if (enterWaitingAllowed.get() == 0)
				break; // Okay, we may wait now.
			
			// Latch was raised concurrently, so have to attempt re-entry.
			int v = waitingThreadsCount.decrementAndGet();
			assert v >= 0; // assert separately so that works without asserts 
			Thread.yield(); // Significantly improves (at least in some cases) queue performance under very high pressure
		}
		
		try
		{
			// Here we are allowed to wait, but before that check if something was added concurrently...
			result = super.poll();
			if (result != null)
				return result;
			
			// Okay, nothing was added concurrently, we can safely wait now.
			if (unit == null)
				return parkQueue.take();
			else
				return parkQueue.poll(timeout, unit);
		} finally
		{
			// On exit decrement number of waiting threads.
			int v = waitingThreadsCount.decrementAndGet();
			assert v >= 0; // assert separately so that works without asserts
		}
	}
	
	/**
	 * Force-adds element to this queue -- even if there are no waiting threads.
	 * 
	 * @return true if element was added, false if not (if capacity exceeded).
	 */
	public boolean forceAdd(E element)
	{
		// Quick-pass if there are waiting threads already...
		if (parkQueue.offer(element))
			return true;

		// Couldn't stuff element in quickly and easily, go for a hard way...
		int v = enterWaitingAllowed.incrementAndGet(); // Close latch (via increment so concurrent accesses are handled fine).
		assert v > 0; // assert separately so that works without asserts
		try
		{
			// While there are potential waiting threads, try to stuff value to them.
			while(waitingThreadsCount.get() > 0)
			{
				if (parkQueue.offer(element))
					return true; // Handled element over, quit.
			}
			
			// No more waiting threads, so this goes into wait queue..
			return super.offer(element);
		} finally
		{
			int va = enterWaitingAllowed.decrementAndGet();
			assert va >= 0; // assert separately so that works without asserts
		}
	}

	/* (non-Javadoc)
	 * @see java.util.AbstractQueue#add(java.lang.Object)
	 */
	@Override
	public boolean add(E e)
	{
		return parkQueue.add(e);
	}

	/* (non-Javadoc)
	 * @see java.util.AbstractQueue#addAll(java.util.Collection)
	 */
	@Override
	public boolean addAll(@Nonnull Collection<? extends E> c)
	{
		return parkQueue.addAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.LinkedBlockingQueue#offer(java.lang.Object, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public boolean offer(E e, long timeout, TimeUnit unit)
		throws InterruptedException
	{
		throw new UnsupportedOperationException("The semantics for offer with delay are too weird for this double-queue implementation.");
//		return parkQueue.offer(e, timeout, unit);
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.LinkedBlockingQueue#offer(java.lang.Object)
	 */
	@Override
	public boolean offer(E e)
	{
		return parkQueue.offer(e);
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.LinkedBlockingQueue#poll(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	@Nullable
	public E poll(long timeout, TimeUnit unit)
		throws InterruptedException
	{
		return retrieveElementWithWait(timeout, unit);
	}
	
	

	/* (non-Javadoc)
	 * @see java.util.concurrent.LinkedBlockingQueue#put(java.lang.Object)
	 */
	@Override
	public void put(E e)
		throws InterruptedException
	{
		throw new UnsupportedOperationException("The semantics for put (with wait) are too weird for this double-queue implementation.");
//		parkQueue.put(e);
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.LinkedBlockingQueue#take()
	 */
	@Override
	@Nonnull
	public E take()
		throws InterruptedException
	{
		return nnChecked(retrieveElementWithWait(0, null));
	}
	
	
}
