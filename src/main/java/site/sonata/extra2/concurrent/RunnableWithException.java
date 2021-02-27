/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.concurrent;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * {@link Runnable} that is allowed to throw exception.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
@FunctionalInterface
public interface RunnableWithException
{
	/**
	 * Just like {@link Runnable#run()} but allows to throw exception.
	 */
	public abstract void run() throws Exception;
}
