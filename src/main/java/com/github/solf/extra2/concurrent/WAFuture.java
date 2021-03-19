/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Extension to {@link Future} interface that has reference to {@link Callable} 
 * that this future is for.
 *
 * @param <T> invoking task type
 * @param <V> task result type
 * 
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public interface WAFuture<T extends Callable<V>, V> extends Future<V>
{
	/**
	 * Gets {@link Callable} that this future is for.
	 * This is useful if e.g. task didn't complete for whatever reason and
	 * result is not available.
	 */
	public T getTask();
}
