/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * This is an extension to {@link FutureTask} that also contains reference
 * to the task being executed.
 * 
 * Note that unlike parent it only supports {@link Callable} ({@link Runnable}
 * is not supported).
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class WAFutureTask<T extends Callable<V>, V> extends FutureTask<V> implements WAFuture<T, V>
{

	/**
	 * Task being executed.
	 */
	private final T task;
	
	/**
	 * @param callable
	 */
	public WAFutureTask(T callable)
	{
		super(callable);
		this.task = callable;
	}

	/* (non-Javadoc)
	 * @see com.github.solf.extra2.concurrent.WAFuture#getTask()
	 */
	@Override
	public T getTask()
	{
		return task;
	}

}
