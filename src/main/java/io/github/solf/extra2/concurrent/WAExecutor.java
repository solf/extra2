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

import static io.github.solf.extra2.util.NullUtil.nn;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.NonNullByDefault;

import io.github.solf.extra2.concurrent.exception.WAExecutionException;
import io.github.solf.extra2.concurrent.exception.WAInterruptedException;
import io.github.solf.extra2.concurrent.exception.WATimeoutException;
import io.github.solf.extra2.util.TypeUtil;

/**
 * Class that improves on {@link ThreadPoolExecutor} by providing back futures
 * that have references to the task actually being ran.
 * 
 * NOTE: by default uses executor with daemon threads, so if it is essential that
 * tasks complete before JVM exit you have to wait for results. 
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class WAExecutor
{
	/**
	 * Executor to be used for tasks by default.
	 */
	private static final ExecutorService defaultExecutor = new WAThreadPoolExecutor("WAExecutor", true);

	/**
     * Executes the given tasks, returning a list of Futures holding
     * their status and results when all complete.
     * {@link Future#isDone} is {@code true} for each
     * element of the returned list.
     * Note that a <em>completed</em> task could have
     * terminated either normally or by throwing an exception.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks the collection of tasks
     * @param <T> the type of the values returned from the tasks
     * @return a list of Futures representing the tasks, in the same
     *         sequential order as produced by the iterator for the
     *         given task list, each of which has completed
     * @throws InterruptedException if interrupted while waiting, in
     *         which case unfinished tasks are cancelled
     * @throws NullPointerException if tasks or any of its elements are {@code null}
     * @throws RejectedExecutionException if any task cannot be
     *         scheduled for execution
     * 
	 * @see {@link ExecutorService#invokeAll(Collection)}
	 */
    @SuppressWarnings("unchecked")
	public static <T extends Callable<V>, V> List<WAFuture<T, V>> invokeAll(Collection<T> tasks)
		throws WAInterruptedException
    {
    	try
    	{
	    	// The brutal type disregard is because we know that the result is actually
	    	// going to be what we expected.
			@SuppressWarnings("rawtypes")
			List result = defaultExecutor.invokeAll(tasks);
	    	return result;
    	} catch (InterruptedException e)
    	{
    		throw new WAInterruptedException(e);
    	}
    }

    /**
     * Executes the given tasks, returning a list of Futures holding
     * their status and results
     * when all complete or the timeout expires, whichever happens first.
     * {@link Future#isDone} is {@code true} for each
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
     * @return a list of Futures representing the tasks, in the same
     *         sequential order as produced by the iterator for the
     *         given task list. If the operation did not time out,
     *         each task will have completed. If it did time out, some
     *         of these tasks will not have completed.
     * @throws InterruptedException if interrupted while waiting, in
     *         which case unfinished tasks are cancelled
     * @throws NullPointerException if tasks, any of its elements, or
     *         unit are {@code null}
     * @throws RejectedExecutionException if any task cannot be scheduled
     *         for execution
     * 
     * @see ExecutorService#invokeAll(Collection, long, TimeUnit)
     */
    @SuppressWarnings("unchecked")
	public static <T extends Callable<V>, V> List<WAFuture<T, V>> invokeAll(Collection<T> tasks,
        long timeout, TimeUnit unit)
        	throws WAInterruptedException
    {
    	try
    	{
	    	// The brutal type disregard is because we know that the result is actually
	    	// going to be what we expected.
			@SuppressWarnings("rawtypes")
			List result = defaultExecutor.invokeAll(tasks, timeout, unit);
	    	return result;
    	} catch (InterruptedException e)
    	{
    		throw new WAInterruptedException(e);
    	}
    }
    
    /**
     * Invokes all using given executor.
     * Convenience method so you don't have to deal with rawtype warnings and such.
     * 
     * @see {@link ExecutorService#invokeAll(Collection)}
     */
    @SuppressWarnings("unchecked")
	public static <T extends Callable<V>, V> List<WAFuture<T, V>> invokeAllWith(
		Collection<T> tasks, WAExecutorService executorService)
		throws WAInterruptedException
    {
    	try
    	{
	    	// The brutal type disregard is because we know that the result is actually
	    	// going to be what we expected.
			@SuppressWarnings("rawtypes")
			List result = executorService.invokeAll(tasks);
	    	return result;
    	} catch (InterruptedException e)
    	{
    		throw new WAInterruptedException(e);
    	}
    }
    
    /**
     * Invokes all using given executor.
     * Convenience method so you don't have to deal with rawtype warnings and such.
     * 
     * @see {@link ExecutorService#invokeAll(Collection)}
     */
    @SuppressWarnings("unchecked")
	public static <T extends Callable<V>, V> List<WAFuture<T, V>> invokeAllWith(
		Collection<T> tasks, WAExecutorService executorService, long timeout, TimeUnit unit)
		throws WAInterruptedException
    {
    	try
    	{
	    	// The brutal type disregard is because we know that the result is actually
	    	// going to be what we expected.
			@SuppressWarnings("rawtypes")
			List result = executorService.invokeAll(tasks, timeout, unit);
	    	return result;
    	} catch (InterruptedException e)
    	{
    		throw new WAInterruptedException(e);
    	}
    }
    
    /**
     * Submits a value-returning task for execution and returns a
     * {@link WAFuture} representing the pending results of the task. The
     * {@link WAFuture} <tt>get</tt> method will return the task's result upon
     * successful completion.
     * 
     * NOTE: uses daemon thread pool. Wait for completion if completion is essential.
     * 
     * Argument is actually {@link Callable} instance parameterized with its
     * return type. Return value is {@link WAFuture} parameterized with given
     * callable type and return value type.
     *
     * <p>
     * If you would like to immediately block waiting
     * for a task, you can use constructions of the form
     * <tt>result = WAExecutor.submit(aCallable).get();</tt>
     *
     * @param task the task to submit
     * @return a {@link WAFuture} representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     * 
     * @see {@link ExecutorService#submit(Callable)}
     */
	public static <T extends Callable<V>, V> WAFuture<T, V> submit(T task)
    {
    	// The brutal type disregard is because we know that the result is actually
    	// going to be what we expected.
    	return TypeUtil.coerce(nn(defaultExecutor.submit(task)));
    }
    
    /**
     * Same as {@link #submit(Callable)} but not overloaded method to avoid
     * problems with overloaded methods and lambdas: http://stackoverflow.com/questions/21905169/java8-ambiguity-with-lambdas-and-overloaded-methods
     * E.g.: javac can't compile this: 		WAFuture<?, Boolean> future2 = WAExecutor.submit(() -> {return service2.shutdown(limit);});
     * 
     * Submits a value-returning task for execution and returns a
     * {@link WAFuture} representing the pending results of the task. The
     * {@link WAFuture} <tt>get</tt> method will return the task's result upon
     * successful completion.
     * 
     * NOTE: uses daemon thread pool. Wait for completion if completion is essential.
     * 
     * Argument is actually {@link Callable} instance parameterized with its
     * return type. Return value is {@link WAFuture} parameterized with given
     * callable type and return value type.
     *
     * <p>
     * If you would like to immediately block waiting
     * for a task, you can use constructions of the form
     * <tt>result = WAExecutor.submit(aCallable).get();</tt>
     *
     * @param task the task to submit
     * @return a {@link WAFuture} representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     * 
     * @see {@link ExecutorService#submit(Callable)}
     */
	public static <T extends Callable<V>, V> WAFuture<T, V> submitCallable(T task)
    {
    	return submit(task);
    }
    
    /**
     * Submits a task (without return value) for execution and returns a
     * {@link WARunnableFuture} representing the pending results of the task. The
     * {@link WARunnableFuture} <tt>get</tt> method will return the task's result upon
     * successful completion.
     * 
     * NOTE: uses daemon thread pool. Wait for completion if completion is essential.
     * 
     * Argument is actually {@link Runnable} instance. Return value is 
     * {@link WARunnableFuture} parameterized with given task type and ? (since
     * there's no return value type).
     *
     * NOTE2: get() on the returned Future will return null in case of successful
     * completion.
     *
     * NOTE3: if you want to use anonymous inner type instance for Runnable, you
     * will need something like this for variable holding the return value:
     * WARunnableFuture<? extends Runnable, ?> 
     *
     * <p>
     * If you would like to immediately block waiting
     * for a task, you can use constructions of the form
     * <tt>result = WAExecutor.submit(aRunnable).get();</tt>
     *
     * @param task the task to submit
     * @return a {@link WARunnableFuture} representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     * 
     * @see {@link ExecutorService#submit(Runnable)}
     */
	public static <T extends Runnable> WARunnableFuture<T, ?> submit(T task)
    {
    	// The brutal type disregard is because we know that the result is actually
    	// going to be what we expected.
    	return TypeUtil.coerce(nn(defaultExecutor.submit(task)));
    }
    
    /**
     * Same as {@link #submit(Runnable)} but not overloaded method to avoid
     * problems with overloaded methods and lambdas: http://stackoverflow.com/questions/21905169/java8-ambiguity-with-lambdas-and-overloaded-methods
     * E.g.: javac can't compile this: 		WAFuture<?, Boolean> future2 = WAExecutor.submit(() -> {return service2.shutdown(limit);});
     * 
     * Submits a task (without return value) for execution and returns a
     * {@link WARunnableFuture} representing the pending results of the task. The
     * {@link WARunnableFuture} <tt>get</tt> method will return the task's result upon
     * successful completion.
     * 
     * NOTE: uses daemon thread pool. Wait for completion if completion is essential.
     * 
     * Argument is actually {@link Runnable} instance. Return value is 
     * {@link WARunnableFuture} parameterized with given task type and ? (since
     * there's no return value type).
     *
     * NOTE2: get() on the returned Future will return null in case of successful
     * completion.
     *
     * NOTE3: if you want to use anonymous inner type instance for Runnable, you
     * will need something like this for variable holding the return value:
     * WARunnableFuture<? extends Runnable, ?> 
     *
     * <p>
     * If you would like to immediately block waiting
     * for a task, you can use constructions of the form
     * <tt>result = WAExecutor.submit(aRunnable).get();</tt>
     *
     * @param task the task to submit
     * @return a {@link WARunnableFuture} representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     * 
     * @see {@link ExecutorService#submit(Runnable)}
     */
	public static <T extends Runnable> WARunnableFuture<T, ?> submitRunnable(T task)
    {
		return submit(task);
    }
    
    /**
     * Executes a value-returning task asynchronously waiting for result indefinitely.
     * NOTE: uses daemon thread pool. Wait for completion if completion is essential.
     * 
     * Argument is actually {@link Callable} instance parameterized with its
     * return type. Return value is whatever given {@link Callable} returns.
     * 
     * The benefit (for certain situations) of this method is that it converts 
     * checked exceptions to unchecked.
     */
    public static <T extends Callable<V>, V> V invoke(T task) throws
    	NullPointerException, RejectedExecutionException, CancellationException,
    	WAInterruptedException, WAExecutionException
    {
    	try
		{
			return submit(task).get();
		} catch( InterruptedException e )
		{
			throw new WAInterruptedException(e);
		} catch( ExecutionException e )
		{
			throw new WAExecutionException(e);
		}
    }
    
    /**
     * Same as {@link #invoke(Callable)} but not overloaded method to avoid
     * problems with overloaded methods and lambdas: http://stackoverflow.com/questions/21905169/java8-ambiguity-with-lambdas-and-overloaded-methods
     * E.g.: javac can't compile this: 		WAFuture<?, Boolean> future2 = WAExecutor.submit(() -> {return service2.shutdown(limit);});
     * 
     * Executes a value-returning task asynchronously waiting for result indefinitely.
     * NOTE: uses daemon thread pool. Wait for completion if completion is essential.
     * 
     * Argument is actually {@link Callable} instance parameterized with its
     * return type. Return value is whatever given {@link Callable} returns.
     * 
     * The benefit (for certain situations) of this method is that it converts 
     * checked exceptions to unchecked.
     */
    public static <T extends Callable<V>, V> V invokeCallable(T task) throws
    	NullPointerException, RejectedExecutionException, CancellationException,
    	WAInterruptedException, WAExecutionException
    {
    	return invoke(task);
    }
    
    /**
     * Executes a value-returning task asynchronously waiting for result for
     * the specified time.
     * NOTE: uses daemon thread pool. Wait for completion if completion is essential.
     * 
     * Argument is actually {@link Callable} instance parameterized with its
     * return type. Return value is whatever given {@link Callable} returns.
     * 
     * The benefit (for certain situations) of this method is that it converts 
     * checked exceptions to unchecked.
     */
    public static <T extends Callable<V>, V> V invoke(T task, long timeout, TimeUnit unit) throws
    	NullPointerException, RejectedExecutionException, CancellationException,
    	WAInterruptedException, WAExecutionException, WATimeoutException
    {
    	try
		{
			return submit(task).get(timeout, unit);
		} catch( InterruptedException e )
		{
			throw new WAInterruptedException(e);
		} catch( ExecutionException e )
		{
			throw new WAExecutionException(e);
		} catch( TimeoutException e )
		{
			throw new WATimeoutException(e);
		}
    }
    
    /**
     * Same as {@link #invoke(Callable, long, TimeUnit)} but not overloaded method to avoid
     * problems with overloaded methods and lambdas: http://stackoverflow.com/questions/21905169/java8-ambiguity-with-lambdas-and-overloaded-methods
     * E.g.: javac can't compile this: 		WAFuture<?, Boolean> future2 = WAExecutor.submit(() -> {return service2.shutdown(limit);});
     * 
     * Executes a value-returning task asynchronously waiting for result for
     * the specified time.
     * NOTE: uses daemon thread pool. Wait for completion if completion is essential.
     * 
     * Argument is actually {@link Callable} instance parameterized with its
     * return type. Return value is whatever given {@link Callable} returns.
     * 
     * The benefit (for certain situations) of this method is that it converts 
     * checked exceptions to unchecked.
     */
    public static <T extends Callable<V>, V> V invokeCallable(T task, long timeout, TimeUnit unit) throws
    	NullPointerException, RejectedExecutionException, CancellationException,
    	WAInterruptedException, WAExecutionException, WATimeoutException
    {
    	return invoke(task, timeout, unit);
    }
    
    /**
     * Executes a task (without return value) asynchronously waiting for completion indefinitely.
     * NOTE: uses daemon thread pool. Wait for completion if completion is essential.
     * 
     * The benefit (for certain situations) of this method is that it converts 
     * checked exceptions to unchecked.
     */
    public static void invoke(Runnable task) throws
    	NullPointerException, RejectedExecutionException, CancellationException,
    	WAInterruptedException, WAExecutionException
    {
    	try
		{
    		submit(task).get();
		} catch( InterruptedException e )
		{
			throw new WAInterruptedException(e);
		} catch( ExecutionException e )
		{
			throw new WAExecutionException(e);
		}
    }
    
    /**
     * Same as {@link #invoke(Runnable)} but not overloaded method to avoid
     * problems with overloaded methods and lambdas: http://stackoverflow.com/questions/21905169/java8-ambiguity-with-lambdas-and-overloaded-methods
     * E.g.: javac can't compile this: 		WAFuture<?, Boolean> future2 = WAExecutor.submit(() -> {return service2.shutdown(limit);});
     * 
     * Executes a task (without return value) asynchronously waiting for completion indefinitely.
     * NOTE: uses daemon thread pool. Wait for completion if completion is essential.
     * 
     * The benefit (for certain situations) of this method is that it converts 
     * checked exceptions to unchecked.
     */
    public static void invokeRunnable(Runnable task) throws
    	NullPointerException, RejectedExecutionException, CancellationException,
    	WAInterruptedException, WAExecutionException
    {
    	invoke(task);
    }
    
    /**
     * Executes a task (without return value) asynchronously waiting for completion indefinitely.
     * NOTE: uses daemon thread pool. Wait for completion if completion is essential.
     * 
     * The benefit (for certain situations) of this method is that it converts 
     * checked exceptions to unchecked.
     */
    public static void invoke(Runnable task, long timeout, TimeUnit unit) throws
    	NullPointerException, RejectedExecutionException, CancellationException,
    	WAInterruptedException, WAExecutionException
    {
    	try
		{
    		submit(task).get(timeout, unit);
		} catch( InterruptedException e )
		{
			throw new WAInterruptedException(e);
		} catch( ExecutionException e )
		{
			throw new WAExecutionException(e);
		} catch( TimeoutException e )
		{
			throw new WATimeoutException(e);
		}
    }
    
    /**
     * Same as {@link #invoke(Runnable, long, TimeUnit)} but not overloaded method to avoid
     * problems with overloaded methods and lambdas: http://stackoverflow.com/questions/21905169/java8-ambiguity-with-lambdas-and-overloaded-methods
     * E.g.: javac can't compile this: 		WAFuture<?, Boolean> future2 = WAExecutor.submit(() -> {return service2.shutdown(limit);});
     * 
     * Executes a task (without return value) asynchronously waiting for completion indefinitely.
     * NOTE: uses daemon thread pool. Wait for completion if completion is essential.
     * 
     * The benefit (for certain situations) of this method is that it converts 
     * checked exceptions to unchecked.
     */
    public static void invokeRunnable(Runnable task, long timeout, TimeUnit unit) throws
    	NullPointerException, RejectedExecutionException, CancellationException,
    	WAInterruptedException, WAExecutionException
    {
    	invokeRunnable(task, timeout, unit);
    }
}
