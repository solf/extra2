/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.cache.exception;

import static com.github.solf.extra2.util.NullUtil.nullable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.solf.extra2.nullable.NullableOptional;
import com.github.solf.extra2.util.TypeUtil;

/**
 * Indicates that cache element with the particular key is not yet loaded.
 *
 * @param K key type
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class CacheElementNotYetLoadedException extends CacheElementNotLoadedException
{
	/**
	 * Key for which exception is generated.
	 */
	protected final Object key;
	
	/**
	 * Constructor.
	 * 
	 * @param key must be non-null
	 * @param parentExceptionSource if optional is given and it contains an
	 * 		exception -- it will be set as the parent exception
	 */
	public <@Nonnull K> CacheElementNotYetLoadedException(String cacheName, K key, @Nullable NullableOptional<?> parentExceptionSource)
	{
		super("Cache [" + cacheName + "] element is not yet loaded: {{==}}", // do not toString() key unnecessarily, might be expensive, instead do this in getMessage()
			parentExceptionSource == null ? null : parentExceptionSource.hasException() ? parentExceptionSource.getException() : null);  
		
		this.key = key;
		if (nullable(key) == null)
			throw new IllegalArgumentException("Key may not be null");
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
