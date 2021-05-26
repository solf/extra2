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
package io.github.solf.extra2.cache.exception;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.NonNullByDefault;

import io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache;
import io.github.solf.extra2.util.TypeUtil;
import lombok.NonNull;

/**
 * Indicates that cache element with the particular key has failed to write
 * (typically final failure, after retries).
 * <p>
 * Note that this might not be currently used, but can be potentially useful
 * in e.g. 
 * {@link WriteBehindResyncInBackgroundCache#spiWriteLockMakeCacheWriteDecision_WriteFailedFinal(Object, io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
 *
 * @param K key type
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
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
