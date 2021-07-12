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
package io.github.solf.extra2.cache.wbrb;

import java.util.MissingResourceException;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;

import io.github.solf.extra2.cache.wbrb.TestAbstractWBRBStringCache.TestCacheStorageEntry;
import io.github.solf.extra2.config.FlatConfiguration;
import io.github.solf.extra2.config.OverrideFlatConfiguration;
import io.github.solf.extra2.util.TypeUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * A version of cache that collects all updates in order to do read-applyUpdates-write
 * during the write in order to be sort-of 'atomic'.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class TestWBRBReadBeforeWriteCache extends 
	WBRBReadBeforeWriteCache<String, String, StringBuilder, StringBuilder, String, Character, Character>
{
	/**
	 * Read delay
	 */
	protected final long readDelayMs;
	/**
	 * Write delay
	 */
	protected final long writeDelayMs;
	
	/**
	 * Optional delay when processing items in the main queue
	 */
	@Getter @Setter
	protected volatile long mainQueueDelay = 0;
	
	/**
	 * Keep data here.
	 */
	@Getter
	protected final ConcurrentHashMap<String, TestCacheStorageEntry> storageDataMap;
	
	/**
	 * If non-null, logs event traces & similar to the given logger.
	 */
	protected volatile @Nullable Logger debugLogger;

	/**
	 * Constructor.
	 */
	public TestWBRBReadBeforeWriteCache(String cacheName, FlatConfiguration fConfig,
		boolean allowWritesWithoutPriorResync,
		long readDelayMs, long writeDelayMs)
		throws IllegalArgumentException,
		IllegalStateException,
		MissingResourceException,
		NumberFormatException
	{
		this(cacheName, fConfig, allowWritesWithoutPriorResync, new ConcurrentHashMap<>(), readDelayMs, writeDelayMs);
	}

	/**
	 * Constructor that allows using externally-provided storage data map -- e.g.
	 * so that multiple instances can compete for the single storage.
	 */
	public TestWBRBReadBeforeWriteCache(String cacheName, FlatConfiguration fConfig,
		boolean allowWritesWithoutPriorResync,
		ConcurrentHashMap<String, TestCacheStorageEntry> storageDataMap,
		long readDelayMs, long writeDelayMs)
		throws IllegalArgumentException,
		IllegalStateException,
		MissingResourceException,
		NumberFormatException
	{
		super(new WBRBConfig(new OverrideFlatConfiguration(fConfig).override("cacheName", cacheName)), allowWritesWithoutPriorResync);
		
		this.readDelayMs = readDelayMs;
		this.writeDelayMs = writeDelayMs;
		this.storageDataMap = storageDataMap;
	}

	@Override
	protected StringBuilder createNewCacheEntryAfterUpdatesWriteSplit(
		StringBuilder cacheData)
	{
		return cacheData;
	}

	@Override
	protected RBWWriteSplit splitForWriteRBW(
		String key, StringBuilder cacheData)
	{
		return new RBWWriteSplit(cacheData, cacheData.toString());
	}

	@Override
	protected void writeToStorageRBW(String key, String dataToWrite)
		throws InterruptedException
	{
		if (writeDelayMs > 0)
			Thread.sleep(writeDelayMs);
		
		getOrCreateStorageEntry(key).setValue(dataToWrite);
	}

	@Override
	protected StringBuilder convertFromCacheFormatToStorageData(String key,
		StringBuilder cacheData)
	{
		return cacheData;
	}

	@Override
	protected StringBuilder readFromStorage(String key, boolean isRefreshRead)
		throws InterruptedException
	{
		if (readDelayMs > 0)
			Thread.sleep(readDelayMs);
		
		StringBuilder result = new StringBuilder(getOrCreateStorageEntry(key).getValue());
		
		return result;
	}

	@Override
	protected Character convertToInternalUpdateFormatFromExternalUpdate(
		String key, Character externalUpdate)
	{
		return externalUpdate;
	}

	@Override
	protected StringBuilder convertToCacheFormatFromStorageData(String key,
		StringBuilder storageData)
	{
		return storageData;
	}

	@Override
	protected String convertFromCacheFormatToReturnValue(String key,
		StringBuilder cachedData)
	{
		return cachedData.toString();
	}

	@Override
	protected StringBuilder applyUpdate(StringBuilder cacheData,
		Character update)
	{
		return cacheData.append(update);
	}
	
	/**
	 * If non-null, logs event traces & similar to the given logger.
	 * <p>
	 * WARNING: setting this will log ALL messages to the given logger regardless
	 * of throttling settings; it may also duplicate messages as they are logged
	 * *in addition* to default logging mechanism.
	 * <p>
	 * Therefore this is best used with some file-log.
	 */
	public <T extends TestWBRBReadBeforeWriteCache> T setDebugLogger(@Nullable Logger debugLogger)
	{
		this.debugLogger = debugLogger;
		setDebugEventLogger(debugLogger);
		return TypeUtil.coerce(this);
	}
	

	@Override
	protected void spiUnknownLockLogMessage(WBRBCacheMessage msg,
		@Nullable Throwable exception,
		@Nonnull Object @Nonnull... args)
			throws InterruptedException
	{
		Logger logger = debugLogger;
		if (logger != null) // whether to log everything
			super.spiUnknownLockLogMessage_Plain(logger, msg, exception, args); 
		super.spiUnknownLockLogMessage(msg, exception, args);
	}

	/**
	 * Gets existing entry or creates new one in the storage 
	 */
	protected TestCacheStorageEntry getOrCreateStorageEntry(String key)
	{
		TestCacheStorageEntry result = storageDataMap.get(key);
		
		if (result == null)
		{
			result = new TestCacheStorageEntry("");
			TestCacheStorageEntry prev = storageDataMap.putIfAbsent(key, result);
			if (prev != null)
				result = prev; // use previously-added data
		}
		
		return result;
	}

	// Overriding this to cheat and synchronized-lock the underlying storage
	// entry so that tests can compare against expected results reliably.
	@Override
	protected void spiNoLockWriteToStorage(String key,
		WBRBWriteQueueEntry writeEntry)
		throws InterruptedException
	{
		TestCacheStorageEntry storageEntry = getOrCreateStorageEntry(key);
		
		// ensure no concurrent updates between instances
		synchronized(storageEntry)
		{
			super.spiNoLockWriteToStorage(key, writeEntry);
		}
	}

	@Override
	protected WriteBehindResyncInBackgroundCache<String, String, StringBuilder, StringBuilder, RBWWriteData<String, Character>, Character, Character>.WBRBMainQueueProcessingDecision spiWriteLockMakeMainQueueProcessingDecision(
		String key,
		WBRBCacheEntry cacheEntry,
		WBRBCachePayload payload)
		throws InterruptedException
	{
		WBRBMainQueueProcessingDecision result = super.spiWriteLockMakeMainQueueProcessingDecision(key, cacheEntry,
			payload);

		long delay = mainQueueDelay;
		if (delay > 0)
		{
			try
			{
				Thread.sleep(delay);
			} catch( InterruptedException e )
			{
				throw new RuntimeException(e);
			}
		}
		
		return result;
	}
	
	
	
}
