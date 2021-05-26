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
package io.github.solf.extra2.lambda;

import static io.github.solf.extra2.util.NullUtil.fakeNonNull;
import static io.github.solf.extra2.util.NullUtil.nullable;

import java.util.function.Supplier;

import javax.annotation.Nonnull;

/**
 * Thread-unsafe lazily initialized container.
 * Intended for use in e.g. method-local variables.
 * 
 * Factory reference is released after object creation (so it can be garbage-collected).
 *
 * @author Sergey Olefir
 */
public class UnsafeLazy<T>
{
	/**
	 * Factory for actually creating/initializing the object.
	 * Set to null after object is created.
	 */
	private Supplier<T> factory;
	
	/**
	 * Actual object (lazily created via factory).
	 */
	private T obj;
	
	/**
	 * Constructor.
	 */
	public UnsafeLazy(@Nonnull Supplier<T> factory)
	{
		this.factory = factory;
		this.obj = fakeNonNull(); // make compiler happy; it'll be initialized later
		
		if (nullable(factory) == null)
			throw new IllegalArgumentException("Factory may not be null.");
	}
	
	/**
	 * Gets the contained object.
	 * Object is created via provided factory on the first request and then
	 * cached.
	 * 
	 * Thread-UNSAFE!
	 */
	public T get()
	{
		if (factory != null)
		{
			obj = factory.get();
			factory = null;
		}
		
		return obj;
	}
}
