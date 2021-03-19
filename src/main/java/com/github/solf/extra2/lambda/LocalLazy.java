/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.lambda;

import java.util.function.Supplier;

import javax.annotation.Nonnull;

/**
 * Thread-unsafe lazily initialized container.
 * 
 * This is {@link UnsafeLazy} renamed for semantics purposes.
 * 
 * Intended for use in local variables.
 * 
 * Factory reference is released after object creation (so it can be garbage-collected).
 *
 * @author Sergey Olefir
 */
public class LocalLazy<T> extends UnsafeLazy<T>
{
	/**
	 * @param factory
	 */
	public LocalLazy(@Nonnull Supplier<T> factory)
	{
		super(factory);
	}
}
