/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.concurrent;

import static site.sonata.extra2.util.NullUtil.nn;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.ParametersAreNonnullByDefault;

import site.sonata.extra2.concurrent.exception.WAExecutionException;
import site.sonata.extra2.concurrent.exception.WAInterruptedException;
import site.sonata.extra2.concurrent.exception.WATimeoutException;
import site.sonata.extra2.util.TypeUtil;

/**
 * Class that improves on {@link ThreadPoolExecutor} by providing back futures
 * that have references to the task actually being ran.
 * 
 * NOTE: by default uses executor with daemon threads, so if it is essential that
 * tasks complete before JVM exit you have to wait for results. 
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class WAExecutor
{
	/**
	 * Executor to be used for tasks by default.
	 */
	private static final ExecutorService defaultExecutor = new WAThreadPoolExecutor("WAExecutor", true);

	/**
     * NOTE: uses daemon thread pool. Wait for completion if completion is essential.
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
     * NOTE: uses daemon thread pool. Wait for completion if completion is essential.
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
