/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.concurrent;

import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * {@link Consumer} that is allowed to throw {@link InterruptedException}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
@FunctionalInterface
public interface InterruptableConsumer<T>
{
	/**
	 * Just like {@link Consumer#accept(Object)} but allows to throw {@link InterruptedException}
	 */
    void accept(T t) throws InterruptedException;
}
