/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.cache.exception;

import static com.github.solf.extra2.util.NullUtil.nullable;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.solf.extra2.util.TypeUtil;

/**
 * Indicates that cache element has too many updates and they can no longer
 * be collected.
 *
 * @param K key type
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class CacheElementHasTooManyUpdates extends CacheIllegalStateException
{
	/**
	 * Key for which exception is generated.
	 */
	protected final Object key;
	
	/**
	 * Number of updates that was held when additional update was attempted.
	 */
	protected final int count;
	
	/**
	 * Constructor.
	 * 
	 * @param key must be non-null
	 */
	public <@Nonnull K> CacheElementHasTooManyUpdates(String cacheName, K key, int count)
	{
		super("Cache [" + cacheName + "] element has too many updates already [" + count + "]: {{==}}"); // do not toString() key unnecessarily, might be expensive, instead do this in getMessage() 
		
		this.key = key;
		if (nullable(key) == null)
			throw new IllegalArgumentException("Key may not be null");
		
		this.count = count;
	}
	
	/**
	 * Gets key for this exception.
	 */
	public <@Nonnull K> K getKey()
	{
		return TypeUtil.coerce(key);
	}

	@Override
	public String getMessage()
	{
		return super.getMessage().replace("{{==}}", key.toString());
	}
	
	
}
