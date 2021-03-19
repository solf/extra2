/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.cache.exception;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache;
import com.github.solf.extra2.util.TypeUtil;

import lombok.NonNull;

/**
 * Indicates that cache element with the particular key has failed to write
 * (typically final failure, after retries).
 * <p>
 * Note that this might not be currently used, but can be potentially useful
 * in e.g. 
 * {@link WriteBehindResyncInBackgroundCache#spiWriteLockMakeCacheWriteDecision_WriteFailedFinal(Object, com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
 *
 * @param K key type
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class CacheElementFailedWriteException extends CacheIllegalExternalStateException
{
	/**
	 * Key for which exception is generated.
	 */
	protected final Object key;

	/**
	 * Constructor.
	 * 
	 * @param key must be non-null
	 */
	public <@Nonnull K> CacheElementFailedWriteException(String cacheName, @NonNull K key)
	{
		this(cacheName, key, null);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param key must be non-null
	 */
	public <@Nonnull K> CacheElementFailedWriteException(String cacheName, @NonNull K key, @Nullable String msg)
	{
		super("Cache [" + cacheName + "] element failed to write " + (msg == null ? "" : "[" + msg + "]") + ": {{==}}"); // do not toString() key unnecessarily, might be expensive, instead do this in getMessage() 
		
		this.key = key;
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
