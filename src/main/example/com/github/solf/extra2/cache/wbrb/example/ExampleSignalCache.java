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
package com.github.solf.extra2.cache.wbrb.example;

import java.nio.charset.Charset;
import java.util.MissingResourceException;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.solf.extra2.cache.wbrb.WBRBConfig;
import com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache;
import com.github.solf.extra2.nullable.NullableOptional;

import lombok.Getter;

/**
 * Example implementation of the signal cache -- it updates either 'hi' or 'lo'
 * value depending on initialization parameters.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class ExampleSignalCache extends WriteBehindResyncInBackgroundCache<
	Long /*key*/, 
	ExSignalSample /*returned value*/, 
	ExSignalCache /*value stored in cache*/,
	ExSignalStorage /*value read from storage*/, 
	String /*value written to storage */, 
	Integer, /*used to update stored data externally*/ // use Integer instead of Character for demonstration purposes
	Character /*used to update stored data internally*/>
{
	/**
	 * Charset used for encoding to/from String.
	 */
	private static Charset ENCODING_CHARSET = Charset.forName("UTF-16");
	
	/**
	 * true -- this cache is for the modification of 'hi' signal part,
	 * false -- it is for 'lo' part
	 */
	@Getter
	private final boolean hiCache;
	
	/**
	 * Underlying 'storage' for this cache.
	 */
	private final ConcurrentHashMap<Long, ExSignalStorage> underlyingStorage;

	/**
	 * Constructor
	 * 
	 * @param hiCache true -- this cache modifies 'hi' value, false -- 'lo' value
	 */
	protected ExampleSignalCache(WBRBConfig config, boolean hiCache, ConcurrentHashMap<Long, ExSignalStorage> underlyingStorage)
		throws IllegalArgumentException,
		IllegalStateException,
		MissingResourceException,
		NumberFormatException
	{
		super(config);
		
		this.hiCache = hiCache;
		this.underlyingStorage = underlyingStorage;
	}
	
	/**
	 * Gets underlying signal storage for the given key.
	 */
	private ExSignalStorage getSignalStorage(Long key)
	{
		return underlyingStorage.computeIfAbsent(key, k -> new ExSignalStorage());
	}

	@Override
	protected ExSignalStorage readFromStorage(Long key, boolean isRefreshRead)
		throws InterruptedException
	{
		return getSignalStorage(key);
	}

	@Override
	protected Character convertToInternalUpdateFormatFromExternalUpdate(
		Long key, Integer externalUpdate)
	{
		return (char)externalUpdate.intValue();
	}

	@Override
	protected ExSignalCache convertToCacheFormatFromStorageData(Long key,
		ExSignalStorage storageData)
	{
		byte[] our;
		byte[] other;
		if (hiCache)
		{
			our = storageData.getHi();
			other = storageData.getLo();
		}
		else
		{
			our = storageData.getLo();
			other = storageData.getHi();
		}
		
		return new ExSignalCache(
			new StringBuilder(new String(our, ENCODING_CHARSET)),
			new String(other, ENCODING_CHARSET)
		);
	}

	@Override
	protected ExSignalSample convertFromCacheFormatToReturnValue(Long key,
		ExSignalCache cachedData)
	{
		if (hiCache)
			return new ExSignalSample(cachedData.getOurSignalValue().toString(), cachedData.getOtherSignalValue());
		else
			return new ExSignalSample(cachedData.getOtherSignalValue(), cachedData.getOurSignalValue().toString());
	}

	@Override
	protected ExSignalCache applyUpdate(ExSignalCache cacheData,
		Character update)
	{
		cacheData.getOurSignalValue().append(update);
		return cacheData;
	}

	@Override
	protected WriteBehindResyncInBackgroundCache<Long, ExSignalSample, ExSignalCache, ExSignalStorage, String, Integer, Character>.WriteSplit splitForWrite(
		Long key, ExSignalCache cacheData,
		NullableOptional<String> previousFailedWriteData)
	{
		// Since we always overwrite 'our' value entirely, we don't care if 
		// previous operation had failures in 'previousFailedWriteData' --
		// we write entire current value regardless.
		// Other cache implementations may actually care about 'previousFailedWriteData'
		return new WriteSplit(
			cacheData, // we continue to use the same element for further cache updates 
			cacheData.getOurSignalValue().toString() // this is what is sent to the actual write operation
		);
	}

	@Override
	protected void writeToStorage(Long key, String dataToWrite)
		throws InterruptedException
	{
		ExSignalStorage stored = getSignalStorage(key);
		byte[] bytes = dataToWrite.getBytes(ENCODING_CHARSET);
		
		if (hiCache)
			stored.setHi(bytes);
		else
			stored.setLo(bytes);
	}
	
	
}
