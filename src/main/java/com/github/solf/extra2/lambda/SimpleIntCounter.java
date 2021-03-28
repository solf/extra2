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

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.ToString;

/**
 * A simple int 'counter' class that doesn't provide any synchronization
 * guarantees.
 * <p>
 * NOT thread-safe.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
@ToString
public class SimpleIntCounter
{
	/**
	 * Current counter value.
	 */
	private int value;
	
	/**
	 * Constructor.
	 */
	public SimpleIntCounter(int initialValue)
	{
		this.value = initialValue;
	}
	
	/**
	 * Sets new value.
	 * 
	 * @return this for method chaining
	 */
	public SimpleIntCounter set(int newValue)
	{
		this.value = newValue;
		
		return this;
	}
	
	/**
	 * Gets current value.
	 */
	public int get()
	{
		return value;
	}
	
	/**
	 * Resets counter to zero.
	 * 
	 * @return this for method chaining
	 */
	public SimpleIntCounter reset()
	{
		value = 0;
		
		return this;
	}
	
	/**
	 * Increments counter value by 1 and returns value after this update.
	 */
	public int incrementAndGet()
	{
		value++;
		
		return value;
	}
	
	/**
	 * Decrements counter value by 1 and returns value after this update.
	 */
	public int decrementAndGet()
	{
		value--;
		
		return value;
	}
	
	/**
	 * Increments counter value by the given amount and returns value after this update.
	 */
	public int incrementByAndGet(int delta)
	{
		value += delta;
		
		return value;
	}
	
	/**
	 * Decrements counter value by the given amount and returns value after this update.
	 */
	public int decrementByAndGet(int delta)
	{
		value -= delta;
		
		return value;
	}
}
