/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * This is an extension to {@link FutureTask} that also contains reference
 * to the task being executed.
 * 
 * Note that unlike parent it only supports {@link Callable} ({@link Runnable}
 * is not supported).
 *
 * @author Sergey Olefir
 */
public class WARunnableFutureTask<T extends Runnable, V> extends FutureTask<V> implements WARunnableFuture<T, V>
{

	/**
	 * Task being executed.
	 */
	private final T runnable;
	
	/**
	 * @param callable
	 */
	public WARunnableFutureTask(T runnable, V result)
	{
		super(runnable, result);
		this.runnable = runnable;
	}

	/* (non-Javadoc)
	 * @see site.sonata.extra2.concurrent.WARunnableFuture#getRunnable()
	 */
	@Override
	public T getRunnable()
	{
		return runnable;
	}
}
