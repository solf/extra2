/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.lambda;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.ToString;

/**
 * A simple long 'counter' class that doesn't provide any synchronization
 * guarantees.
 * <p>
 * NOT thread-safe.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
@ToString
public class SimpleLongCounter
{
	/**
	 * Current counter value.
	 */
	private long value;
	
	/**
	 * Constructor.
	 */
	public SimpleLongCounter(long initialValue)
	{
		this.value = initialValue;
	}
	
	/**
	 * Sets new value.
	 * 
	 * @return this for method chaining
	 */
	public SimpleLongCounter set(long newValue)
	{
		this.value = newValue;
		
		return this;
	}
	
	/**
	 * Gets current value.
	 */
	public long get()
	{
		return value;
	}
	
	/**
	 * Resets counter to zero.
	 * 
	 * @return this for method chaining
	 */
	public SimpleLongCounter reset()
	{
		value = 0;
		
		return this;
	}
	
	/**
	 * Increments counter value by 1 and returns value after this update.
	 */
	public long incrementAndGet()
	{
		value++;
		
		return value;
	}
	
	/**
	 * Decrements counter value by 1 and returns value after this update.
	 */
	public long decrementAndGet()
	{
		value--;
		
		return value;
	}
	
	/**
	 * Increments counter value by the given amount and returns value after this update.
	 */
	public long incrementByAndGet(long delta)
	{
		value += delta;
		
		return value;
	}
	
	/**
	 * Decrements counter value by the given amount and returns value after this update.
	 */
	public long decrementByAndGet(long delta)
	{
		value -= delta;
		
		return value;
	}
}
