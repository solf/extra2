/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.cache.wbrb;

import java.util.MissingResourceException;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import site.sonata.extra2.config.FlatConfiguration;
import site.sonata.extra2.config.OverrideFlatConfiguration;
import site.sonata.extra2.nullable.NullableOptional;
import site.sonata.extra2.util.TypeUtil;

/**
 * Base abstract class for writing test cache implementations that use String key;
 * use Character as update unit and store/read String from the underlying storage.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public abstract class TestAbstractWBRBStringCache extends WriteBehindResyncInBackgroundCache<String, String, StringBuilder, String, String, Character, Character>
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
	 * Keep data here.
	 */
	@Getter
	protected final ConcurrentHashMap<String, TestCacheStorageEntry> storageDataMap = new ConcurrentHashMap<>();
	
	/**
	 * If non-null, logs event traces & similar to the given logger.
	 */
	protected volatile @Nullable Logger debugLogger;
	
	/**
	 * Used as synchronization point.
	 */
	@RequiredArgsConstructor
	@ToString
	protected static class TestCacheStorageEntry
	{
		@NonNull // make required & non-null
		@Getter
		@Setter
		private volatile String value;
	}

	/**
	 * Constructor.
	 */
	public TestAbstractWBRBStringCache(String cacheName, FlatConfiguration fConfig,
		long readDelayMs, long writeDelayMs)
		throws IllegalArgumentException,
		IllegalStateException,
		MissingResourceException,
		NumberFormatException
	{
		super(new WBRBConfig(new OverrideFlatConfiguration(fConfig).override("cacheName", cacheName)));
		
		this.readDelayMs = readDelayMs;
		this.writeDelayMs = writeDelayMs;
	}
	
	/**
	 * Applies changes to the stored data.
	 */
	protected abstract void spiSynchronized_applyWrite(String writeData, TestCacheStorageEntry entry);

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
	
	@Override
	protected String readFromStorage(String key, boolean isRefreshRead)
		throws InterruptedException
	{
		if (readDelayMs > 0)
			Thread.sleep(readDelayMs);
		
		return getOrCreateStorageEntry(key).getValue();
	}

	@Override
	protected Character convertToInternalUpdateFormatFromExternalUpdate(
		String key, Character externalUpdate)
	{
		return externalUpdate;
	}
	
	@Override
	protected StringBuilder convertToCacheFormatFromStorageData(String key, String storageData)
	{
		return new StringBuilder(storageData);
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

	@Override
	protected WriteSplit splitForWrite(
		String key, StringBuilder cacheData,
		NullableOptional<String> previousFailedWriteData)
	{
		return new WriteSplit(cacheData, cacheData.toString());
	}
	
	@Override
	protected void writeToStorage(String key, String dataToWrite)
		throws InterruptedException
	{
		if (writeDelayMs > 0)
			Thread.sleep(writeDelayMs);
		
		TestCacheStorageEntry entry = getOrCreateStorageEntry(key);
		synchronized(entry)
		{
			spiSynchronized_applyWrite(dataToWrite, entry);
		}
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
	public <T extends TestAbstractWBRBStringCache> T setDebugLogger(@Nullable Logger debugLogger)
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
}
