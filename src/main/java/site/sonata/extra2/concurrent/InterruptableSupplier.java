/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.concurrent;

import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * {@link Supplier} that is allowed to throw {@link InterruptedException}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
@FunctionalInterface
public interface InterruptableSupplier<T>
{
	/**
	 * Just like {@link Supplier#get()} but allows to throw {@link InterruptedException}
	 */
	public T get() throws InterruptedException;
}
