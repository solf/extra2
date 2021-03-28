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
package com.github.solf.extra2.cache.exception;

import static com.github.solf.extra2.util.NullUtil.nullable;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.solf.extra2.util.TypeUtil;

/**
 * Indicates that cache element with the particular key has failed to load.
 *
 * @param K key type
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class CacheElementFailedResyncException extends CacheIllegalExternalStateException
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
	public <@Nonnull K> CacheElementFailedResyncException(String cacheName, K key)
	{
		super("Cache [" + cacheName + "] element failed to load: {{==}}"); // do not toString() key unnecessarily, might be expensive, instead do this in getMessage() 
		
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
