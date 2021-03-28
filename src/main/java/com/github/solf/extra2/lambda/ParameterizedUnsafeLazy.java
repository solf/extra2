/**
 * Copyright Sergey Olefir
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.solf.extra2.lambda;

import static com.github.solf.extra2.util.NullUtil.fakeNonNull;
import static com.github.solf.extra2.util.NullUtil.nullable;

import java.util.function.Function;

import javax.annotation.Nonnull;

/**
 * Thread-unsafe lazily initialized container.
 * Intended for use in e.g. method-local variables.
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
public class ParameterizedUnsafeLazy<T, R>
{
	/**
	 * Factory for actually creating/initializing the object.
	 * Set to null after object is created.
	 */
	private Function<T, R> factory;
	
	/**
	 * Actual object (lazily created via factory).
	 */
	private R obj;
	
	/**
	 * Constructor.
	 */
	public ParameterizedUnsafeLazy(@Nonnull Function<T, R> factory)
	{
		this.factory = factory;
		this.obj = fakeNonNull(); // make compiler happy; it'll be initialized later
		
		if (nullable(factory) == null)
			throw new IllegalArgumentException("Factory may not be null.");
	}
	
	/**
	 * Gets the contained object.
	 * Object is created via provided factory using the supplied argument 
	 * on the first request and then cached.
	 * 
	 * Subsequent invocations return cached value (ignoring whatever is passed
	 * in argument).
	 * 
	 * Thread-UNSAFE!
	 */
	public R get(T initializationArg)
	{
		if (factory != null)
		{
			obj = factory.apply(initializationArg);
			factory = null;
		}
		
		return obj;
	}
}
