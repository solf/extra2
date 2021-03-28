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

import static com.github.solf.extra2.util.NullUtil.nullable;

import java.util.function.Function;

import javax.annotation.Nonnull;

import com.github.solf.extra2.util.TypeUtil;

/**
 * Thread-safe lazily initialized container.
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
public class ParameterizedSafeLazy<T, R>
{
	/**
	 * Object value used as 'not initialized' flag.
	 */
	private static Object VALUE_NOT_INITIALIZED = new Object();
	
	/**
	 * Factory for actually creating/initializing the object.
	 * Set to null after object is created.
	 */
	private Function<T, R> factory;
	
	/**
	 * Actual object (lazily created via factory).
	 * Volatile to account for concurrency issues.
	 * Typed as Object so we can use {@link #VALUE_NOT_INITIALIZED} as marker.
	 */
	private Object obj;
	
	/**
	 * Constructor.
	 */
	public ParameterizedSafeLazy(@Nonnull Function<T, R> factory)
	{
		this.factory = factory;
		this.obj = VALUE_NOT_INITIALIZED;
		
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
	 * Thread-safe: cost is one volatile access per get (if value already created)
	 * and full synchronization when actually creating the value.
	 */
	public R get(T initializationArg)
	{
		Object result = obj; // Cache volatile locally.
		if (result == VALUE_NOT_INITIALIZED)
		{
			synchronized(this)
			{
				result = obj; // Double-checked locking: re-read in case it was initialized concurrently
				if (result == VALUE_NOT_INITIALIZED)
				{
					result = factory.apply(initializationArg);
					obj = result;
					factory = null; // Release factory so it can be garbage-collected.
				}
			}
		}
		
		return TypeUtil.coerceUnknown(result); // Assuming that factory returns proper type. 
	}
}
