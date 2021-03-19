/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.lambda;

import java.util.function.Function;

import javax.annotation.Nonnull;

/**
 * Thread-unsafe lazily initialized container.
 * Intended specifically for use in local variables.
 * 
 * This is {@link ParameterizedUnsafeLazy} renamed for semantics purposes.
 * 
 * This container accepts one argument to {@link #get(Object)} -- thus allowing
 * parameterized lazy creation. HOWEVER once object is actually created, all
 * subsequent invocations return the cached instance (ignoring whatever was
 * passed in argument).
 * 
 * Factory reference is released after object creation (so it can be garbage-collected).
 *
 * @param <T> parameter type for object initialization (first arg in {@link Function})
 * @param <R> actual stored/returned object type (second arg in {@link Function})
 * 
 * @author Sergey Olefir
 */
public class ParameterizedLocalLazy<T, R> extends ParameterizedUnsafeLazy<T, R>
{

	/**
	 * @param factory
	 */
	public ParameterizedLocalLazy(@Nonnull Function<T, R> factory)
	{
		super(factory);
	}
	
}
