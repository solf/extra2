/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.concurrent;

import static site.sonata.extra2.util.NullUtil.nn;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import site.sonata.extra2.concurrent.exception.WAInterruptedException;
import site.sonata.extra2.util.TypeUtil;

/**
 * This is a slight modification of {@link ThreadPoolExecutor} that alters
 * returned futures so that they are instances of {@link WAFutureTask} or 
 * {@link WARunnableFutureTask} (depending on whether tasks are {@link Callable}
 * or {@link Runnable}).
 * 
 * wa* methods provide convenient type-safe access to {@link WAFuture}s returned 
 * by the standard invoke/submit functions.
 * 
 * Also, unlike {@link ThreadPoolExecutor}, this class provides capability of
 * a dynamic bounded (by max thread number) thread pool -- that is thread starts
 * out empty and fills with up to max number of threads as needed (which may
 * then timeout and stop if idle).
 * 
 * Original {@link ThreadPoolExecutor} with {@link SynchronousQueue} will simply
 * reject tasks if all threads are currently busy. With {@link LinkedBlockingQueue}
 * (or similar) it will simply never create any additional threads beyond the 
 * core pool size.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class WAThreadPoolExecutor extends ThreadPoolExecutor implements WAExecutorService
{
	/**
	 * Rejection handler for integration with {@link WAExecutorQueue}
	 */
	private static class WARejectionHandler implements RejectedExecutionHandler
	{
		/**
		 * Executor queue with force add.
		 */
		private final WAExecutorQueue<Runnable> queue;
		
		/**
		 * Constructor.
		 */
		public WARejectionHandler(WAExecutorQueue<Runnable> queue)
		{
			assert queue != null;
			this.queue = queue;
		}

		/* (non-Javadoc)
		 * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
		 */
		@Override
		public void rejectedExecution(@SuppressWarnings("null") Runnable r, @SuppressWarnings("null") ThreadPoolExecutor executor)
		{
			if (!queue.forceAdd(r))
				throw new RejectedExecutionException("Unable to force-add task to the queue.");
		}
		
	}
	
	/**
	 * Constructor.
	 * Priority is set to {@link Thread#NORM_PRIORITY}
	 * 
	 * Number of threads is unbounded -- as many as created as there are concurrent
	 * tasks to execute.
	 * 
	 * Idle threads timeout & shut down after 1 minute. 
	 * 
	 * @param groupName group name to be used for threads and also prefix for every thread name
	 * @param daemon whether threads should be daemon
	 */
	public WAThreadPoolExecutor(String groupName, boolean daemon) throws IllegalArgumentException
	{
		this(groupName, daemon, Thread.NORM_PRIORITY);
	}
	
	/**
	 * Constructor.
	 * 
	 * Number of threads is unbounded -- as many as created as there are concurrent
	 * tasks to execute.
	 * 
	 * Idle threads timeout & shut down after 1 minute. 
	 * 
	 * @param groupName group name to be used for threads and also prefix for every thread name
	 * @param daemon whether threads should be daemon
	 * @param priority what priority threads should have, e.g. {@link Thread#NORM_PRIORITY}
	 */
	public WAThreadPoolExecutor(String groupName, boolean daemon, int priority)
	{
		super(0, Integer.MAX_VALUE,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(),
            new WAThreadFactory(groupName, daemon, priority));
	}
	
	/**
	 * Constructor with limit on how many threads can be used simultaneously.
	 * Priority is set to {@link Thread#NORM_PRIORITY}
	 * 
	 * Idle threads timeout & shut down after 1 minute. 
	 * 
	 * @param maxThreads maximum number of threads that can be used concurrently by this executor
	 * 		(starts out empty and adds up to maximum number of threads over time)
	 * @param groupName group name to be used for threads and also prefix for every thread name
	 * @param daemon whether threads should be daemon
	 */
	public WAThreadPoolExecutor(int maxThreads, String groupName, boolean daemon)
	{
		this(maxThreads, groupName, daemon, Thread.NORM_PRIORITY);
	}
	
	/**
	 * Constructor with limit on how many threads can be used simultaneously.
	 * 
	 * Idle threads timeout & shut down after 1 minute. 
	 * 
	 * @param maxThreads maximum number of threads that can be used concurrently by this executor
	 * 		(starts out empty and adds up to maximum number of threads over time)
	 * @param groupName group name to be used for threads and also prefix for every thread name
	 * @param daemon whether threads should be daemon
	 * @param priority what priority threads should have, e.g. {@link Thread#NORM_PRIORITY}
	 */
	public WAThreadPoolExecutor(int maxThreads, String groupName, boolean daemon, int priority)
	{
		super(0, maxThreads,
            60L, TimeUnit.SECONDS,
            new WAExecutorQueue<Runnable>(),
			new WAThreadFactory(groupName, daemon, priority));
		setRejectedExecutionHandler(new WARejectionHandler((WAExecutorQueue<Runnable>)getQueue()));
	}
	

	/**
	 * Most flexible constructor -- in most cases using other constructors is
	 * preferable.
	 * 
     * Creates a new {@code WAThreadPoolExecutor} with the given initial
     * parameters.
     * 
     * Unlike original {@link ThreadPoolExecutor} implements true bounded
     * thread pool -- that is number of threads starts at corePoolSize and can
     * scale up to maximumPoolSize if there are tasks to do (see class comment
     * for more details).
     * 
     * This is implemented via manipulation of tasks queue and rejected
     * execution handler -- therefore it is not allowed to specify those
     * externally.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool; pass {@link Integer#MAX_VALUE} if pool should be unbounded
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param threadFactory the factory to use when the executor
     *        creates a new thread; consider using {@link WAThreadFactory}
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code threadFactory} is null
	 */
	public WAThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
		long keepAliveTime, TimeUnit unit, ThreadFactory threadFactory)
	{
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, 
			maximumPoolSize == Integer.MAX_VALUE ? 
				new SynchronousQueue<Runnable>() : 
				new WAExecutorQueue<Runnable>()	,
			threadFactory);
		
		if (maximumPoolSize != Integer.MAX_VALUE)
			setRejectedExecutionHandler(new WARejectionHandler((WAExecutorQueue<Runnable>)getQueue()));
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.AbstractExecutorService#newTaskFor(java.lang.Runnable, java.lang.Object)
	 */
	@Override
	protected <T> RunnableFuture<T> newTaskFor(@SuppressWarnings("null") Runnable runnable, T value)
	{
		return new WARunnableFutureTask<Runnable, T>(runnable, value);
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.AbstractExecutorService#newTaskFor(java.util.concurrent.Callable)
	 */
	@Override
	protected <T> RunnableFuture<T> newTaskFor(@SuppressWarnings("null") Callable<T> callable)
	{
		return new WAFutureTask<Callable<T>, T>(callable);
	}
	

    /**
     * Submits a value-returning task for execution and returns a
     * {@link WAFuture} representing the pending results of the task. The
     * WAFuture's {@code get} method will return the task's result upon
     * successful completion.
     *
     * <p>
     * If you would like to immediately block waiting
     * for a task, you can use constructions of the form
     * {@code result = exec.submit(aCallable).get();}
     *
     * <p>Note: The {@link Executors} class includes a set of methods
     * that can convert some other common closure-like objects,
     * for example, {@link java.security.PrivilegedAction} to
     * {@link Callable} form so they can be submitted.
     *
     * @param task the task to submit
     * @param <T> the type of the task's result
     * @return a WAFuture representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
	public <T extends Callable<V>, V> WAFuture<T, V> waSubmit(Callable<V> task)
    {
    	// The brutal type disregard is because we know that the result is actually
    	// going to be what we expected.
    	return TypeUtil.coerce(nn(this.submit(task)));
    }

    /**
     * Submits a Runnable task for execution and returns a {@link WAFuture}
     * representing that task. The WAFuture's {@code get} method will
     * return the given result upon successful completion.
     *
     * @param task the task to submit
     * @param result the result to return
     * @param <T> the type of the result
     * @return a WAFuture representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
	public <T extends Runnable, V> WARunnableFuture<T, V> waSubmit(Runnable task, T result)
    {
    	// The brutal type disregard is because we know that the result is actually
    	// going to be what we expected.
    	return TypeUtil.coerce(nn(this.submit(task, result)));
    }

    /**
     * Submits a Runnable task for execution and returns a {@link WAFuture}
     * representing that task. The WAFuture's {@code get} method will
     * return {@code null} upon <em>successful</em> completion.
     *
     * @param task the task to submit
     * @return a WAFuture representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
	public <T extends Runnable> WARunnableFuture<T, Void> waSubmit(Runnable task)
    {
    	// The brutal type disregard is because we know that the result is actually
    	// going to be what we expected.
    	return TypeUtil.coerce(nn(this.submit(task)));
    }


    /**
     * Executes the given tasks, returning a list of {@link WAFuture}s holding
     * their status and results when all complete.
     * {@link WAFuture#isDone} is {@code true} for each
     * element of the returned list.
     * Note that a <em>completed</em> task could have
     * terminated either normally or by throwing an exception.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks the collection of tasks
     * @param <T> the type of the values returned from the tasks
     * @return a list of WAFutures representing the tasks, in the same
     *         sequential order as produced by the iterator for the
     *         given task list, each of which has completed
     * @throws WAInterruptedException if interrupted while waiting, in
     *         which case unfinished tasks are cancelled
     * @throws NullPointerException if tasks or any of its elements are {@code null}
     * @throws RejectedExecutionException if any task cannot be
     *         scheduled for execution
     */
	public <T extends Callable<V>, V> List<WAFuture<T, V>> waInvokeAll(Collection<T> tasks)
        throws WAInterruptedException
    {
		return WAExecutor.invokeAllWith(tasks, this);
    }

    /**
     * Executes the given tasks, returning a list of {@link WAFuture}s holding
     * their status and results
     * when all complete or the timeout expires, whichever happens first.
     * {@link WAFuture#isDone} is {@code true} for each
     * element of the returned list.
     * Upon return, tasks that have not completed are cancelled.
     * Note that a <em>completed</em> task could have
     * terminated either normally or by throwing an exception.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks the collection of tasks
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @param <T> the type of the values returned from the tasks
     * @return a list of WAFutures representing the tasks, in the same
     *         sequential order as produced by the iterator for the
     *         given task list. If the operation did not time out,
     *         each task will have completed. If it did time out, some
     *         of these tasks will not have completed.
     * @throws WAInterruptedException if interrupted while waiting, in
     *         which case unfinished tasks are cancelled
     * @throws NullPointerException if tasks, any of its elements, or
     *         unit are {@code null}
     * @throws RejectedExecutionException if any task cannot be scheduled
     *         for execution
     */
	public <T extends Callable<V>, V> List<WAFuture<T, V>> waInvokeAll(Collection<T> tasks,
                                  long timeout, TimeUnit unit)
        throws WAInterruptedException
    {
    	return WAExecutor.invokeAllWith(tasks, this, timeout, unit);
    }



    /**
     * This is an exact mirror of {@link #execute(Runnable)} added only so that
     * waExecute would be present for completeness of wa* methods.
     * 
     * Executes the given task sometime in the future.  The task
     * may execute in a new thread or in an existing pooled thread.
     *
     * If the task cannot be submitted for execution, either because this
     * executor has been shutdown or because its capacity has been reached,
     * the task is handled by the current {@code RejectedExecutionHandler}.
     *
     * @param command the task to execute
     * @throws RejectedExecutionException at discretion of
     *         {@code RejectedExecutionHandler}, if the task
     *         cannot be accepted for execution
     * @throws NullPointerException if {@code command} is null
     */
    public void waExecute(Runnable command) 
    {
    	execute(command);
    }
}
