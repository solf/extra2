/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.concurrent;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * {@link Runnable} that is allowed to throw {@link InterruptedException}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
@FunctionalInterface
public interface InterruptableRunnable
{
	/**
	 * Just like {@link Runnable#run()} but allows to throw {@link InterruptedException}
	 */
	public abstract void run() throws InterruptedException;
}
