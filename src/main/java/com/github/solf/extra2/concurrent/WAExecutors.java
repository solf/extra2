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
package com.github.solf.extra2.concurrent;

import static com.github.solf.extra2.util.NullUtil.nn;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A 'better' version of {@link Executors}.
 * 
 * Some methods return {@link WAThreadPoolExecutor} instances that provide
 * access to {@link WAFuture} (which is 'better' that {@link Future} because
 * it contains reference to the original task).
 * 
 * Even if methods do not provide extra functionality, they at least require
 * enough information specification so that 'anonymous' threads are not created
 * and daemon true|false is very explicit (unlike {@link Executors} methods
 * that silently create non-daemon threads with generic names of 'pool-XX-thread-YY').
 * 
 * @see WAExecutor
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class WAExecutors
{

    static class FinalizableWAThreadPoolExecutor
        extends WAThreadPoolExecutor {
        /**
		 * @param corePoolSize
		 * @param maximumPoolSize
		 * @param keepAliveTime
		 * @param unit
		 * @param threadFactory
		 */
		public FinalizableWAThreadPoolExecutor(int corePoolSize,
			int maximumPoolSize, long keepAliveTime, @Nonnull TimeUnit unit,
			@Nonnull ThreadFactory threadFactory)
		{
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, threadFactory);
		}

		@SuppressWarnings("all") // should really be @SuppressWarnings("deprecation") but 'all' doesn't produce warnings in both 8 and 11 java
		@Override
		protected void finalize() {
            super.shutdown();
        }
    }

    /**
     * Creates a thread pool that creates new threads as needed, but
     * will reuse previously constructed threads when they are
     * available.  These pools will typically improve the performance
     * of programs that execute many short-lived asynchronous tasks.
     * Calls to {@code execute} will reuse previously constructed
     * threads if available. If no existing thread is available, a new
     * thread will be created and added to the pool. Threads that have
     * not been used for sixty seconds are terminated and removed from
     * the cache. Thus, a pool that remains idle for long enough will
     * not consume any resources. Note that pools with similar
     * properties but different details (for example, timeout parameters)
     * may be created using {@link ThreadPoolExecutor} constructors.
     * 
	 * @param groupName group name to be used for threads and also prefix for every thread name
	 * @param daemon whether threads should be daemon
     *
     * @return the newly created thread pool
     */
    public static WAThreadPoolExecutor newCachedThreadPool(String groupName, boolean daemon) {
    	return new WAThreadPoolExecutor(groupName, daemon);
    }

    /**
     * Creates a thread pool that creates new threads as needed, but
     * will reuse previously constructed threads when they are
     * available, and uses the provided
     * ThreadFactory to create new threads when needed.
     * @param threadFactory the factory to use when creating new threads
     * @return the newly created thread pool
     * @throws NullPointerException if threadFactory is null
     */
    public static WAThreadPoolExecutor newCachedThreadPool(ThreadFactory threadFactory) {
        return new WAThreadPoolExecutor(0, Integer.MAX_VALUE,
                                      60L, TimeUnit.SECONDS,
                                      threadFactory);
    }

    /**
     * Creates a thread pool that reuses a fixed number of threads
     * operating off a shared unbounded queue.  At any point, at most
     * {@code nThreads} threads will be active processing tasks.
     * If additional tasks are submitted when all threads are active,
     * they will wait in the queue until a thread is available.
     * If any thread terminates due to a failure during execution
     * prior to shutdown, a new one will take its place if needed to
     * execute subsequent tasks.  The threads in the pool will exist
     * until it is explicitly {@link ExecutorService#shutdown shutdown}.
     *
     * @param nThreads the number of threads in the pool
	 * @param groupName group name to be used for threads and also prefix for every thread name
	 * @param daemon whether threads should be daemon
     * @return the newly created thread pool
     * @throws IllegalArgumentException if {@code nThreads <= 0}
     */
    public static WAThreadPoolExecutor newFixedThreadPool(int nThreads, String groupName, boolean daemon) {
        return new WAThreadPoolExecutor(nThreads, nThreads,
                                      0L, TimeUnit.MILLISECONDS,
                                      new WAThreadFactory(groupName, daemon));
    }

    /**
     * Creates a thread pool that reuses a fixed number of threads
     * operating off a shared unbounded queue, using the provided
     * ThreadFactory to create new threads when needed.  At any point,
     * at most {@code nThreads} threads will be active processing
     * tasks.  If additional tasks are submitted when all threads are
     * active, they will wait in the queue until a thread is
     * available.  If any thread terminates due to a failure during
     * execution prior to shutdown, a new one will take its place if
     * needed to execute subsequent tasks.  The threads in the pool will
     * exist until it is explicitly {@link ExecutorService#shutdown
     * shutdown}.
     *
     * @param nThreads the number of threads in the pool
     * @param threadFactory the factory to use when creating new threads
     * @return the newly created thread pool
     * @throws NullPointerException if threadFactory is null
     * @throws IllegalArgumentException if {@code nThreads <= 0}
     */
    public static WAThreadPoolExecutor newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        return new WAThreadPoolExecutor(nThreads, nThreads,
                                      0L, TimeUnit.MILLISECONDS,
                                      threadFactory);
    }

    /**
     * Creates a thread pool that can schedule commands to run after a
     * given delay, or to execute periodically.
     * 
     * @param corePoolSize the number of threads to keep in the pool,
     * even if they are idle
	 * @param groupName group name to be used for threads and also prefix for every thread name
	 * @param daemon whether threads should be daemon
     * @return a newly created scheduled thread pool
     * @throws IllegalArgumentException if {@code corePoolSize < 0}
     */
    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, String groupName, boolean daemon) {
        return nn(Executors.newScheduledThreadPool(corePoolSize, new WAThreadFactory(groupName, daemon)));
    }

    /**
     * Creates a thread pool that can schedule commands to run after a
     * given delay, or to execute periodically.
     * @param corePoolSize the number of threads to keep in the pool,
     * even if they are idle
     * @param threadFactory the factory to use when the executor
     * creates a new thread
     * @return a newly created scheduled thread pool
     * @throws IllegalArgumentException if {@code corePoolSize < 0}
     * @throws NullPointerException if threadFactory is null
     */
    public static ScheduledExecutorService newScheduledThreadPool(
            int corePoolSize, ThreadFactory threadFactory) {
        return nn(Executors.newScheduledThreadPool(corePoolSize, threadFactory));
    }

    /**
     * Creates an Executor that uses a single worker thread operating
     * off an unbounded queue. (Note however that if this single
     * thread terminates due to a failure during execution prior to
     * shutdown, a new one will take its place if needed to execute
     * subsequent tasks.)  Tasks are guaranteed to execute
     * sequentially, and no more than one task will be active at any
     * given time. Unlike the otherwise equivalent
     * {@code newFixedThreadPool(1)} the returned executor is
     * guaranteed not to be reconfigurable to use additional threads.
     *
	 * @param groupName group name to be used for threads and also prefix for every thread name
	 * @param daemon whether threads should be daemon
     * @return the newly created single-threaded Executor
     */
    public static WAThreadPoolExecutor newSingleThreadExecutor(String groupName, boolean daemon) {
        return new FinalizableWAThreadPoolExecutor
            (1, 1,
             0L, TimeUnit.MILLISECONDS,
             new WAThreadFactory(groupName, daemon));
    }

    /**
     * Creates an Executor that uses a single worker thread operating
     * off an unbounded queue, and uses the provided ThreadFactory to
     * create a new thread when needed. Unlike the otherwise
     * equivalent {@code newFixedThreadPool(1, threadFactory)} the
     * returned executor is guaranteed not to be reconfigurable to use
     * additional threads.
     *
     * @param threadFactory the factory to use when creating new
     * threads
     *
     * @return the newly created single-threaded Executor
     * @throws NullPointerException if threadFactory is null
     */
    public static WAThreadPoolExecutor newSingleThreadExecutor(ThreadFactory threadFactory) {
        return new FinalizableWAThreadPoolExecutor
            (1, 1,
             0L, TimeUnit.MILLISECONDS,
             threadFactory);
    }

    /**
     * Creates a single-threaded executor that can schedule commands
     * to run after a given delay, or to execute periodically.
     * (Note however that if this single
     * thread terminates due to a failure during execution prior to
     * shutdown, a new one will take its place if needed to execute
     * subsequent tasks.)  Tasks are guaranteed to execute
     * sequentially, and no more than one task will be active at any
     * given time. Unlike the otherwise equivalent
     * {@code newScheduledThreadPool(1)} the returned executor is
     * guaranteed not to be reconfigurable to use additional threads.
     *
	 * @param groupName group name to be used for threads and also prefix for every thread name
	 * @param daemon whether threads should be daemon
     * @return the newly created scheduled executor
     */
    public static ScheduledExecutorService newSingleThreadScheduledExecutor(String groupName, boolean daemon) {
        return nn(Executors.newSingleThreadScheduledExecutor(new WAThreadFactory(groupName, daemon)));
    }

    /**
     * Creates a single-threaded executor that can schedule commands
     * to run after a given delay, or to execute periodically.  (Note
     * however that if this single thread terminates due to a failure
     * during execution prior to shutdown, a new one will take its
     * place if needed to execute subsequent tasks.)  Tasks are
     * guaranteed to execute sequentially, and no more than one task
     * will be active at any given time. Unlike the otherwise
     * equivalent {@code newScheduledThreadPool(1, threadFactory)}
     * the returned executor is guaranteed not to be reconfigurable to
     * use additional threads.
     * @param threadFactory the factory to use when creating new
     * threads
     * @return a newly created scheduled executor
     * @throws NullPointerException if threadFactory is null
     */
    public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
        return nn(Executors.newSingleThreadScheduledExecutor(threadFactory));
    }

}
