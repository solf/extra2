/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.concurrent;

import java.util.concurrent.Future;

/**
 * Extension to {@link Future} interface that has reference to {@link Runnable} 
 * that this future is for.
 *
 * @param <V> type of return value -- because Runnable tasks can be asked to
 * 		return specific value when they complete via e.g. {@link WAThreadPoolExecutor#submit(Runnable, Object)}
 *
 * @author Sergey Olefir
 */
public interface WARunnableFuture<T extends Runnable, V> extends Future<V>
{
	/**
	 * Gets {@link Runnable} that this future is for.
	 * This is useful if e.g. task didn't complete for whatever reason and
	 * result is not available.
	 */
	public T getRunnable();
}
