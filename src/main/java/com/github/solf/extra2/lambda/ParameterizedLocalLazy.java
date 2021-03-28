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
