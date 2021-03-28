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

import javax.annotation.DefaultLocation;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.ToString;

/**
 * Simple wrapper for an object.
 * <p>
 * Useful e.g. when lambda blocks / nested methods need to change something
 * available from the enclosing method/class.
 *
 * @author Sergey Olefir
 */
//Exclude TYPE_ARGUMENT as we will allow nullable type values.
@ParametersAreNonnullByDefault({DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE, DefaultLocation.FIELD, DefaultLocation.TYPE_BOUND, DefaultLocation.ARRAY_CONTENTS})
@ToString
public class ObjectWrapper<T>
{
	/**
	 * Stored object.
	 */
	private T value;
	
	/**
	 * Constructor.
	 */
	protected ObjectWrapper(T value)
	{
		this.value = value;
	}
	
	/**
	 * Gets value.
	 */
	public T get()
	{
		return value;
	}
	
	/**
	 * Sets new value.
	 * 
	 * @return this for method chaining
	 */
	public ObjectWrapper<T> set(T newValue)
	{
		value = newValue;
		
		return this;
	}
	
	/**
	 * Constructs wrapper for the given argument.
	 */
	public static <T> ObjectWrapper<T> of(T value)
	{
		return new ObjectWrapper<T>(value);
	}
}
