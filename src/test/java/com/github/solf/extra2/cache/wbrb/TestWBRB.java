/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.cache.wbrb;

import static com.github.solf.extra2.testutil.AssertExtra.assertBetweenInclusive;
import static com.github.solf.extra2.testutil.AssertExtra.assertContains;
import static com.github.solf.extra2.testutil.AssertExtra.assertContainsIgnoreCase;
import static com.github.solf.extra2.testutil.AssertExtra.assertFails;
import static com.github.solf.extra2.testutil.AssertExtra.assertFailsWithSubstring;
import static com.github.solf.extra2.testutil.AssertExtra.assertGreater;
import static com.github.solf.extra2.testutil.AssertExtra.assertGreaterOrEqual;
import static com.github.solf.extra2.testutil.AssertExtra.assertLess;
import static com.github.solf.extra2.testutil.AssertExtra.assertLessOrEqual;
import static com.github.solf.extra2.util.NullUtil.fakeNonNull;
import static com.github.solf.extra2.util.NullUtil.nn;
import static com.github.solf.extra2.util.NullUtil.nnChecked;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.ArrayUtils;
import org.javatuples.Pair;
import org.javatuples.Quartet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.github.solf.extra2.cache.wbrb.TestAbstractWBRBStringCache.TestCacheStorageEntry;
import com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheControlState;
import com.github.solf.extra2.concurrent.Latch;
import com.github.solf.extra2.concurrent.WAThreadPoolExecutor;
import com.github.solf.extra2.concurrent.exception.WAInterruptedException;
import com.github.solf.extra2.config.Configuration;
import com.github.solf.extra2.config.FlatConfiguration;
import com.github.solf.extra2.config.OverrideFlatConfiguration;
import com.github.solf.extra2.lambda.TriFunction;
import com.github.solf.extra2.nullable.NullableOptional;
import com.github.solf.extra2.testutil.TestUtil;
import com.github.solf.extra2.testutil.TestUtil.AsyncTestRunner;
import com.github.solf.extra2.thread.ExitableThread;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tests for {@link WriteBehindResyncInBackgroundCache}
 * FIXME needs tests for write failures; maybe use key patterns to distinguish which writes should fully fail
 *
 * @author Sergey Olefir
 */
@Slf4j
@ParametersAreNonnullByDefault
public class TestWBRB
{
	/**
	 * Logs directly to file to log various spam if needed.
	 */
	private static final Logger testFileLog = LoggerFactory.getLogger("testFileLogger");
	
	/**
	 * Random generator.
	 */
	private final Random random = new Random();
	
	/**
	 * Single update entry.
	 */
	@RequiredArgsConstructor
	private static class UpdateEntry
	{
		/**
		 * Key.
		 */
		@Getter
		private final String key;
		
		/**
		 * Update
		 */
		@Getter
		private final char update;
	}
	
	/**
	 * Warm up stuff before testing for e.g. timing issues particularly under
	 * debug.
	 */
	@BeforeClass
	public void warmUp() throws InterruptedException
	{
		log.info("Running code warm up...");
		final String baseTestName = "warmUp";
		int testIndex = 1;
		
		FlatConfiguration baseConfig = Configuration.fromPropertiesFile("wbrb/wbrb-default.properties");
		
		
		{
			final String testName = baseTestName + (testIndex++);
			
			// Control debug logging/event tracing to file.
			final boolean debugLogging;
			{
				boolean f = false;
//				f = true; {} // uncomment this to enable debug logging; empty block produces warning in order to not forget to re-comment
				debugLogging = f;
			}
			
			// Tests initial read failure with immediate read failures
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			config.override("eventNotificationEnabled", "" + debugLogging); // set to true to enable event logging for debugging
			
			final TestWBRBReadBeforeWriteCache cache = new TestWBRBReadBeforeWriteCache(testName, config, false, 20, 20)
				.start();
			if (debugLogging)
			{
				cache.setDebugLogger(testFileLog);
			}
			
			AtomicBoolean hadAsyncFailures = new AtomicBoolean(false);
			final BlockingQueue<UpdateEntry> updatesQueue = new LinkedBlockingQueue<>();
			AtomicInteger processed = new AtomicInteger(0);
			
			WAThreadPoolExecutor pool = new WAThreadPoolExecutor(testName + "-executors", true);
			for (int i = 0; i < 20; i++)
			{
				pool.waSubmit(() -> {
					try
					{
						while (true)
						{
							UpdateEntry update = updatesQueue.take();
							final String key = update.getKey();
							cache.readForOrException(key, 3500);
							cache.writeIfCachedOrException(key, update.getUpdate());
							
							processed.incrementAndGet();
						}
					} catch( InterruptedException e )
					{
						throw new WAInterruptedException(e);
					} catch (Exception e)
					{
						log.error("[async executor] " + testName + " failed: " + e, e);
						hadAsyncFailures.set(true);
					}
					
				});
			}
			
			final int updatesCount = 20;
			for (int i = 0; i < updatesCount; i++)
			{
				final String key = "key" + i;
				updatesQueue.put(new UpdateEntry(key, 'u'));
			}
			
			for (int i = 0; i < 150; i++)
			{
				assertFalse(hadAsyncFailures.get(), "Had asynchronous processing failures, check logs (search for '[async executor]')");
				
				if (processed.get() == updatesCount)
					break;
				
				Thread.sleep(20); // delay for spooling operations
			}
			assertEquals(processed.get(), updatesCount);
			
			assert cache.shutdownFor(3000) : "Cache failed to shutdown in time, still has items: " + cache.inflightMap.size();

			assertFalse(hadAsyncFailures.get(), "Had asynchronous processing failures, check logs (search for '[async executor]')");
			
			assertEquals(cache.inflightMap.size(), 0); 
			
			pool.shutdown();
			
			log.info("Warm up done, proceeding...");
		}
	}
	
	/**
	 * Tests simple (trivial) success path. 
	 */
	@Test
	public void testSimpleSuccessPath()
	{
		TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(
			"testSimpleSuccessPath-1", 
			Configuration.fromPropertiesFile("wbrb/wbrb-default.properties"), 
			50, 50)
			.start();
		
		final String key = "a-key";
		
		assertEquals("", cache.readForOrException(key, 500));
		
		cache.writeIfCached(key, 'u');
		
		assertTrue(cache.shutdownFor(3000), "Cache failed to shutdown in time, still has items: " + cache.inflightMap.size());
		
		assertStorageMapContentsExactlyEquals(cache.getStorageDataMap(), key, "u");
		
		// Check some monitoring statuses.
		WBRBStatus status = cache.getStatus(0);
		assertEquals(status.getCurrentCacheSize(), 0);
		assertEquals(status.getMainQueueSize(), 0);
		assertEquals(status.getReturnQueueSize(), 0);
		assertEquals(status.getReadQueueSize(), 0);
		assertEquals(status.getWriteQueueSize(), 0);

		assertEquals(status.getReadThreadPoolActiveThreads(), 0);
		assertEquals(status.getWriteThreadPoolActiveThreads(), 0);
		
		assertEquals(status.getConfigMainQueueCacheTimeMs(), 4000);
		assertEquals(status.getConfigReturnQueueCacheTimeMinMs(), 1000);
		assertEquals(status.getConfigMainQueueMaxTargetSize(), 1000);
		assertEquals(status.getConfigMaxCacheElementsHardLimit(), 2000);
		
		assertEquals(status.getStorageReadQueueProcessedItems(), 1);
		assertEquals(status.getStorageReadTotalAttempts(), 1);
		assertEquals(status.getStorageReadTotalSuccesses(), 1);
		assertEquals(status.getStorageReadTotalFailures(), 0);
		assertEquals(status.getStorageReadRefreshAttempts(), 0);
		assertEquals(status.getStorageReadRefreshSuccesses(), 0);
		assertEquals(status.getStorageReadRefreshFailures(), 0);
		assertEquals(status.getStorageReadRefreshTooLateCount(), 0);
		assertEquals(status.getStorageReadRefreshDataNotUsedCount(), 0);
		assertEquals(status.getStorageReadInitialAttempts(), 1);
		assertEquals(status.getStorageReadInitialSuccesses(), 1);
		assertEquals(status.getStorageReadInitialFailures(), 0);

		assertEquals(status.getStorageWriteQueueProcessedItems(), 1);
		assertEquals(status.getStorageWriteAttempts(), 1);
		assertEquals(status.getStorageWriteSuccesses(), 1);
		assertEquals(status.getStorageWriteFailures(), 0);
		
		assertEquals(status.getMainQueueProcessedItems(), 1);
		{
			long dur = status.getMainQueueLastItemInQueueDurationMs();
			assertGreater(dur, 0L);
			assertLess(dur, 1000L);
		}
		assertEquals(status.getMainQueueSentWrites(), 1);
		assertEquals(status.getMainQueueExpiredFromCacheCount(), 0);
		assertEquals(status.getMainQueueRemovedFromCacheCount(), 0);
		assertEquals(status.getMainQueueRequeueToMainQueueCount(), 0);
		assertEquals(status.getMainQueueNotAllOkCount(), 0);
		
		assertEquals(status.getReturnQueueProcessedItems(), 1);
		{
			long dur = status.getReturnQueueLastItemInQueueDurationMs();
			assertGreater(dur, 0L);
			assertLess(dur, 1000L);
		}
		assertEquals(status.getReturnQueueScheduledResyncs(), 0);
		assertEquals(status.getReturnQueueDoNothingCount(), 0);
		assertEquals(status.getReturnQueueExpiredFromCacheCount(), 1);
		assertEquals(status.getReturnQueueRemovedFromCacheCount(), 0);
		assertEquals(status.getReturnQueueRequeueToReturnQueueCount(), 0);

		assertEquals(status.getCheckCacheAttemptsNoDedup(), 1);
		assertEquals(status.getCheckCachePreloadAttempts(), 0);
		assertEquals(status.getCheckCachePreloadCacheHit(), 0);
		assertEquals(status.getCheckCachePreloadCacheFullExceptionCount(), 0);
		assertEquals(status.getCheckCacheReadAttempts(), 1);
		assertEquals(status.getCheckCacheReadCacheHit(), 0);
		assertEquals(status.getCheckCacheReadCacheFullExceptionCount(), 0);
		assertEquals(status.getCheckCacheTotalCacheFullExceptionCount(), 0);
		assertEquals(status.getCheckCacheNullKeyCount(), 0);

		assertEquals(status.getCacheReadAttempts(), 1);
		assertEquals(status.getCacheReadTimeouts(), 0);
		assertEquals(status.getCacheReadErrors(), 0);
		
		assertEquals(status.getCacheWriteAttempts(), 1);
		assertEquals(status.getCacheWriteElementNotPresentCount(), 0);
		assertEquals(status.getCacheWriteErrors(), 0);
		assertEquals(status.getCacheWriteTooManyUpdates(), 0);
		
		assertEquals(status.getMsgWarnCount(), 0);
		assertEquals(status.getMsgExternalWarnCount(), 0);
		assertEquals(status.getMsgExternalErrorCount(), 0);
		assertEquals(status.getMsgExternalDataLossCount(), 0);
		assertEquals(status.getMsgErrorCount(), 0);
		assertEquals(status.getMsgFatalCount(), 0);
		assertEquals(status.getMsgTotalWarnOrHigherCount(), 0);
		assertEquals(status.getMsgTotalErrorOrHigherCount(), 0);
}

	/**
	 * Asserts that storage map has specific content.
	 */
	private void assertStorageMapContentsExactlyEquals(ConcurrentHashMap<String, TestCacheStorageEntry> argStorageMap,
		Map<String, String> expectedContent)
	{
		assertStorageMapContents(argStorageMap, expectedContent, (key, a, b) -> a.equals(b));
	}
	

	/**
	 * Asserts that storage map has specific content -- it must contain all the
	 * same characters as expected content though not necessarily in the same order.
	 */
	private void assertStorageMapContentsHasSameUpdates(ConcurrentHashMap<String, TestCacheStorageEntry> argStorageMap,
		Map<String, String> expectedContent)
	{
		assertStorageMapContents(argStorageMap, expectedContent, (key, actual, expected) -> {
			return stringCharactersMatch(actual, expected);
		});
	}

	/**
	 * Checks that characters in two strings match regardless of the order.
	 */
	private @Nonnull Boolean stringCharactersMatch(@Nonnull String actual,
		@Nonnull String expected)
	{
		char[] arr1 = actual.toCharArray();
		char[] arr2 = expected.toCharArray();
		Arrays.sort(arr1);
		Arrays.sort(arr2);
		return Arrays.equals(arr1, arr2);
	}
	
	/**
	 * Asserts that storage map has specific content.
	 * 
	 * @param comparator arg order: key, actual, expected
	 */
	private void assertStorageMapContents(ConcurrentHashMap<String, TestCacheStorageEntry> argStorageMap,
		Map<String, String> expectedContent, TriFunction<String, String, String, Boolean> comparator)
	{
		HashMap<String, TestCacheStorageEntry> storageMap = new HashMap<>(argStorageMap); // make a copy so we ignore subsequent changes
		HashMap<String, TestCacheStorageEntry> data = new HashMap<>(storageMap);
		
		for (Entry<String, String> entry : expectedContent.entrySet())
		{
			String key = entry.getKey();
			String value = entry.getValue();
			TestCacheStorageEntry dataValue = data.remove(key);
			
			// Use standard assert for compiler's sake
			assert dataValue != null : "Missing key [" + key + "] in storage map: " + storageMap;
			
			@NonNull String actualValue = dataValue.getValue();
			if (!comparator.apply(key, actualValue, value))
				throw new AssertionError("Mimatched value in key [" + key + "], expected: " + value + ", got: " + actualValue + "; in storage map: " + storageMap); 
		}
		
		assert data.isEmpty() : "Extra values in storage map: " + data;
	}
	
	
	/**
	 * Asserts that storage map has specific content.
	 */
	private void assertStorageMapContentsExactlyEquals(ConcurrentHashMap<String, TestCacheStorageEntry> argStorageMap,
		String... expectedKeyValuePairs)
	{
		Map<String, String> pairsMap = new HashMap<>();
		for (int i = 0; i < expectedKeyValuePairs.length; i += 2)
		{
			String key = expectedKeyValuePairs[i];
			String value = expectedKeyValuePairs[i + 1];
			
			assert pairsMap.put(key, value) == null : "Duplicate key: " + key;
		}
		
		assertStorageMapContentsExactlyEquals(argStorageMap, pairsMap);
	}
	
	/**
	 * Tests cache by generating a lot of reads/update accesses w/ no 
	 * storage-related failures.
	 */
	@Test
	public void testLotsOfAccessWithNoFailures()
		throws InterruptedException
	{
		final String name = "testLotsOfAccessWithNoFailures";
		
		OverrideFlatConfiguration config = new OverrideFlatConfiguration("wbrb/wbrb-default.properties");
		config.override("maxUpdatesToCollect", "2000"); // we are generating A LOT of updates to some values.
		config.override("maxSleepTime", ""); // use default maxSleepTime for this test
		config.override("readQueueBatchingDelay", "30ms");
		config.override("writeQueueBatchingDelay", "30ms");
		
		final AtomicInteger readBatchCounter = new AtomicInteger(0);
		final AtomicInteger writeBatchCounter = new AtomicInteger(0);
		
		final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(
			name, config, 50, 50)
			{
				@Override
				protected void spiNoLockReadBatchDelayExpired()
					throws InterruptedException
				{
					readBatchCounter.incrementAndGet();
				}

				@Override
				protected void spiNoLockWriteBatchDelayExpired()
					throws InterruptedException
				{
					writeBatchCounter.incrementAndGet();
				}
			}
			.start();
		
		final BlockingQueue<UpdateEntry> updatesQueue = new LinkedBlockingQueue<>();
		
		// Launch all the executors
		WAThreadPoolExecutor pool = new WAThreadPoolExecutor(name + "-executors", true);
		for (int i = 0; i < 200; i++)
		{
			pool.waSubmit(() -> {
				try
				{
					while (true)
					{
						UpdateEntry update = updatesQueue.take();
						final String key = update.getKey();
						cache.readForOrException(key, 250);
						Thread.sleep(100);
						cache.writeIfCachedOrException(key, update.getUpdate());
					}
				} catch( InterruptedException e )
				{
					throw new WAInterruptedException(e);
				}
			});
		}
		
		Map<String, String> dataMap = new HashMap<String, String>();
		
		final int stepAmount = 50;
		
		// Single update
		for (int i = 0; i < stepAmount; i++)
			generateXUpdatesWithNewKey(1, dataMap);
		
		// Two updates
		for (int i = 0; i < stepAmount; i++)
			generateXUpdatesWithNewKey(2, dataMap);
		
		// Three updates
		for (int i = 0; i < stepAmount; i++)
			generateXUpdatesWithNewKey(3, dataMap);
		
		// 10 updates
		for (int i = 0; i < stepAmount; i++)
			generateXUpdatesWithNewKey(10, dataMap);
		
		// 50 updates
		for (int i = 0; i < stepAmount; i++)
			generateXUpdatesWithNewKey(50, dataMap);
		
		// 100 updates
		for (int i = 0; i < stepAmount; i++)
			generateXUpdatesWithNewKey(100, dataMap);
		
		// 1000 updates
		generateXUpdatesWithNewKey(1000, dataMap);
		
		// 2000 updates
		generateXUpdatesWithNewKey(2000, dataMap);
		
		ArrayList<UpdateEntry> updatesList = generateRandomizedUpdateList(dataMap);
		
		// Send updates
		final long startTime = System.currentTimeMillis();
		final long duration = 10000;
		spoolUpdatesList(name, true, updatesQueue, updatesList, startTime, duration);
		
		final int firstReadBatchCount = readBatchCounter.get();
		final int firstWriteBatchCount = writeBatchCounter.get();
		assertGreaterOrEqual(firstReadBatchCount, 50); // ought to be 100, but there can be issues under load
		assertGreaterOrEqual(firstWriteBatchCount, 50);
		
		assert cache.shutdownFor(3000) : "Cache failed to shutdown in time, still has items: " + cache.inflightMap.size();

		int lastReadBatchCount = readBatchCounter.get();
		{
			int delta = lastReadBatchCount - firstReadBatchCount;
			assertGreaterOrEqual(delta, 0);
			assertLessOrEqual(delta, 2);
		}
		assertGreater(writeBatchCounter.get(), firstWriteBatchCount);
		
		log.info("Waiting done, checking result");
		
		assertEquals(cache.inflightMap.size(), 0); 
		
		assertStorageMapContentsHasSameUpdates(cache.getStorageDataMap(), dataMap);
		
		Thread.sleep(500); // need some delay before checking that all cache threads terminated
		assertEquals(cache.threadGroup.activeCount(), 0, "Still active threads in cache thread group: " + cache.threadGroup.activeCount());
		
		pool.shutdownNow();
	}

	/**
	 * Spools given updates list into updates queue starting at the specified
	 * time and taking the specified time.
	 * <p>
	 * The events are organized into batches with 100 ms duration between batches.
	 * 
	 * @param waitForEmptyQueue if true, then waits at the end up to 5 seconds
	 * 		for the updates queue to become empty (fails if it is still not empty) 
	 */
	private void spoolUpdatesList(String logPrefix, boolean waitForEmptyQueue, final BlockingQueue<UpdateEntry> updatesQueue,
		List<UpdateEntry> updatesList, final long startTime,
		final long duration)
		throws InterruptedException
	{
		final int totalUpdates = updatesList.size();
		
		log.info("[{}] Total updates: {}", logPrefix, totalUpdates);
		
		int count = 0;
		final long batchDuration = 100; // however long we wait for batching (bunching up) the updates
		long logTime = 0;
		for (UpdateEntry update : updatesList)
		{
			count++;
			int targetEntry = (int)((System.currentTimeMillis() - startTime) * totalUpdates / duration);
			
			if (targetEntry < count)
			{
				Thread.sleep(batchDuration); // Sleep to organize some 'batching' of updates
				long now = System.currentTimeMillis();
				if ((now - logTime) >= 1000)
				{
					log.info("[{}] Sent so far: {}, queue size: {}", new Object[] {logPrefix, count, updatesQueue.size()});
					logTime = now;
				}
			}
			
			updatesQueue.put(update);
		}
		
		log.info("[{}] All updates sent: {}, queue size: {}", new Object[] {logPrefix, count, updatesQueue.size()});
		
		if (waitForEmptyQueue)
		{
			for (int i = 0; i < 10; i++)
			{
				if (updatesQueue.size() == 0)
					break;
				Thread.sleep(500);
			}
			assert updatesQueue.isEmpty() : "Updates queue isn't empty even after waiting for 5 seconds: " + updatesQueue.size();
			
			Thread.sleep(500); // Some more delay to make sure threads that picked up items from update queue had a chance to finish.
			
			log.info("[{}] Updates queue is now empty.", logPrefix);
		}
	}
	
	/**
	 * Generates a new 4-lower-case-letter key.
	 * 
	 * @param keysToAvoid if specified, avoids using keys already present in the
	 * 		map
	 */
	private String generateKey(String keyPrefix, @Nullable Map<String, ?> keysToAvoid)
	{
		final int length = 4;
		final int cap = 'z' - 'a' + 1;
		final int prefixLength = keyPrefix.length();
		
		while(true)
		{
			StringBuilder sb = new StringBuilder(length + prefixLength + (prefixLength > 0 ? 1 : 0));
			sb.append(keyPrefix);
			if (prefixLength > 0)
				sb.append('_');
			for (int i = 0; i < length; i++)
			{
				sb.append((char)('a' + random.nextInt(cap)));
			}
			
			String result = sb.toString();
			if ((keysToAvoid == null) || (!keysToAvoid.containsKey(result)))
				return result;
		}
	}
	
	/**
	 * Generates a single 'update'
	 */
	private char generateSingleUpdate(boolean upperCaseCharactersOnly)
	{
		final char start;
		if (upperCaseCharactersOnly)
			start = 'A';
		else
			start = '0';
		
		final int cap = 'Z' - start + 1;
		
		return (char)(start + random.nextInt(cap));
	}

	
	/**
	 * Generates a new entry in the data map with value length corresponding to
	 * update count, characters in range 0-Z are used
	 * 
	 * @param keyPrefixArray if specified (only 1 element must be specified!), then
	 * 		used as key prefix instead of auto-generating one from updateCount
	 */
	private void generateXUpdatesWithNewKey(int updateCount, Map<String, String> dataMap, String... keyPrefixArray)
	{
		generateXUpdatesWithNewKey(updateCount, dataMap, false, keyPrefixArray);
	}
	
	/**
	 * Generates a new entry in the data map with value length corresponding to
	 * update count, characters in range 0-Z are used
	 * 
	 * @param upperCaseCharactersOnly if specified, only upper-case characters
	 * 		are generated as possible updates
	 * @param keyPrefixArray if specified (only 1 element must be specified!), then
	 * 		used as key prefix instead of auto-generating one from updateCount
	 */
	private void generateXUpdatesWithNewKey(int updateCount, Map<String, String> dataMap, boolean upperCaseCharactersOnly, String... keyPrefixArray)
	{
		if (keyPrefixArray.length > 1)
			throw new IllegalArgumentException("keyPrefixArray may have 0-1 elements only, got: " + keyPrefixArray);
		
		final String keyPrefix;
		if (keyPrefixArray.length > 0)
			keyPrefix = keyPrefixArray[0];
		else
			keyPrefix = "u" + updateCount;
		
		String key = generateKey(keyPrefix, dataMap);
		StringBuilder sb = new StringBuilder(updateCount);
		for (int i = 0; i < updateCount; i++)
			sb.append(generateSingleUpdate(upperCaseCharactersOnly));
		
		dataMap.put(key, sb.toString());
	}
	
	/**
	 * Generates randomized update list from the given data map.
	 */
	private ArrayList<UpdateEntry> generateRandomizedUpdateList(Map<String, String> dataMap)
	{
		ArrayList<UpdateEntry> result = new ArrayList<>(dataMap.size() * 10);
		
		for (Entry<String, String> entry : dataMap.entrySet())
		{
			final String key = entry.getKey();
			
			for (char update : entry.getValue().toCharArray())
			{
				result.add(new UpdateEntry(key, update));
			}
		}
		
		Collections.shuffle(result);
		
		return result;
	}
	
	
	/**
	 * Tests cache by generating a lot of reads/update accesses w/ no 
	 * storage-related failures + tests that resync happens properly
	 */
	@Test
	public void testLotsOfResyncWithNoFailures()
		throws InterruptedException
	{
		final String name = "testLotsOfResyncWithNoFailures";
		final String writeMarker = "###";
		
		OverrideFlatConfiguration config = new OverrideFlatConfiguration("wbrb/wbrb-default.properties");
		config.override("maxSleepTime", ""); // use default maxSleepTime for this test
		
		final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(
			name, config, 50, 50)
			{
				@Override
				protected void spiSynchronized_applyWrite(String writeData,
					TestCacheStorageEntry entry)
				{
					super.spiSynchronized_applyWrite(writeData + writeMarker, entry); // Append write markers which should be read during re-sync
				}
			}
			.start();
		
		final BlockingQueue<UpdateEntry> updatesQueue = new LinkedBlockingQueue<>();
		
		// Launch all the executors
		WAThreadPoolExecutor pool = new WAThreadPoolExecutor(name + "-executors", true);
		for (int i = 0; i < 200; i++)
		{
			pool.waSubmit(() -> {
				try
				{
					while (true)
					{
						UpdateEntry update = updatesQueue.take();
						final String key = update.getKey();
						cache.readForOrException(key, 250);
						Thread.sleep(100);
						cache.writeIfCachedOrException(key, update.getUpdate());
					}
				} catch( InterruptedException e )
				{
					throw new WAInterruptedException(e);
				}
			});
		}

		final int stepAmount = 50;
		
		Map<String, String> update1 = new HashMap<String, String>();
		Map<String, String> update2 = new HashMap<String, String>();
		Map<String, String> update10 = new HashMap<String, String>();
		
		
		// The keys below will be distinct between maps due to prefix
		
		// Single update
		for (int i = 0; i < stepAmount; i++)
			generateXUpdatesWithNewKey(1, update1);
		// Two updates
		for (int i = 0; i < stepAmount; i++)
			generateXUpdatesWithNewKey(2, update2);
		// 10 updates
		for (int i = 0; i < stepAmount; i++)
			generateXUpdatesWithNewKey(10, update10);
		
		// Build spread-out updates list
		ArrayList<UpdateEntry> updatesList = new ArrayList<>(10000);
		for (int i = 0; i < 10; i++)
		{
			HashMap<String, String> stepUpdate = new HashMap<String, String>(update10);
			
			// Add these twice
			if ((i == 0) || (i == 9))
				stepUpdate.putAll(update2);
			
			// These once
			if (i == 1)
				stepUpdate.putAll(update1);
			
			updatesList.addAll(generateRandomizedUpdateList(stepUpdate));
		}
		
		// Send updates
		final long startTime = System.currentTimeMillis();
		final long duration = 10000;
		spoolUpdatesList(name, true, updatesQueue, updatesList, startTime, duration);

		assert cache.shutdownFor(4000) : "Cache failed to shutdown in time, still has items: " + cache.inflightMap.size();
		
		log.info("Waiting done, checking result");
		
		
		HashMap<String, String> dataMap = new HashMap<String, String>(update10);
		dataMap.putAll(update2);
		dataMap.putAll(update1);
		
		
		assertStorageMapContents(cache.getStorageDataMap(), dataMap, (key, actual, expectedPrototype) -> {
			
			StringBuilder expected = new StringBuilder(200);
			
			int writeMarkerCount;
			int repeatCount;
			if (key.startsWith("u1_"))
			{
				writeMarkerCount = 1;
				repeatCount = 1;
			}
			else if (key.startsWith("u2_"))
			{
				writeMarkerCount = 2;
				repeatCount = 2;
			}
			else if (key.startsWith("u10_"))
			{
				writeMarkerCount = 3; // This is how many times we expect item to rotate through cache
				repeatCount = 10;
			}
			else
				throw new IllegalStateException("Unsupported key type: " + key);

			for (int i = 0; i < writeMarkerCount; i++)
				expected.append(writeMarker);
			for (int i = 0; i < repeatCount; i++)
				expected.append(expectedPrototype);
			
			return stringCharactersMatch(actual, expected.toString());
		});
		
		pool.shutdownNow();
	}

	/**
	 * Tests cache by generating a lot of reads/update accesses w/ 
	 * storage-related failures.
	 * <p>
	 * However it is built in such a way, as to not cause failures that will
	 * corrupt the final data -- this is in order to check that the system works
	 * correctly (by comparing source data and final data) when failures are not
	 * 'fatal'.
	 */
	@Test
	public void testLotsOfAccessWithFailuresNoFatalWithMergeWrites()
		throws InterruptedException
	{
		testLotsOfAccessWithFailuresNoFatal(true);
	}


	/**
	 * Tests cache by generating a lot of reads/update accesses w/ 
	 * storage-related failures.
	 * <p>
	 * However it is built in such a way, as to not cause failures that will
	 * corrupt the final data -- this is in order to check that the system works
	 * correctly (by comparing source data and final data) when failures are not
	 * 'fatal'.
	 */
	@Test
	public void testLotsOfAccessWithFailuresNoFatalNoMergeWrites()
		throws InterruptedException
	{
		testLotsOfAccessWithFailuresNoFatal(false);
	}
	
	
	/**
	 * Tests cache by generating a lot of reads/update accesses w/ 
	 * storage-related failures.
	 * <p>
	 * However it is built in such a way, as to not cause failures that will
	 * corrupt the final data -- this is in order to check that the system works
	 * correctly (by comparing source data and final data) when failures are not
	 * 'fatal'.
	 * 
	 * @param canMergeWrites whether to run with this configuration flag on or off
	 */
	private void testLotsOfAccessWithFailuresNoFatal(boolean canMergeWrites)
		throws InterruptedException
	{
		final String name = "testLotsOfAccessWithFailuresNoFatal(canMergeWrites=" + canMergeWrites + ")";
		
		// Control debug logging/event tracing to file.
		final boolean debugLogging;
		{
			boolean f = false;
//			f = true; {} // uncomment this to enable debug logging; empty block produces warning in order to not forget to re-comment
			debugLogging = f;
		}
		
		OverrideFlatConfiguration config = new OverrideFlatConfiguration("wbrb/wbrb-default.properties");
		// This value results in quite a few TOO_MANY_CACHE_ELEMENT_UPDATES; 
		// however since there are no background updates and writing is allowed
		// after resync fail -- it still results in the correct end data
		config.override("maxUpdatesToCollect", "15");
		config.override("readThreadPoolSize", "0,100"); // need a bunch of read threads as they will fail and delay reads
		config.override("logThrottleMaxMessagesOfTypePerTimeInterval", "2"); // reduce spam
		config.override("logThrottleTimeInterval", "5s"); // make sure we have time to rotate through time period
		config.override("eventNotificationEnabled", "" + debugLogging); // set to true to enable event logging for debugging
		config.override("canMergeWrites", "" + canMergeWrites);
		config.override("maxSleepTime", ""); // use default maxSleepTime for this test
		config.override("returnQueueCacheTimeMin", "3s"); // increase for test stability as had unexpected re-queues sometimes under heavy load
		
		AtomicBoolean loggingOk = new AtomicBoolean(true);
		AtomicBoolean hadAsyncFailures = new AtomicBoolean(false);
		AtomicBoolean hadInvalidSplit = new AtomicBoolean(false);
		
		final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(name, config, 40, 40) // had failures under high CPU load with 50, 50 so lower values
			{
				private final ThreadLocal<Boolean> readMayFail = new ThreadLocal<Boolean>();

				@Override
				protected void spiNoLockProcessReadFromStorage(@Nonnull String key,
					boolean isRefreshRead,
					@Nonnull WBRBCacheEntry cacheEntry)
					throws InterruptedException
				{
					withReadLock(cacheEntry, () -> {
						WBRBCachePayload payload = cacheEntry.getPayload();
						Pair<Integer, Integer> failuresData = withReadLock(cacheEntry, () -> 
							new Pair<>(payload.getReadFailureCount().get(), payload.getFullCacheCycleFailureCount().get()));
						int readFailures = failuresData.getValue0();
						int fullCycleFailures = failuresData.getValue1();
						int potentialReadFailures = readFailures + 1;
						int potentialFullCycleFailures = fullCycleFailures + 1;
						
						boolean mayFail = true;
						
						if (potentialReadFailures >= config.getReadFailureMaxRetryCount())
						{
							if (!isRefreshRead)
									mayFail = false; // don't completely fail initial read 
							else
							{
								if (potentialFullCycleFailures >= config.getFullCacheCycleFailureMaxRetryCount())
									mayFail = false; // do not fail full-cycle failure limit
							}
						}
						
						readMayFail.set(mayFail);
					});
					
					super.spiNoLockProcessReadFromStorage(key, isRefreshRead, cacheEntry);
				}

				@Override
				protected String readFromStorage(String key,
					boolean isRefreshRead)
					throws InterruptedException
				{
					if (readMayFail.get())
					{
						if (random.nextInt(2) == 0) // 50%
						{
							if (readDelayMs > 0)
								Thread.sleep(readDelayMs);
							throw new RuntimeException("Read failed simulation.");
						}
					}
					return super.readFromStorage(key, isRefreshRead);
				}
				
				
				private final ThreadLocal<Boolean> writeMayFail = new ThreadLocal<Boolean>();

				@Override
				protected void spiNoLockWriteToStorage(@Nonnull String key,
					@Nonnull WBRBWriteQueueEntry writeEntry)
					throws InterruptedException
				{
					WBRBCacheEntry cacheEntry = writeEntry.getCacheEntry();
					Pair<Integer, Integer> failuresData = withReadLock(cacheEntry, () -> 
						new Pair<>(cacheEntry.getPayload().getWriteFailureCount().get(), cacheEntry.getPayload().getFullCacheCycleFailureCount().get()));
					int writeFailures = failuresData.getValue0();
					int fullCycleFailures = failuresData.getValue1();
					int potentialWriteFailures = writeFailures + 1;
					int potentialFullCycleFailures = fullCycleFailures + 1;
					
					boolean mayFail = true;
					
					if (potentialWriteFailures >= config.getWriteFailureMaxRetryCount())
					{
						if (potentialFullCycleFailures >= config.getFullCacheCycleFailureMaxRetryCount())
							mayFail = false; // do not fail full-cycle failure limit
					}
					
					writeMayFail.set(mayFail);
					
					super.spiNoLockWriteToStorage(key, writeEntry);
				}

				@Override
				protected void writeToStorage(String key, String dataToWrite)
					throws InterruptedException
				{
					if (writeMayFail.get())
					{
						if (random.nextInt(2) == 0) // 50%
						{
							if (writeDelayMs > 0)
								Thread.sleep(writeDelayMs);
							throw new RuntimeException("Write failed simulation.");
						}
					}
					super.writeToStorage(key, dataToWrite);
				}

				@Override
				protected void spiUnknownLockLogMessage(WBRBCacheMessage msg,
					@Nullable Throwable exception,
					@Nonnull Object @Nonnull... args)
						throws InterruptedException
				{
					if (debugLogging) // whether to log everything to file
						super.spiUnknownLockLogMessage_Plain(testFileLog, msg, exception, args); 
					super.spiUnknownLockLogMessage(msg, exception, args);
				}

				@Override
				protected void spiUnknownLock_Event(WBRBEvent event, String key,
					@Nullable WBRBCacheEntry cacheEntry,
					@Nullable WBRBCachePayload payload,
					@Nullable Throwable exception,
					@Nonnull Object @Nonnull... additionalArgs)
				{
					boolean success = false;
					
					try
					{
						Pair<String, @Nullable Object[]> output = debugUnknownLock_FormatEventForSlf4jLogging(event, key, cacheEntry, payload, exception, additionalArgs);
						testFileLog.info(output.getValue0(), output.getValue1());
						
						success = true;
					} finally
					{
						if (!success)
							loggingOk.set(false);
					}
				}

				@Override
				protected WriteSplit splitForWrite(
					String key, StringBuilder cacheData,
					NullableOptional<String> previousFailedWriteData)
				{
					if (!canMergeWrites)
					{
						if (!previousFailedWriteData.isEmpty())
						{
							hadInvalidSplit.set(true);
							throw new IllegalStateException("Non-empty previous failed write data for key [" + key + "] when canMergeWrites=false : " + previousFailedWriteData);
						}
					}
					
					return super.splitForWrite(key, cacheData, previousFailedWriteData);
				}
				
				
				
			}
			.start();
		
		final BlockingQueue<UpdateEntry> updatesQueue = new LinkedBlockingQueue<>();
		
		// Launch all the executors
		WAThreadPoolExecutor pool = new WAThreadPoolExecutor(name + "-executors", true);
		for (int i = 0; i < 200; i++)
		{
			pool.waSubmit(() -> {
				try
				{
					while (true)
					{
						try
						{
							UpdateEntry update = updatesQueue.take();
							final String key = update.getKey();
							if (Math.random() > 0.5) // do pre-loading roughly half the time
								cache.preloadCache(key);
							cache.readForOrException(key, 1000); // more time to handle loading failures (had issues under load)
							Thread.sleep(100);
							cache.writeIfCachedOrException(key, update.getUpdate());
						} catch (RuntimeException e)
						{
							log.error("[async executor] " + name + " failed: " + e, e);
							hadAsyncFailures.set(true);
						}
					}
				} catch( InterruptedException e )
				{
					throw new WAInterruptedException(e);
				}
			});
		}
		
		Map<String, String> dataMap = new HashMap<String, String>();
		
		final int stepAmount = 50;
		
		// Single update
		for (int i = 0; i < stepAmount; i++)
			generateXUpdatesWithNewKey(1, dataMap);
		
		// Two updates
		for (int i = 0; i < stepAmount; i++)
			generateXUpdatesWithNewKey(2, dataMap);
		
		// Three updates
		for (int i = 0; i < stepAmount; i++)
			generateXUpdatesWithNewKey(3, dataMap);
		
		// 10 updates
		for (int i = 0; i < stepAmount; i++)
			generateXUpdatesWithNewKey(10, dataMap);
		
		// 50 updates
		for (int i = 0; i < stepAmount; i++)
			generateXUpdatesWithNewKey(50, dataMap);
		
		// 100 updates
		for (int i = 0; i < stepAmount; i++)
			generateXUpdatesWithNewKey(100, dataMap);
		
		// 1000 updates
		generateXUpdatesWithNewKey(1000, dataMap);
		
		// 2000 updates
		generateXUpdatesWithNewKey(2000, dataMap);
		
		ArrayList<UpdateEntry> updatesList = generateRandomizedUpdateList(dataMap);
		
		// Send updates
		final long startTime = System.currentTimeMillis();
		final long duration = 10000;
		final long timeLimit = startTime + duration + 4000; // add some grace
		AsyncTestRunner<Void> future = TestUtil.runAsynchronously(
			() -> spoolUpdatesList(name, true, updatesQueue, updatesList, startTime, duration));
		
		// Check that on-the-fly states update properly
		{
			boolean okQueueSizes = false;
			boolean okReadPool = false;
			boolean okWritePool = false;
			final long sleepDuration = 20;
			for (int i = 0; i < duration; i += sleepDuration)
			{
				Thread.sleep(sleepDuration);
				WBRBStatus status = cache.getStatus(0);
				if (!okQueueSizes)
				{
					if (   (status.getCurrentCacheSize() > 50)
						&& (status.getMainQueueSize() > 50)
						&& (status.getReturnQueueSize() > 5))
					{
						okQueueSizes = true;
					}
				}
				if (!okReadPool)
				{
					if (status.getReadThreadPoolActiveThreads() > 5)
					{
						okReadPool = true;
					}
				}
				if (!okWritePool)
				{
					if (status.getWriteThreadPoolActiveThreads() > 5)
					{
						okWritePool = true;
					}
				}
				
				if (okQueueSizes && okReadPool && okWritePool)
					break;
			}
			assertTrue(okQueueSizes);
			assertTrue(okReadPool);
			assertTrue(okWritePool);
		}
		
		try
		{
			future.getResult(timeLimit - System.currentTimeMillis());
		} catch (Exception e)
		{
			throw new RuntimeException("Failed to properly complete data spool: " + e, e);
		}
		
		Thread.sleep(1000); // give time for executor threads to finish (they take time after picking up an element from queue)
		
		// wait for shutdown (give more time due to potentially failing operations)
		assert cache.shutdownFor(10000) : "Cache failed to shutdown in time, still has items: " + cache.inflightMap.size();

		assert !hadInvalidSplit.get() : "Had invalid split, search logs for canMergeWrites";
		assert loggingOk.get() : "Failures in event logging, search logs for SPI_EXCEPTION_Event";
		assert !hadAsyncFailures.get() : "Had asynchronous processing failures, check logs (search for '[async executor]')";
		assert updatesQueue.size() == 0 : updatesQueue.size();
		assert cache.inflightMap.size() == 0 : cache.inflightMap.size();
		
		log.info("Waiting done, checking result");
		
		assert cache.inflightMap.size() == 0 : cache.inflightMap.size(); 
		
		assertStorageMapContentsHasSameUpdates(cache.getStorageDataMap(), dataMap);
		
		pool.shutdownNow();
		
		// Check some monitoring statuses.
		// FIXME add tests for stuff that is zero here
		final long lowBound = 90; // had case where it was 99
		final long veryLowBound = 10;
		WBRBStatus status = cache.getStatus(0);
		assertEquals(status.getCurrentCacheSize(), 0);
		assertEquals(status.getMainQueueSize(), 0);
		assertEquals(status.getReturnQueueSize(), 0);
		assertEquals(status.getReadQueueSize(), 0);
		assertEquals(status.getWriteQueueSize(), 0);

		assertEquals(status.getReadThreadPoolActiveThreads(), 0);
		assertEquals(status.getWriteThreadPoolActiveThreads(), 0);
		
		assertEquals(status.getConfigMainQueueCacheTimeMs(), 4000);
		assertEquals(status.getConfigReturnQueueCacheTimeMinMs(), 3000);
		assertEquals(status.getConfigMainQueueMaxTargetSize(), 1000);
		assertEquals(status.getConfigMaxCacheElementsHardLimit(), 2000);
		
		assertGreater(status.getStorageReadQueueProcessedItems(), lowBound);
		assertGreater(status.getStorageReadTotalAttempts(), lowBound);
		assertGreater(status.getStorageReadTotalSuccesses(), lowBound);
		assertGreater(status.getStorageReadTotalFailures(), lowBound);
		assertGreater(status.getStorageReadRefreshAttempts(), lowBound);
		assertGreater(status.getStorageReadRefreshSuccesses(), lowBound);
		assertGreater(status.getStorageReadRefreshFailures(), lowBound);
//		assertLess(status.getStorageReadRefreshTooLateCount(), lowBound);
//		assertLess(status.getStorageReadRefreshDataNotUsedCount(), lowBound);
		assertGreater(status.getStorageReadInitialAttempts(), lowBound);
		assertGreater(status.getStorageReadInitialSuccesses(), lowBound);
		assertGreater(status.getStorageReadInitialFailures(), lowBound);
		assertEquals(status.getStorageReadTotalAttempts(), status.getStorageReadRefreshAttempts() + status.getStorageReadInitialAttempts());
		assertEquals(status.getStorageReadTotalSuccesses(), status.getStorageReadRefreshSuccesses() + status.getStorageReadInitialSuccesses());
		assertEquals(status.getStorageReadTotalFailures(), status.getStorageReadRefreshFailures() + status.getStorageReadInitialFailures()); 

		assertGreater(status.getStorageWriteQueueProcessedItems(), lowBound);
		assertGreater(status.getStorageWriteAttempts(), lowBound);
		assertGreater(status.getStorageWriteSuccesses(), lowBound);
		assertGreater(status.getStorageWriteFailures(), lowBound);
		
		assertGreater(status.getMainQueueProcessedItems(), lowBound);
//		status.getMainQueueLastItemInQueueDurationMs();
		assertGreater(status.getMainQueueSentWrites(), lowBound);
		assertEquals(status.getMainQueueExpiredFromCacheCount(), 0);
		assertEquals(status.getMainQueueRemovedFromCacheCount(), 0);
		assertEquals(status.getMainQueueRequeueToMainQueueCount(), 0);
		assertGreater(status.getMainQueueNotAllOkCount(), veryLowBound);
		
		assertGreater(status.getReturnQueueProcessedItems(), lowBound);
//		status.getReturnQueueLastItemInQueueDurationMs();
		assertGreater(status.getReturnQueueScheduledResyncs(), lowBound);
		assertEquals(status.getReturnQueueDoNothingCount(), 0);
		assertGreater(status.getReturnQueueExpiredFromCacheCount(), lowBound);
		assertEquals(status.getReturnQueueRemovedFromCacheCount(), 0);
		assertEquals(status.getReturnQueueRequeueToReturnQueueCount(), 0);

		assertGreater(status.getCheckCacheAttemptsNoDedup(), lowBound);
		assertGreater(status.getCheckCachePreloadAttempts(), lowBound);
		assertGreater(status.getCheckCachePreloadCacheHit(), lowBound);
		assertEquals(status.getCheckCachePreloadCacheFullExceptionCount(), 0);
		assertGreater(status.getCheckCacheReadAttempts(), lowBound);
		assertGreater(status.getCheckCacheReadCacheHit(), lowBound);
		assertEquals(status.getCheckCacheReadCacheFullExceptionCount(), 0);
		assertEquals(status.getCheckCacheTotalCacheFullExceptionCount(), 0);
		assertEquals(status.getCheckCacheNullKeyCount(), 0);

		assertGreater(status.getCacheReadAttempts(), lowBound);
		assertEquals(status.getCacheReadTimeouts(), 0);
		assertEquals(status.getCacheReadErrors(), 0);
		
		assertGreater(status.getCacheWriteAttempts(), lowBound);
		assertEquals(status.getCacheWriteElementNotPresentCount(), 0);
		assertEquals(status.getCacheWriteErrors(), 0);
		assertBetweenInclusive(status.getCacheWriteTooManyUpdates(), 25L, 100L); // empirically it seems to be about 50
		
		assertEquals(status.getMsgWarnCount(), 0);
		assertGreater(status.getMsgExternalWarnCount(), veryLowBound);
		assertGreater(status.getMsgExternalErrorCount(), lowBound);
		assertGreater(status.getMsgExternalDataLossCount(), veryLowBound);
		assertEquals(status.getMsgErrorCount(), 0);
		assertEquals(status.getMsgFatalCount(), 0);
		assertGreater(status.getMsgTotalWarnOrHigherCount(), lowBound);
		assertGreater(status.getMsgTotalErrorOrHigherCount(), lowBound);
	}
	
	
	/**
	 * Map of {@link WriteBehindResyncInBackgroundCache} fields that are used
	 * to test alive/not-alive stuff -- to the corresponding getters in {@link WBRBStatus}
	 */
	private static final Map<String, String> WBRB_ALIVENESS_MAP;
	
	static
	{
		Map<String, String> map = new HashMap<String, String>();
		map.put("readQueueProcessingThread", "isReadQueueProcessingThreadAlive");
		map.put("writeQueueProcessingThread", "isWriteQueueProcessingThreadAlive");
		map.put("mainQueueProcessingThread", "isMainQueueProcessingThreadAlive");
		map.put("returnQueueProcessingThread", "isReturnQueueProcessingThreadAlive");
		map.put("readThreadPool", "isReadThreadPoolAlive");
		map.put("writeThreadPool", "isWriteThreadPoolAlive");
		
		WBRB_ALIVENESS_MAP = Collections.unmodifiableMap(map);
	}
	
	/**
	 * Tests some monitoring functionality.
	 * <p>
	 * See also {@link #testSimpleSuccessPath()} for a lot more checks for monitoring;
	 * many other methods also check various monitoring information.
	 */
	@Test
	public void testMonitoring() throws Exception
	{
		final String name = "testMonitoring";
		int testIndex = 1;
		
		 FlatConfiguration baseConfig = Configuration.fromPropertiesFile("wbrb/wbrb-default.properties");
		
		{
			// Check status caching.
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(name + (testIndex++), config, 50, 50).start();
			
			WBRBStatus status1 = cache.getStatus(100);
			WBRBStatus status2 = cache.getStatus(100);
			Thread.sleep(200);
			WBRBStatus status3 = cache.getStatus(100);
			WBRBStatus status4 = cache.getStatus(0);
			
			assertEquals(status2, status1);
			assertNotEquals(status3, status1);
			assertNotEquals(status4, status3);
			
			try
			{
				cache.getStatus(-1);
				fail("should not be reacheable");
			} catch (RuntimeException e)
			{
				assertTrue(e.toString().contains("non-negative"), e.toString());
			}
			
			assertTrue(cache.shutdownFor(200));
		}
		
		{
			// Check messages counting.
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(name + (testIndex++), config, 50, 50).start();
			
			{
				WBRBStatus status = cache.getStatus(0);
				assertEquals(status.getMsgWarnCount(), 0);
				assertEquals(status.getMsgExternalWarnCount(), 0);
				assertEquals(status.getMsgExternalErrorCount(), 0);
				assertEquals(status.getMsgExternalDataLossCount(), 0);
				assertEquals(status.getMsgErrorCount(), 0);
				assertEquals(status.getMsgFatalCount(), 0);
				assertEquals(status.getMsgTotalWarnOrHigherCount(), 0);
				assertEquals(status.getMsgTotalErrorOrHigherCount(), 0);
			}
			
			cache.logMessage(WBRBCacheMessage.TEST_WARN, null); // WARN
			
			cache.logMessage(WBRBCacheMessage.MAIN_QUEUE_NON_STANDARD_OUTCOME, null); // EXTERNAL_WARN
			cache.logMessage(WBRBCacheMessage.MAIN_QUEUE_NON_STANDARD_OUTCOME, null); // EXTERNAL_WARN
			
			cache.logMessage(WBRBCacheMessage.STORAGE_READ_FAIL, null); // EXTERNAL_ERROR
			cache.logMessage(WBRBCacheMessage.STORAGE_READ_FAIL, null); // EXTERNAL_ERROR
			cache.logMessage(WBRBCacheMessage.STORAGE_READ_FAIL, null); // EXTERNAL_ERROR

			cache.logMessage(WBRBCacheMessage.STORAGE_READ_FAIL_FINAL, null); // EXTERNAL_DATA_LOSS
			cache.logMessage(WBRBCacheMessage.STORAGE_READ_FAIL_FINAL, null); // EXTERNAL_DATA_LOSS
			cache.logMessage(WBRBCacheMessage.STORAGE_READ_FAIL_FINAL, null); // EXTERNAL_DATA_LOSS
			cache.logMessage(WBRBCacheMessage.STORAGE_READ_FAIL_FINAL, null); // EXTERNAL_DATA_LOSS
			
			cache.logMessage(WBRBCacheMessage.MAIN_QUEUE_PROCESSOR_UNEXPECTED_INTERRUPT, null); // ERROR
			cache.logMessage(WBRBCacheMessage.MAIN_QUEUE_PROCESSOR_UNEXPECTED_INTERRUPT, null); // ERROR
			cache.logMessage(WBRBCacheMessage.MAIN_QUEUE_PROCESSOR_UNEXPECTED_INTERRUPT, null); // ERROR
			cache.logMessage(WBRBCacheMessage.MAIN_QUEUE_PROCESSOR_UNEXPECTED_INTERRUPT, null); // ERROR
			cache.logMessage(WBRBCacheMessage.MAIN_QUEUE_PROCESSOR_UNEXPECTED_INTERRUPT, null); // ERROR
			
			cache.logMessage(WBRBCacheMessage.ASSERTION_FAILED, null); // FATAL
			cache.logMessage(WBRBCacheMessage.ASSERTION_FAILED, null); // FATAL
			cache.logMessage(WBRBCacheMessage.ASSERTION_FAILED, null); // FATAL
			cache.logMessage(WBRBCacheMessage.ASSERTION_FAILED, null); // FATAL
			cache.logMessage(WBRBCacheMessage.ASSERTION_FAILED, null); // FATAL
			cache.logMessage(WBRBCacheMessage.ASSERTION_FAILED, null); // FATAL
			
			{
				WBRBStatus status = cache.getStatus(0);
				assertEquals(status.getMsgWarnCount(), 1);
				assertEquals(status.getMsgExternalWarnCount(), 2);
				assertEquals(status.getMsgExternalErrorCount(), 3);
				assertEquals(status.getMsgExternalDataLossCount(), 4);
				assertEquals(status.getMsgErrorCount(), 5);
				assertEquals(status.getMsgFatalCount(), 6);
				assertEquals(status.getMsgTotalWarnOrHigherCount(), 1 + 2 + 3 + 4 + 5 + 6);
				assertEquals(status.getMsgTotalErrorOrHigherCount(), 3 + 4 + 5 + 6);
			}
			
			// Test that WARN counts up properly
			cache.logMessage(WBRBCacheMessage.TEST_WARN, null); // WARN
			assertEquals(cache.getStatus(0).getMsgWarnCount(), 2);
				
			assertTrue(cache.shutdownFor(200));
		}
	}

	/**
	 * Tests that 'aliveness' states are properly handled by monitoring.
	 * @throws ExecutionException 
	 * @throws TimeoutException 
	 */
	@Test
	public void testMonitoringForAliveness()
		throws NoSuchFieldException,
		IllegalAccessException,
		InterruptedException, TimeoutException, ExecutionException
	{
		final String name = "testMonitoringForAliveness";
		int testIndex = 1;
		
		OverrideFlatConfiguration config = new OverrideFlatConfiguration("wbrb/wbrb-default.properties");
		
		{
			// Check thread alive-ness stuff.
			for (String fieldName : WBRB_ALIVENESS_MAP.keySet())
			{
				log.info("Testing thread/pool termination: {}", fieldName);
				
				Set<String> fields = new HashSet<String>(WBRB_ALIVENESS_MAP.keySet());
				
				final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(name + (testIndex++), config, 50, 50);
				
				{
					WBRBStatus status = cache.getStatus(0);
					assertFalse(status.isCacheAlive());
					assertFalse(status.isEverythingAlive());
					ensureWBRBStatusBeforeStartAliveness(status);
				}
				
				cache.start();
				{
					WBRBStatus status = cache.getStatus(0);
					assertTrue(status.isCacheAlive());
					assertTrue(status.isEverythingAlive());
					ensureWBRBStatusAliveness(status, fields);
				}
				
				// Now terminate one of the items.
				{
					Field field = WriteBehindResyncInBackgroundCache.class.getDeclaredField(fieldName);
					field.setAccessible(true);
					
					Object item = field.get(cache);
					if (item instanceof ExitableThread)
					{
						ExitableThread t = (ExitableThread)item;
						t.exitAsap();
						t.join(500); // wait for it to die
					}
					else if (item instanceof ThreadPoolExecutor)
					{
						((ThreadPoolExecutor)item).shutdown();
					}
					else
						fail();
				}
				
				// Check everything is alive except the one we terminated
				fields.remove(fieldName);
				{
					WBRBStatus status = cache.getStatus(0);
					assertTrue(status.isCacheAlive());
					ensureWBRBStatusAliveness(status, fields);
					assertFalse(status.isEverythingAlive());
				}
				
				assertTrue(cache.shutdownFor(200));
				Thread.sleep(200); // delay to let threads die
				
				{
					WBRBStatus status = cache.getStatus(0);
					assertFalse(status.isCacheAlive());
					ensureWBRBStatusAliveness(status, Collections.EMPTY_SET);
					assertFalse(status.isEverythingAlive());
				}
			}
			log.info("Done testing terminating threads/pools.");
		}
		
		
		{
			// Check shutdown immediately marking 'everything alive' as not alive
			log.info("Testing shutdown leading to non-alive.");
			
			Set<String> fields = new HashSet<String>(WBRB_ALIVENESS_MAP.keySet());
			
			final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(name + (testIndex++), config, 10/*read*/, 300/*write*/);
			{
				WBRBStatus status = cache.getStatus(0); 
				assertEquals(cache.getControlState(), WBRBCacheControlState.NOT_STARTED);
				assertEquals(status.getCacheControlState(), WBRBCacheControlState.NOT_STARTED);
				assertEquals(status.getCacheControlStateString(), WBRBCacheControlState.NOT_STARTED.name());
				assertFalse(status.isEverythingAlive());
			}
			
			cache.start();
			{
				WBRBStatus status = cache.getStatus(0); 
				assertEquals(cache.getControlState(), WBRBCacheControlState.RUNNING);
				assertEquals(status.getCacheControlState(), WBRBCacheControlState.RUNNING);
				assertEquals(status.getCacheControlStateString(), WBRBCacheControlState.RUNNING.name());
				assertTrue(status.isEverythingAlive());
			}
			
			final String key = "key";
			cache.readForOrException(key, 200);
			cache.writeIfCached(key, 'u');
			AsyncTestRunner<Void> shutdownFuture = TestUtil.runAsynchronously(() -> {
				assertTrue(cache.shutdownFor(2000));
			});
			
			Thread.sleep(100); // give some time
			{
				WBRBStatus status = cache.getStatus(0);
				assertEquals(cache.getControlState(), WBRBCacheControlState.SHUTDOWN_IN_PROGRESS);
				assertEquals(status.getCacheControlState(), WBRBCacheControlState.SHUTDOWN_IN_PROGRESS);
				assertEquals(status.getCacheControlStateString(), WBRBCacheControlState.SHUTDOWN_IN_PROGRESS.name());
				
				ensureWBRBStatusAliveness(status, fields);
				assertFalse(status.isEverythingAlive());
			}
			
			shutdownFuture.getResult(2000); // Check shutdown is fine (will exception if not)
			{
				Thread.sleep(200); // Wait some to make sure threads have time to terminate
				WBRBStatus status = cache.getStatus(0);
				assertEquals(cache.getControlState(), WBRBCacheControlState.SHUTDOWN_COMPLETED);
				assertEquals(status.getCacheControlState(), WBRBCacheControlState.SHUTDOWN_COMPLETED);
				assertEquals(status.getCacheControlStateString(), WBRBCacheControlState.SHUTDOWN_COMPLETED.name());
				
				ensureWBRBStatusAliveness(status, Collections.emptySet());
				assertFalse(status.isEverythingAlive());
			}
			
			log.info("Done testing shutdown leading to non-alive.");
		}
	}
	
	/**
	 * Checks that threads & pools are alive according to the list; items not
	 * in the list are expected to be non-alive
	 * 
	 * @param fieldsExpectedToBeAlive list of cache fields expected to be alive
	 */
	private void ensureWBRBStatusAliveness(
		WBRBStatus status, Set<String> fieldsExpectedToBeAlive)
	{
		scanWBRBStatusAliveness(status, (name, value) -> 
			assertEquals(value.booleanValue(), fieldsExpectedToBeAlive.contains(name), name + " unexpected value: " + value ));
	}
	
	/**
	 * Checks that threads & pools are in the correct state BEFORE cache is started
	 */
	private void ensureWBRBStatusBeforeStartAliveness(WBRBStatus status)
	{
		// Pools should be alive, threads -- should not
		scanWBRBStatusAliveness(status, (name, value) -> 
			assertEquals(value.booleanValue(), name.contains("Pool"), name + " unexpected value: " + value ));
	}
	
	/**
	 * Scans all aliveness-related statuses in {@link WBRBStatus} and reports
	 * them to the given checker
	 * <p>
	 * Checker arguments are: cache field name (NOT status getter name) and 
	 * boolean aliveness value from the status.
	 */
	private void scanWBRBStatusAliveness(WBRBStatus status, BiConsumer<String, Boolean> checker)
	{
		try
		{
			for (Entry<String, String> entry : WBRB_ALIVENESS_MAP.entrySet())
			{
				final String field = entry.getKey();
				final String getter = entry.getValue();
				
				Method method = status.getClass().getMethod(getter);
				boolean alive = (boolean)method.invoke(status);
				
				checker.accept(field, alive);
			}
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Tests in-thread (rather than in separate pool) reads & writes.
	 */
	@Test
	public void testInThreadOperations() throws Exception
	{
		final String name = "testInThreadOperations";
		int testIndex = 1;
		
		FlatConfiguration baseConfig = Configuration.fromPropertiesFile("wbrb/wbrb-default.properties");
		
		{
			// Test for in-thread read & read queue sizes
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			config.override("readThreadPoolSize", "-1,-1"); // disable read pool so we can test read queue size
			config.override("readQueueBatchingDelay", "70ms");
//			newConfig.override("eventNotificationEnabled", "true"); {}
			
			final AtomicInteger readBatchCounter = new AtomicInteger(0);
			
			final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(name + (testIndex++), config, 35, 50)
				{
					@Override
					protected void spiNoLockReadBatchDelayExpired()
						throws InterruptedException
					{
						readBatchCounter.incrementAndGet();
					}
				}
//				.setDebugLogger(log)
				.start();
				
			assertEquals(readBatchCounter.get(), 0);
				
			for (int i = 0; i < 20; i++)
				cache.preloadCache("key" + i);
			
			{
				assertEquals(readBatchCounter.get(), 0);
				WBRBStatus status = cache.getStatus(0);
				assert status.getCurrentCacheSize() > 10;
				assert status.getMainQueueSize() > 10;
				assert status.getReturnQueueSize() == 0;
				assert status.getReadQueueSize() > 10;
				assert status.getWriteQueueSize() == 0;
			}
			
			for (int i = 0; i < 100; i++)
			{
				Thread.sleep(20);
				if (cache.getStatus(0).getReadQueueSize() < 2)
					break;
			}
			
			Thread.sleep(200);
			assertEquals(readBatchCounter.get(), 1);
			
			for (int i = 20; i < 22; i++)
				cache.preloadCache("key" + i);

			assertEquals(readBatchCounter.get(), 1);
			
			Thread.sleep(250);
			assertEquals(readBatchCounter.get(), 2);
			
			assertTrue(cache.shutdownFor(2000));
			assertEquals(readBatchCounter.get(), 2);
		}
		
		{
			// Test for in-thread write & write queue sizes
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			config.override("writeThreadPoolSize", "-1,-1"); // disable read pool so we can test read queue size
			config.override("mainQueueCacheTime", "200ms"); // short main queue so they go to write ASAP
			config.override("writeQueueBatchingDelay", "70ms");
//			newConfig.override("eventNotificationEnabled", "true"); {}
			
			final AtomicInteger writeBatchCounter = new AtomicInteger(0);
			
			final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(name + (testIndex++), config, 0/*read delay*/, 35)
				{
					@Override
					protected void spiNoLockWriteBatchDelayExpired()
						throws InterruptedException
					{
						writeBatchCounter.incrementAndGet();
					}
				}
//				.setDebugLogger(log)
				.start();
			for (int i = 0; i < 20; i++)
				cache.readForOrException("key" + i, 50);
			for (int i = 0; i < 20; i++)
				cache.writeIfCached("key" + i, 'u');
			
			assertEquals(writeBatchCounter.get(), 0);
			
			Thread.sleep(400); // Enough so they should be processed for writing
			
			{
				assertEquals(writeBatchCounter.get(), 0);
				WBRBStatus status = cache.getStatus(0);
				assert status.getCurrentCacheSize() > 10 : status;
//				assert status.getMainQueueSize() > 10 : status;
				assert status.getReturnQueueSize() > 0 : status;
				assert status.getReadQueueSize() == 0 : status;
				assert status.getWriteQueueSize() > 10 : status;
			}
			
			for (int i = 0; i < 100; i++)
			{
				Thread.sleep(20);
				if (cache.getStatus(0).getWriteQueueSize() < 2)
					break;
			}
			
			Thread.sleep(200);
			assertEquals(writeBatchCounter.get(), 1);
			
			for (int i = 0; i < 2; i++)
				cache.writeIfCached("key" + i, 'u');
			
			Thread.sleep(600); // Enough so they should be processed for writing
			assertEquals(writeBatchCounter.get(), 2);
			
			assertTrue(cache.shutdownFor(2000));
			assertEquals(writeBatchCounter.get(), 2);
		}
	}
	
	/**
	 * Tests case when read is 'slow' (slower than max sleep time).
	 */
	@Test
	public void testSlowRead()
	{
		final String name = "testSlowRead";
		int testIndex = 1;
		
		FlatConfiguration baseConfig = Configuration.fromPropertiesFile("wbrb/wbrb-default.properties");
		
		{
			// Test what happens if read is longer than max sleep time
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			config.override("maxSleepTime", "10ms"); // very low max sleep time
//			config.override("eventNotificationEnabled", "true"); {}
			
			final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(name + (testIndex++), config, 150 /*read delay*/, 50)
//				.setDebugLogger(log)
				.start();
			
			assertNotNull(cache.readForOrException("akey", 300)); // should succeed
			
			assertTrue(cache.shutdownFor(2000));
		}
		
		{
			// Test what happens if read is very long and cache is shutdown concurrently
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			config.override("maxSleepTime", "10ms"); // very low max sleep time
//			newConfig.override("eventNotificationEnabled", "true"); {}
			
			final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(name + (testIndex++), config, 15000 /*read delay*/, 50)
//				.setDebugLogger(log)
				.start();
			
			long start = System.currentTimeMillis();
			
			TestUtil.runAsynchronously(() -> {
				Thread.sleep(200);
				assertTrue(cache.shutdownFor(2000));
			});
			
			assertContainsIgnoreCase(
				assertFails(() -> cache.readForOrException("akey", 30000)).toString(), "shutdown");
			
			long end = System.currentTimeMillis();
			long duration = end - start;
			
			assertGreater(duration, 100L);
			assertLess(duration, 400L);
		}
		
		
	}
	
	/**
	 * Tests what happens with null keys.
	 */
	@Test
	public void testNullKeys()
	{
		final String name = "testNullKeys";
		int testIndex = 1;
		
		FlatConfiguration baseConfig = Configuration.fromPropertiesFile("wbrb/wbrb-default.properties");
		
		final String nullKeySubstring = "Key must not be null";
		
		{
			// Test what happens if read is longer than max sleep time
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			
			final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(name + (testIndex++), config, 50 /*read delay*/, 50)
//				.setDebugLogger(log)
				.start();
			
			assertContainsIgnoreCase( assertFails(() -> cache.preloadCache(fakeNonNull())).toString(), nullKeySubstring);
			assertContainsIgnoreCase( assertFails(() -> cache.readFor(fakeNonNull(), 10)).toString(), nullKeySubstring);
			assertContainsIgnoreCase( assertFails(() -> cache.readForOrException(fakeNonNull(), 10)).toString(), nullKeySubstring);
			assertContainsIgnoreCase( assertFails(() -> cache.readIfCached(fakeNonNull())).toString(), nullKeySubstring);
			assertContainsIgnoreCase( assertFails(() -> cache.readIfCachedOrException(fakeNonNull())).toString(), nullKeySubstring);
			assertContainsIgnoreCase( assertFails(() -> cache.readUntil(fakeNonNull(), 10)).toString(), nullKeySubstring);
			assertContainsIgnoreCase( assertFails(() -> cache.readUntilOrException(fakeNonNull(), 10)).toString(), nullKeySubstring);
			assertContainsIgnoreCase( assertFails(() -> cache.writeIfCached(fakeNonNull(), 'u')).toString(), nullKeySubstring);
			assertContainsIgnoreCase( assertFails(() -> cache.writeIfCachedOrException(fakeNonNull(), 'u')).toString(), nullKeySubstring);
			
			assertTrue(cache.shutdownFor(2000));
		}
		
	}
	
	/**
	 * Tests cache control states (not started, shutdown...).
	 */
	@Test
	public void testControlStates()
	{
		final String name = "testControlStates";
		int testIndex = 1;
		
		FlatConfiguration baseConfig = Configuration.fromPropertiesFile("wbrb/wbrb-default.properties");
		
		{
			// Test standard control states functionality
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			
			final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(name + (testIndex++), config, 0 /*read delay*/, 0);
			
			assertFalse(cache.isAlive());
			assertFalse(cache.isUsable());
			assertFalse(cache.getStatus(0).isCacheAlive());
			assertEquals(cache.getStatus(0).getCacheControlStateString(), WBRBCacheControlState.NOT_STARTED.name());
			
			final String key = "akey";
			final String notStartedYet = "not yet started";
			final String startedAlready = "started already";
			final String shutdownAlready = "shutdown already";
			
			assertContains( assertFails(() -> cache.preloadCache(key)).toString(), notStartedYet);
			assertContains( assertFails(() -> cache.readFor(key, 1000)).toString(), notStartedYet);
			assertContains( assertFails(() -> cache.readForOrException(key, 1000)).toString(), notStartedYet);
			assertContains( assertFails(() -> cache.readIfCached(key)).toString(), notStartedYet);
			assertContains( assertFails(() -> cache.readIfCachedOrException(key)).toString(), notStartedYet);
			assertContains( assertFails(() -> cache.readUntil(key, 1000)).toString(), notStartedYet);
			assertContains( assertFails(() -> cache.readUntilOrException(key, 1000)).toString(), notStartedYet);
			assertContains( assertFails(() -> cache.writeIfCached(key, 'u')).toString(), notStartedYet);
			assertContains( assertFails(() -> cache.writeIfCachedOrException(key, 'u')).toString(), notStartedYet);
			
			cache.start();
			
			assertTrue(cache.isAlive());
			assertTrue(cache.isUsable());
			assertTrue(cache.getStatus(0).isCacheAlive());
			assertEquals(cache.getStatus(0).getCacheControlStateString(), WBRBCacheControlState.RUNNING.name());
			
			cache.preloadCache(key);
			cache.readFor(key, 1000);
			cache.readForOrException(key, 1000);
			cache.readIfCached(key);
			cache.readIfCachedOrException(key);
			cache.readUntil(key, 1000);
			cache.readUntilOrException(key, 1000);
			cache.writeIfCached(key, 'u');
			cache.writeIfCachedOrException(key, 'u');

			assertFailsWithSubstring(() -> cache.start(), startedAlready);
			
			assertTrue(cache.isAlive());
			assertTrue(cache.isUsable());
			assertTrue(cache.getStatus(0).isCacheAlive());
			assertEquals(cache.getStatus(0).getCacheControlStateString(), WBRBCacheControlState.RUNNING.name());
			
			assertTrue(cache.shutdownFor(2000));
			
			assertFalse(cache.isAlive());
			assertFalse(cache.isUsable());
			assertFalse(cache.getStatus(0).isCacheAlive());
			assertEquals(cache.getStatus(0).getCacheControlStateString(), WBRBCacheControlState.SHUTDOWN_COMPLETED.name());
			
			assertFailsWithSubstring(() -> cache.preloadCache(key), shutdownAlready);
			assertFailsWithSubstring(() -> cache.readFor(key, 1000), shutdownAlready);
			assertFailsWithSubstring(() -> cache.readForOrException(key, 1000), shutdownAlready);
			assertFailsWithSubstring(() -> cache.readIfCached(key), shutdownAlready);
			assertFailsWithSubstring(() -> cache.readIfCachedOrException(key), shutdownAlready);
			assertFailsWithSubstring(() -> cache.readUntil(key, 1000), shutdownAlready);
			assertFailsWithSubstring(() -> cache.readUntilOrException(key, 1000), shutdownAlready);
			assertFailsWithSubstring(() -> cache.writeIfCached(key, 'u'), shutdownAlready);
			assertFailsWithSubstring(() -> cache.writeIfCachedOrException(key, 'u'), shutdownAlready);

			assertFailsWithSubstring(() -> cache.start(), shutdownAlready);
			
			assertFalse(cache.isAlive());
			assertFalse(cache.isUsable());
			assertFalse(cache.getStatus(0).isCacheAlive());
			assertEquals(cache.getStatus(0).getCacheControlStateString(), WBRBCacheControlState.SHUTDOWN_COMPLETED.name());
			
			assertFailsWithSubstring(() -> cache.shutdownFor(2000), shutdownAlready);
			
			assertFalse(cache.isAlive());
			assertFalse(cache.isUsable());
			assertFalse(cache.getStatus(0).isCacheAlive());
			assertEquals(cache.getStatus(0).getCacheControlStateString(), WBRBCacheControlState.SHUTDOWN_COMPLETED.name());
		}
		
		{
			// Test shutdown without starting
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			
			final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(name + (testIndex++), config, 0 /*read delay*/, 0);
			
			assertFalse(cache.isAlive());
			assertFalse(cache.isUsable());
			assertFalse(cache.getStatus(0).isCacheAlive());
			assertEquals(cache.getStatus(0).getCacheControlStateString(), WBRBCacheControlState.NOT_STARTED.name());
			
			final String key = "akey";
			final String notStartedYet = "not yet started";
			final String shutdownAlready = "shutdown already";
			
			assertContains( assertFails(() -> cache.preloadCache(key)).toString(), notStartedYet);
			assertContains( assertFails(() -> cache.readFor(key, 1000)).toString(), notStartedYet);
			assertContains( assertFails(() -> cache.readForOrException(key, 1000)).toString(), notStartedYet);
			assertContains( assertFails(() -> cache.readIfCached(key)).toString(), notStartedYet);
			assertContains( assertFails(() -> cache.readIfCachedOrException(key)).toString(), notStartedYet);
			assertContains( assertFails(() -> cache.readUntil(key, 1000)).toString(), notStartedYet);
			assertContains( assertFails(() -> cache.readUntilOrException(key, 1000)).toString(), notStartedYet);
			assertContains( assertFails(() -> cache.writeIfCached(key, 'u')).toString(), notStartedYet);
			assertContains( assertFails(() -> cache.writeIfCachedOrException(key, 'u')).toString(), notStartedYet);
			
			assertTrue(cache.shutdownFor(2000));
			
			assertFalse(cache.isAlive());
			assertFalse(cache.isUsable());
			assertFalse(cache.getStatus(0).isCacheAlive());
			assertEquals(cache.getStatus(0).getCacheControlStateString(), WBRBCacheControlState.SHUTDOWN_COMPLETED.name());
			
			assertFailsWithSubstring(() -> cache.preloadCache(key), shutdownAlready);
			assertFailsWithSubstring(() -> cache.readFor(key, 1000), shutdownAlready);
			assertFailsWithSubstring(() -> cache.readForOrException(key, 1000), shutdownAlready);
			assertFailsWithSubstring(() -> cache.readIfCached(key), shutdownAlready);
			assertFailsWithSubstring(() -> cache.readIfCachedOrException(key), shutdownAlready);
			assertFailsWithSubstring(() -> cache.readUntil(key, 1000), shutdownAlready);
			assertFailsWithSubstring(() -> cache.readUntilOrException(key, 1000), shutdownAlready);
			assertFailsWithSubstring(() -> cache.writeIfCached(key, 'u'), shutdownAlready);
			assertFailsWithSubstring(() -> cache.writeIfCachedOrException(key, 'u'), shutdownAlready);

			assertFailsWithSubstring(() -> cache.start(), shutdownAlready);
			
			assertFalse(cache.isAlive());
			assertFalse(cache.isUsable());
			assertFalse(cache.getStatus(0).isCacheAlive());
			assertEquals(cache.getStatus(0).getCacheControlStateString(), WBRBCacheControlState.SHUTDOWN_COMPLETED.name());
			
			assertFailsWithSubstring(() -> cache.shutdownFor(2000), shutdownAlready);
			
			assertFalse(cache.isAlive());
			assertFalse(cache.isUsable());
			assertFalse(cache.getStatus(0).isCacheAlive());
			assertEquals(cache.getStatus(0).getCacheControlStateString(), WBRBCacheControlState.SHUTDOWN_COMPLETED.name());
		}
		
	}
	
	
	/**
	 * Tests cache control states (not started, shutdown...).
	 */
	@Test
	public void testCacheFlush() throws TimeoutException, ExecutionException, InterruptedException
	{
		final String name = "testCacheFlush";
		int testIndex = 1;
		
		FlatConfiguration baseConfig = Configuration.fromPropertiesFile("wbrb/wbrb-default.properties");
		
		{
			// Test quick flush (success scenario)
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			
			final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(name + (testIndex++), config, 0 /*read delay*/, 0)
				.start();
			
			final String key = "akey";
			
			cache.readForOrException(key, 1000);
			cache.writeIfCachedOrException(key, 'u');
			
			assertEquals(cache.getStatus(0).getCurrentCacheSize(), 1);
			
			long start = System.currentTimeMillis();
			assertTrue(cache.flushFor(1000));
			long end = System.currentTimeMillis();
			long duration = end - start;
			
			assertGreater(duration, 20L);
			assertLess(duration, 500L);
			WBRBStatus status = cache.getStatus(0);
			assertEquals(status.getCurrentCacheSize(), 0);
			assertEquals(status.getCacheControlStateString(), WBRBCacheControlState.RUNNING.name());
			assertStorageMapContentsExactlyEquals(cache.getStorageDataMap(), key, "u");
			
			assertTrue(cache.shutdownFor(2000));
		}
		
		{
			// Test slow flush + async stuff
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			
			final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(name + (testIndex++), config, 0 /*read delay*/, 500)
				.start();
			
			final String key = "akey";
			final String key2 = "akey2";
			
			cache.readForOrException(key, 1000);
			cache.writeIfCachedOrException(key, 'u');
			cache.readForOrException(key2, 1000);
			cache.writeIfCachedOrException(key2, 'v');
			
			WBRBStatus status = cache.getStatus(0);
			assertEquals(status.getCurrentCacheSize(), 2);
			assertTrue(status.isCacheAlive());
			assertTrue(status.isCacheUsable());
			
			long start = System.currentTimeMillis();
			
			AsyncTestRunner<Boolean> async = TestUtil.callAsynchronously(() -> cache.flushUntil(start + 2000));
			Thread.sleep(100);
			status = cache.getStatus(0);
			assertTrue(status.isCacheAlive());
			assertFalse(status.isCacheUsable());
			assertEquals(status.getCurrentCacheSize(), 2);
			assertEquals(status.getCacheControlStateString(), WBRBCacheControlState.FLUSHING.name());
			assertFailsWithSubstring(() -> cache.flushFor(200), "already flushing"); // check that we can't concurrently flush
			assertTrue(async.getResult(2000)); // check original flush succeeded
			
			long end = System.currentTimeMillis();
			long duration = end - start;
			
			assertGreater(duration, 400L);
			assertLess(duration, 1000L);
			status = cache.getStatus(0);
			assertTrue(status.isCacheAlive());
			assertTrue(status.isCacheUsable());
			assertEquals(status.getCurrentCacheSize(), 0);
			assertEquals(status.getCacheControlStateString(), WBRBCacheControlState.RUNNING.name());
			assertStorageMapContentsExactlyEquals(cache.getStorageDataMap(), key, "u", key2, "v");
			
			assertTrue(cache.shutdownFor(2000));
		}
		
		{
			// Test slow flush + shutdown
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			
			final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(name + (testIndex++), config, 0 /*read delay*/, 500)
				.start();
			
			final String key = "akey";
			final String key2 = "akey2";
			
			cache.readForOrException(key, 1000);
			cache.writeIfCachedOrException(key, 'u');
			cache.readForOrException(key2, 1000);
			cache.writeIfCachedOrException(key2, 'v');
			
			assertEquals(cache.getStatus(0).getCurrentCacheSize(), 2);
			
			long start = System.currentTimeMillis();
			
			AsyncTestRunner<Boolean> async = TestUtil.callAsynchronously(() -> cache.flushFor(2000));
			Thread.sleep(100);
			WBRBStatus status = cache.getStatus(0);
			assertEquals(status.getCurrentCacheSize(), 2);
			assertEquals(status.getCacheControlStateString(), WBRBCacheControlState.FLUSHING.name());
			assertFailsWithSubstring(() -> cache.flushFor(200), "already flushing"); // check that we can't concurrently flush
			
			assertTrue(cache.shutdownFor(1000)); // shutdown cache while it is also flushing
			
			assertTrue(async.getResult(2000)); // check original flush succeeded
			
			long end = System.currentTimeMillis();
			long duration = end - start;
			
			assertGreater(duration, 400L);
			assertLess(duration, 1000L);
			status = cache.getStatus(0);
			assertEquals(status.getCurrentCacheSize(), 0);
			assertEquals(status.getCacheControlStateString(), WBRBCacheControlState.SHUTDOWN_COMPLETED.name());
			assertStorageMapContentsExactlyEquals(cache.getStorageDataMap(), key, "u", key2, "v");
		}
		
		
		{
			// Test slow flush + not enough time to flush
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			
			final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(name + (testIndex++), config, 0 /*read delay*/, 700)
				.start();
			
			final String key = "akey";
			final String key2 = "akey2";
			
			cache.readForOrException(key, 1000);
			cache.writeIfCachedOrException(key, 'u');
			cache.readForOrException(key2, 1000);
			cache.writeIfCachedOrException(key2, 'v');
			
			assertEquals(cache.getStatus(0).getCurrentCacheSize(), 2);
			
			long start = System.currentTimeMillis();
			
			AsyncTestRunner<Boolean> async = TestUtil.callAsynchronously(() -> cache.flushFor(250));
			Thread.sleep(100);
			WBRBStatus status = cache.getStatus(0);
			assertEquals(status.getCurrentCacheSize(), 2);
			assertEquals(status.getCacheControlStateString(), WBRBCacheControlState.FLUSHING.name());
			assertFailsWithSubstring(() -> cache.flushFor(200), "already flushing"); // check that we can't concurrently flush
			assertFalse(async.getResult(2000)); // check original flush DID NOT succeeded (because not enough time)
			
			long end = System.currentTimeMillis();
			long duration = end - start;
			
			assertGreater(duration, 200L);
			assertLess(duration, 500L);
			status = cache.getStatus(0);
			assertEquals(status.getCurrentCacheSize(), 2);
			assertEquals(status.getCacheControlStateString(), WBRBCacheControlState.RUNNING.name());
			
			assertTrue(cache.shutdownFor(2000)); // shutdown should spool out the data
			
			assertStorageMapContentsExactlyEquals(cache.getStorageDataMap(), key, "u", key2, "v");
		}
		
	}
	
	/**
	 * Tests some read-fail scenarios.
	 */
	@Test
	public void testReadFailsWithCacheRemove() throws InterruptedException
	{
		final String name = "testReadFailsWithCacheRemove";
		int testIndex = 1;
		
		final String notYetLoadedSubstring = "not yet loaded";
		final String tooManyRemovedFromCacheSubstring = "encountered REMOVED_FROM_CACHE state";
		
		FlatConfiguration baseConfig = Configuration.fromPropertiesFile("wbrb/wbrb-default.properties");
		
		{
			// Tests writing while item has not been read yet
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			
			final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(name + (testIndex++), config, 75 /*read delay*/, 0)
				.start();
			
			final String key = "akey";
			
			cache.preloadCache(key);
			
			assertFailsWithSubstring(() -> cache.writeIfCachedOrException(key, 'u'), notYetLoadedSubstring);
			
			Thread.sleep(250);
			
			cache.writeIfCachedOrException(key, 'v');
			
			assertTrue(cache.shutdownFor(2000));
			
			assertStorageMapContentsExactlyEquals(cache.getStorageDataMap(), key, "v");
		}
		
		{
			// Tests initial read failure with immediate read failures
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			config.override("eventNotificationEnabled", "true"); // set to true to enable event logging for debugging
			config.override("logThrottleMaxMessagesOfTypePerTimeInterval", "1"); // reduce spam
			
			final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(name + (testIndex++), config, 0 /*read delay*/, 0)
				{
					@Override
					protected String readFromStorage(String key,
						boolean isRefreshRead)
						throws InterruptedException
					{
						super.readFromStorage(key, isRefreshRead);
						throw new IllegalStateException("storage read failure on purpose");
					}
				}
//				.setDebugEventLogger(log)
				.start();
			
			final String key = "akey";
			
			{
				WBRBStatus status = cache.getStatus(0);
				assertEquals(status.getCheckCacheAttemptsNoDedup(), 0);
				assertEquals(status.getCheckCachePreloadAttempts(), 0);
				assertEquals(status.getCheckCacheReadAttempts(), 0);
			}
			
			cache.preloadCache(key);
			{
				WBRBStatus status = cache.getStatus(0);
				assertEquals(status.getCheckCacheAttemptsNoDedup(), 1);
				assertEquals(status.getCheckCachePreloadAttempts(), 1);
				assertEquals(status.getCheckCacheReadAttempts(), 0);
			}
			
			assertFailsWithSubstring(() -> cache.writeIfCachedOrException(key, 'u'), notYetLoadedSubstring);
			
			{
				NullableOptional<String> value = cache.readIfCached(key);
				assertTrue(value.isEmpty());
				assertFalse(value.isPresent());
				assertFalse(value.hasException());
			}
			
			assertFailsWithSubstring(() -> cache.readIfCachedOrException(key), notYetLoadedSubstring);
			
			{
				NullableOptional<String> value = cache.readFor(key, 1000);
				assertTrue(value.isEmpty());
				assertFalse(value.isPresent());
				assertContains(value.getException(), tooManyRemovedFromCacheSubstring);
			}
			final long baseCheckCacheAttemptsNoDedupCount;
			{
				WBRBStatus status = cache.getStatus(0);
				baseCheckCacheAttemptsNoDedupCount = status.getCheckCacheAttemptsNoDedup(); 
				assertGreaterOrEqual(baseCheckCacheAttemptsNoDedupCount, 6L); // +1 preload, +2 2x readIfCached, +3 readFor (including retries) 
				assertLessOrEqual(baseCheckCacheAttemptsNoDedupCount, 10L); // +1 preload, +6 2x readIfCached (incl retries), +3 readFor (including retries)
				assertEquals(status.getCheckCachePreloadAttempts(), 1);
				assertEquals(status.getCheckCacheReadAttempts(), 3);
			}
			
			{
				Throwable t = assertFails(() -> cache.readForOrException(key, 1000));
				assertContains(t, notYetLoadedSubstring);
				assertContains(t.getCause(), tooManyRemovedFromCacheSubstring);
			}
			{
				WBRBStatus status = cache.getStatus(0);
				assertEquals(status.getCheckCacheAttemptsNoDedup(), baseCheckCacheAttemptsNoDedupCount + 3); 
				assertEquals(status.getCheckCachePreloadAttempts(), 1);
				assertEquals(status.getCheckCacheReadAttempts(), 4);
			}
			
			assertFailsWithSubstring(() -> cache.writeIfCachedOrException(key, 'u'), notYetLoadedSubstring);
			
			assertTrue(cache.shutdownFor(2000));
		}

		
		{
			// Tests initial read failure where read failing takes some time
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			config.override("eventNotificationEnabled", "true"); // set to true to enable event logging for debugging
			config.override("logThrottleMaxMessagesOfTypePerTimeInterval", "1"); // reduce spam
			
			final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(name + (testIndex++), config, 40 /*read delay*/, 0)
				{
					@Override
					protected String readFromStorage(String key,
						boolean isRefreshRead)
						throws InterruptedException
					{
						super.readFromStorage(key, isRefreshRead);
						throw new IllegalStateException("storage read failure on purpose");
					}
				}
				.setDebugEventLogger(log)
				.start();
			
			final String key = "akey";
			
			{
				WBRBStatus status = cache.getStatus(0);
				assertEquals(status.getCheckCacheAttemptsNoDedup(), 0);
				assertEquals(status.getCheckCachePreloadAttempts(), 0);
				assertEquals(status.getCheckCacheReadAttempts(), 0);
			}
			
			cache.preloadCache(key);
			{
				WBRBStatus status = cache.getStatus(0);
				assertEquals(status.getCheckCacheAttemptsNoDedup(), 1);
				assertEquals(status.getCheckCachePreloadAttempts(), 1);
				assertEquals(status.getCheckCacheReadAttempts(), 0);
			}
			
			assertFailsWithSubstring(() -> cache.writeIfCachedOrException(key, 'u'), notYetLoadedSubstring);
			
			{
				NullableOptional<String> value = cache.readIfCached(key);
				assertTrue(value.isEmpty());
				assertFalse(value.isPresent());
				assertFalse(value.hasException());
			}
			
			assertFailsWithSubstring(() -> cache.readIfCachedOrException(key), notYetLoadedSubstring);
			
			{
				NullableOptional<String> value = cache.readFor(key, 70); // short delay, so this should fail on timeout, not 'too many removed from cache'
				assertTrue(value.isEmpty());
				assertFalse(value.isPresent());
				assertFalse(value.hasException());
			}
			
			{
				Throwable t = assertFails(() -> cache.readForOrException(key, 70)); // short delay, so this should fail on timeout, not 'too many removed from cache'
				assertContains(t, notYetLoadedSubstring);
				assertNull(t.getCause());
			}
			
			{
				NullableOptional<String> value = cache.readFor(key, 1000);
				assertTrue(value.isEmpty());
				assertFalse(value.isPresent());
				assertContains(value.getException(), tooManyRemovedFromCacheSubstring);
			}
			final long baseCheckCacheAttemptsNoDedupCount;
			{
				WBRBStatus status = cache.getStatus(0);
				baseCheckCacheAttemptsNoDedupCount = status.getCheckCacheAttemptsNoDedup(); 
				assertGreaterOrEqual(baseCheckCacheAttemptsNoDedupCount, 8L); // +1 preload, +2 2x readIfCached, +2 2x short readFor +3 long readFor (including retries) 
				assertLessOrEqual(baseCheckCacheAttemptsNoDedupCount, 16L); // +1 preload, +6 2x readIfCached, +6 2x short readFor +3 long readFor (including retries)
				assertEquals(status.getCheckCachePreloadAttempts(), 1);
				assertEquals(status.getCheckCacheReadAttempts(), 5);
			}
			
			{
				Throwable t = assertFails(() -> cache.readForOrException(key, 2000));
				assertContains(t, notYetLoadedSubstring);
				assertContains(t.getCause(), tooManyRemovedFromCacheSubstring);
			}
			{
				WBRBStatus status = cache.getStatus(0);
				assertEquals(status.getCheckCacheAttemptsNoDedup(), baseCheckCacheAttemptsNoDedupCount + 3); 
				assertEquals(status.getCheckCachePreloadAttempts(), 1);
				assertEquals(status.getCheckCacheReadAttempts(), 6);
			}
			
			assertFailsWithSubstring(() -> cache.writeIfCachedOrException(key, 'u'), notYetLoadedSubstring);
			
			assertTrue(cache.shutdownFor(2000));
		}
	}

	/**
	 * Tests some read-fail scenarios.
	 */
	@Test
	public void testReadFailsWithSetFailedStatus()
	{
		final String name = "testReadFailsWithSetFailedStatus";
		int testIndex = 1;
		
		final String notYetLoadedSubstring = "not yet loaded";
//		final String tooManyRemovedFromCacheSubstring = "encountered REMOVED_FROM_CACHE state";
		final String failedToLoadSubstring = "failed to load";
		
		FlatConfiguration baseConfig = Configuration.fromPropertiesFile("wbrb/wbrb-default.properties");
		
		
		{
			// Tests initial read failure with immediate read failures
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			config.override("eventNotificationEnabled", "true"); // set to true to enable event logging for debugging
			config.override("logThrottleMaxMessagesOfTypePerTimeInterval", "2"); // reduce spam
			config.override("initialReadFailedFinalAction", "KEEP_AND_THROW_CACHE_READ_EXCEPTIONS"); // failed reads are retained in the cache
			
			final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(name + (testIndex++), config, 0 /*read delay*/, 0)
				{
					@Override
					protected String readFromStorage(String key,
						boolean isRefreshRead)
						throws InterruptedException
					{
						super.readFromStorage(key, isRefreshRead);
						throw new IllegalStateException("storage read failure on purpose");
					}
				}
//				.setDebugEventLogger(log)
				.start();
			
			final String key = "akey";
			
			{
				WBRBStatus status = cache.getStatus(0);
				assertEquals(status.getCheckCacheAttemptsNoDedup(), 0);
				assertEquals(status.getCheckCachePreloadAttempts(), 0);
				assertEquals(status.getCheckCacheReadAttempts(), 0);
			}
			
			cache.preloadCache(key);
			{
				WBRBStatus status = cache.getStatus(0);
				assertEquals(status.getCheckCacheAttemptsNoDedup(), 1);
				assertEquals(status.getCheckCachePreloadAttempts(), 1);
				assertEquals(status.getCheckCacheReadAttempts(), 0);
			}
			
			// Not sure if these are reliably failing with not-loaded-yet, might be failed loading too?
			assertFailsWithSubstring(() -> cache.writeIfCachedOrException(key, 'u'), notYetLoadedSubstring);
			{
				NullableOptional<Boolean> value = cache.writeIfCached(key, 'u');
				assertTrue(value.isEmpty());
				assertFalse(value.isPresent());
				assertFalse(value.hasException());
			}
			
			{
				NullableOptional<String> value = cache.readIfCached(key);
				assertTrue(value.isEmpty());
				assertFalse(value.isPresent());
				assertFalse(value.hasException());
			}
			
			assertFailsWithSubstring(() -> cache.readIfCachedOrException(key), notYetLoadedSubstring);
			
			{
				NullableOptional<String> value = cache.readFor(key, 1000);
				assertTrue(value.isEmpty());
				assertFalse(value.isPresent());
				assertContains(value.getException(), failedToLoadSubstring);
			}
			
			// At this point item should be 'failed loading'
			{
				NullableOptional<String> value = cache.readIfCached(key);
				assertTrue(value.isEmpty());
				assertFalse(value.isPresent());
				assertContains(value.getException(), failedToLoadSubstring);
			}
			assertFailsWithSubstring(() -> cache.readIfCachedOrException(key), failedToLoadSubstring);
			{
				NullableOptional<String> value = cache.readUntil(key, System.currentTimeMillis() + 10);
				assertTrue(value.isEmpty());
				assertFalse(value.isPresent());
				assertContains(value.getException(), failedToLoadSubstring);
			}
			assertFailsWithSubstring(() -> cache.readUntilOrException(key, System.currentTimeMillis() + 10), failedToLoadSubstring);
			
			final long baseCheckCacheAttemptsNoDedupCount;
			{
				WBRBStatus status = cache.getStatus(0);
				baseCheckCacheAttemptsNoDedupCount = status.getCheckCacheAttemptsNoDedup(); 
				assertEquals(baseCheckCacheAttemptsNoDedupCount, 8L); // +1 preload, +4 4x readIfCached, +1 readFor, +2 2x readUntil 
//				assertLessOrEqual(baseCheckCacheAttemptsNoDedupCount, 10L); // +1 preload, +6 2x readIfCached (incl retries), +3 readFor (including retries)
				assertEquals(status.getCheckCachePreloadAttempts(), 1);
				assertEquals(status.getCheckCacheReadAttempts(), 7);
			}
			
			{
				Throwable t = assertFails(() -> cache.readForOrException(key, 1000));
				assertContains(t, failedToLoadSubstring);
				assertNull(t.getCause());
			}
			{
				WBRBStatus status = cache.getStatus(0);
				assertEquals(status.getCheckCacheAttemptsNoDedup(), baseCheckCacheAttemptsNoDedupCount + 1); 
				assertEquals(status.getCheckCachePreloadAttempts(), 1);
				assertEquals(status.getCheckCacheReadAttempts(), 8);
			}
			
			assertFailsWithSubstring(() -> cache.writeIfCachedOrException(key, 'u'), failedToLoadSubstring);
			{
				NullableOptional<Boolean> value = cache.writeIfCached(key, 'u');
				assertTrue(value.isEmpty());
				assertFalse(value.isPresent());
				assertContains(value.getException(), failedToLoadSubstring);
				assertNull(value.getException().getCause());
			}
			
			assertTrue(cache.shutdownFor(2000));
		}
		
		
		
		{
			// Tests initial read failure with delayed read failures
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			config.override("eventNotificationEnabled", "true"); // set to true to enable event logging for debugging
			config.override("logThrottleMaxMessagesOfTypePerTimeInterval", "2"); // reduce spam
			config.override("initialReadFailedFinalAction", "KEEP_AND_THROW_CACHE_READ_EXCEPTIONS"); // failed reads are retained in the cache
			
			final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(name + (testIndex++), config, 40 /*read delay*/, 0)
				{
					@Override
					protected String readFromStorage(String key,
						boolean isRefreshRead)
						throws InterruptedException
					{
						super.readFromStorage(key, isRefreshRead);
						throw new IllegalStateException("storage read failure on purpose");
					}
				}
//				.setDebugEventLogger(log)
				.start();
			
			final String key = "akey";
			
			{
				WBRBStatus status = cache.getStatus(0);
				assertEquals(status.getCheckCacheAttemptsNoDedup(), 0);
				assertEquals(status.getCheckCachePreloadAttempts(), 0);
				assertEquals(status.getCheckCacheReadAttempts(), 0);
			}
			
			cache.preloadCache(key);
			{
				WBRBStatus status = cache.getStatus(0);
				assertEquals(status.getCheckCacheAttemptsNoDedup(), 1);
				assertEquals(status.getCheckCachePreloadAttempts(), 1);
				assertEquals(status.getCheckCacheReadAttempts(), 0);
			}
			
			// Not sure if these are reliably failing with not-loaded-yet, might be failed loading too?
			assertFailsWithSubstring(() -> cache.writeIfCachedOrException(key, 'u'), notYetLoadedSubstring);
			{
				NullableOptional<Boolean> value = cache.writeIfCached(key, 'u');
				assertTrue(value.isEmpty());
				assertFalse(value.isPresent());
				assertFalse(value.hasException());
			}
			
			{
				NullableOptional<String> value = cache.readIfCached(key);
				assertTrue(value.isEmpty());
				assertFalse(value.isPresent());
				assertFalse(value.hasException());
			}
			
			assertFailsWithSubstring(() -> cache.readIfCachedOrException(key), notYetLoadedSubstring);
			
			{
				NullableOptional<String> value = cache.readFor(key, 70); // short delay, so this should fail on timeout, not 'too many removed from cache'
				assertTrue(value.isEmpty());
				assertFalse(value.isPresent());
				assertFalse(value.hasException());
			}
			
			{
				Throwable t = assertFails(() -> cache.readForOrException(key, 70)); // short delay, so this should fail on timeout, not 'too many removed from cache'
				assertContains(t, notYetLoadedSubstring);
				assertNull(t.getCause());
			}
			
			{
				NullableOptional<String> value = cache.readFor(key, 1000);
				assertTrue(value.isEmpty());
				assertFalse(value.isPresent());
				assertContains(value.getException(), failedToLoadSubstring);
			}
			
			// At this point item should be 'failed loading'
			{
				NullableOptional<String> value = cache.readIfCached(key);
				assertTrue(value.isEmpty());
				assertFalse(value.isPresent());
				assertContains(value.getException(), failedToLoadSubstring);
			}
			assertFailsWithSubstring(() -> cache.readIfCachedOrException(key), failedToLoadSubstring);
			{
				NullableOptional<String> value = cache.readUntil(key, System.currentTimeMillis() + 10);
				assertTrue(value.isEmpty());
				assertFalse(value.isPresent());
				assertContains(value.getException(), failedToLoadSubstring);
			}
			assertFailsWithSubstring(() -> cache.readUntilOrException(key, System.currentTimeMillis() + 10), failedToLoadSubstring);
			
			final long baseCheckCacheAttemptsNoDedupCount;
			{
				WBRBStatus status = cache.getStatus(0);
				baseCheckCacheAttemptsNoDedupCount = status.getCheckCacheAttemptsNoDedup(); 
				assertEquals(baseCheckCacheAttemptsNoDedupCount, 10L); // +1 preload, +4 4x readIfCached, +3 readFor, +2 2x readUntil 
//				assertLessOrEqual(baseCheckCacheAttemptsNoDedupCount, 10L); // +1 preload, +6 2x readIfCached (incl retries), +3 readFor (including retries)
				assertEquals(status.getCheckCachePreloadAttempts(), 1);
				assertEquals(status.getCheckCacheReadAttempts(), 9);
			}
			
			{
				Throwable t = assertFails(() -> cache.readForOrException(key, 1000));
				assertContains(t, failedToLoadSubstring);
				assertNull(t.getCause());
			}
			{
				WBRBStatus status = cache.getStatus(0);
				assertEquals(status.getCheckCacheAttemptsNoDedup(), baseCheckCacheAttemptsNoDedupCount + 1); 
				assertEquals(status.getCheckCachePreloadAttempts(), 1);
				assertEquals(status.getCheckCacheReadAttempts(), 10);
			}
			
			assertFailsWithSubstring(() -> cache.writeIfCachedOrException(key, 'u'), failedToLoadSubstring);
			{
				NullableOptional<Boolean> value = cache.writeIfCached(key, 'u');
				assertTrue(value.isEmpty());
				assertFalse(value.isPresent());
				assertContains(value.getException(), failedToLoadSubstring);
				assertNull(value.getException().getCause());
			}
			
			assertTrue(cache.shutdownFor(2000));
		}
	}
	
	/**
	 * Tests success scenario for {@link WBRBReadBeforeWriteCache}
	 */
	@Test
	public void testReadBeforeWriteSuccessScenario()
		throws InterruptedException, TimeoutException, ExecutionException
	{
		final String baseTestName = "testReadBeforeWriteSuccessScenario";
		int testIndex = 1;
		
		FlatConfiguration baseConfig = Configuration.fromPropertiesFile("wbrb/wbrb-default.properties");
		
		
		{
			final String testName = baseTestName + (testIndex++);
			
			// Control debug logging/event tracing to file.
			final boolean debugLogging;
			{
				boolean f = false;
//				f = true; {} // uncomment this to enable debug logging; empty block produces warning in order to not forget to re-comment
				debugLogging = f;
			}
			
			// Tests initial read failure with immediate read failures
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			config.override("eventNotificationEnabled", "" + debugLogging); // set to true to enable event logging for debugging
			
			AtomicBoolean hadAsyncFailures = new AtomicBoolean(false);
			
			// Use shared storage
			final TestWBRBReadBeforeWriteCache cache1 = new TestWBRBReadBeforeWriteCache(testName + ":1", config, false, 20, 20)
				.start();
			final TestWBRBReadBeforeWriteCache cache2 = new TestWBRBReadBeforeWriteCache(testName + ":2", config, false, cache1.getStorageDataMap(), 20, 20)
				.start();
			if (debugLogging)
			{
				cache1.setDebugLogger(testFileLog);
				cache2.setDebugLogger(testFileLog);
			}
			
			final BlockingQueue<UpdateEntry> updatesQueue1 = new LinkedBlockingQueue<>();
			final BlockingQueue<UpdateEntry> updatesQueue2 = new LinkedBlockingQueue<>();
			
			final long startTime = System.currentTimeMillis();
			final long duration = 10000;
			final AtomicBoolean secondPartOfTheTest = new AtomicBoolean(false);
			
			final Latch startupLatch = new Latch(false); 
			
			// Launch all the executors
			WAThreadPoolExecutor pool = new WAThreadPoolExecutor(testName + "-executors", true);
			{
				final int threadCount = 300;
				for (int i = 0; i < threadCount; i++)
				{
					final BlockingQueue<UpdateEntry> updatesQueue;
					final TestWBRBReadBeforeWriteCache cache;
					if (i < (threadCount / 2))
					{
						updatesQueue = updatesQueue1;
						cache = cache1;
					}
					else
					{
						updatesQueue = updatesQueue2; 
						cache = cache2;
					}
					pool.waSubmit(() -> {
						try
						{
							startupLatch.await();
							
							while (true)
							{
								boolean secondPart = secondPartOfTheTest.get();
								UpdateEntry update = updatesQueue.take();
								final String key = update.getKey();
								if (secondPart)
								{
									String data = cache.readForOrException(key, 200);
									// Check that we have a mix of data from both caches if we have 'a bunch' of data.
									if (data.length() > 25) // note that this is a probabilistic check, adjust it if it doesn't work well
										assertNotEquals(data.toLowerCase(), data.toUpperCase());
								}
								else
									cache.readForOrException(key, 1500); // this can be slow on startup, particularly under debug
								Thread.sleep(100);
								cache.writeIfCachedOrException(key, update.getUpdate());
							}
						} catch(InterruptedException e)
						{
							throw new WAInterruptedException(e);
						} catch (Throwable e)
						{
							log.error("[async executor] " + testName + " failed: " + e, e);
							hadAsyncFailures.set(true);
						}
					});
				}
			}
			
			Map<String, String> dataMap1 = new HashMap<String, String>();
			
			final int stepAmount = 40;
			
			// Single update
			for (int i = 0; i < stepAmount; i++)
				generateXUpdatesWithNewKey(1, dataMap1, true);
			
			// Two updates
			for (int i = 0; i < stepAmount; i++)
				generateXUpdatesWithNewKey(2, dataMap1, true);
			
			// Three updates
			for (int i = 0; i < stepAmount; i++)
				generateXUpdatesWithNewKey(3, dataMap1, true);
			
			// 10 updates
			for (int i = 0; i < stepAmount; i++)
				generateXUpdatesWithNewKey(10, dataMap1, true);
			
			// 50 updates
			for (int i = 0; i < stepAmount; i++)
				generateXUpdatesWithNewKey(50, dataMap1, true);
			
			// 100 updates
			for (int i = 0; i < stepAmount; i++)
				generateXUpdatesWithNewKey(100, dataMap1, true);

//			// 1000 updates
//			generateXUpdatesWithNewKey(1000, dataMap);
//			
//			// 2000 updates
//			generateXUpdatesWithNewKey(2000, dataMap);
			
			Map<String, String> dataMap2 = new HashMap<String, String>(dataMap1.size());
			Map<String, String> resultsMap = new HashMap<String, String>(dataMap1.size());
			for (Entry<String, String> entry : dataMap1.entrySet())
			{
				final String key = entry.getKey();
				final String value = entry.getValue();
				final String newValue = value.toLowerCase();
				assertNotEquals(value, newValue);
				assertNull(dataMap2.put(key, newValue));
				assertNull(resultsMap.put(key, value + newValue));
			}
			
			ArrayList<UpdateEntry> updatesList1 = generateRandomizedUpdateList(dataMap1);
			ArrayList<UpdateEntry> updatesList2 = generateRandomizedUpdateList(dataMap2);
			
			// Send updates
			startupLatch.open();
			AsyncTestRunner<Void> setSecondTestPartFlagFuture = TestUtil.runAsynchronously(
				() -> {Thread.sleep(6000); secondPartOfTheTest.set(true);}); // sleep 6000 because main queue in 4000 long
			AsyncTestRunner<Void> spool1Future = TestUtil.runAsynchronously(
				() -> spoolUpdatesList(testName + ":1", true, updatesQueue1, updatesList1, startTime, duration));
			spoolUpdatesList(testName + ":2", true, updatesQueue2, updatesList2, startTime, duration);
			
			try
			{
				spool1Future.getResult(startTime + duration + 2000 - System.currentTimeMillis());
			} catch (Exception e)
			{
				throw new RuntimeException("Failed to properly complete data spool: " + e, e);
			}
			
			setSecondTestPartFlagFuture.getResult(0); // will fail if not completed
			
			assert cache1.shutdownFor(3000) : "Cache failed to shutdown in time, still has items: " + cache1.inflightMap.size();
			assert cache2.shutdownFor(3000) : "Cache failed to shutdown in time, still has items: " + cache2.inflightMap.size();
			
			log.info("Waiting done, checking result");
			
			assertFalse(hadAsyncFailures.get(), "Had asynchronous processing failures, check logs (search for '[async executor]')");
			
			assertEquals(cache1.inflightMap.size(), 0); 
			assertEquals(cache2.inflightMap.size(), 0); 
			
			// same storage, so either will do
			assertStorageMapContentsHasSameUpdates(cache1.getStorageDataMap(), resultsMap);
			
			pool.shutdownNow();
		}
		
	}

	
	/**
	 * Tests basic write functionality
	 */
	@Test
	public void testBasicWriteFunctionality()
	{
		final String baseTestName = "testBasicWriteFunctionality";
		int testIndex = 1;
		
		FlatConfiguration baseConfig = Configuration.fromPropertiesFile("wbrb/wbrb-default.properties");
		
		
		{
			final String testName = baseTestName + (testIndex++);
			
			// Control debug logging/event tracing to file.
			final boolean debugLogging;
			{
				boolean f = false;
//				f = true; {} // uncomment this to enable debug logging; empty block produces warning in order to not forget to re-comment
				debugLogging = f;
			}
			
			// Tests initial read failure with immediate read failures
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			config.override("eventNotificationEnabled", "" + debugLogging); // set to true to enable event logging for debugging
			
			final String writeFailSubstring = "'v' is not allowed for write";
			final String readFailSubstring = "Unable to read stuff ending with 'f':";
			final String notYetLoadedSubstring = "NotYetLoaded";
			final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(testName, config, 0 /*read delay*/, 0)
				{
					@Override
					protected StringBuilder applyUpdate(StringBuilder cacheData,
						Character update)
					{
						if (update == 'v')
							throw new IllegalStateException(writeFailSubstring);
						return super.applyUpdate(cacheData, update);
					}

					@Override
					protected String convertFromCacheFormatToReturnValue(
						String key, StringBuilder cachedData)
					{
						if (cachedData.toString().endsWith("f"))
							throw new IllegalStateException(readFailSubstring + " " + cachedData.toString());
						return super.convertFromCacheFormatToReturnValue(key, cachedData);
					}
				}
//				.setDebugEventLogger(log)
				.start();
				
			{
				// Basic tests for writing something that doesn't exist in cache
				final String keyNotExists = "key-not-exists";
				
				assertTrue(cache.writeIfCached(keyNotExists, 'u').isEmpty());
				assertTrue(cache.writeIfCachedAndRead(keyNotExists, 'u').isEmpty());
				assertFailsWithSubstring(() -> cache.writeIfCachedOrException(keyNotExists, 'u'), notYetLoadedSubstring);
				assertFailsWithSubstring(() -> cache.writeIfCachedAndReadOrException(keyNotExists, 'u'), notYetLoadedSubstring);
			}
			
			int keyIndex = 1;
			
			{
				// Test writeIfCached
				final String key = "key" + (keyIndex++);
				
				assertTrue(cache.writeIfCached(key, 'u').isEmpty());
				assertEquals(cache.readForOrException(key, 1000), "");

				assertTrue(cache.writeIfCached(key, 'u').isPresent());
				assertTrue(cache.writeIfCached(key, 'x').isPresent());
				assertEquals(cache.readIfCached(key).get(), "ux");
				
				// Failing write
				assertFailsWithSubstring(() -> cache.writeIfCached(key, 'v'), writeFailSubstring);
				assertEquals(cache.readIfCached(key).get(), "ux");
				
				// Flush
				assertTrue(cache.flushFor(1000));
				
				// Failing read leading to cache removal
				assertEquals(cache.readForOrException(key, 1000), "ux");
				assertTrue(cache.writeIfCached(key, 'y').isPresent());
				assertEquals(cache.readIfCached(key).get(), "uxy");
				assertTrue(cache.writeIfCached(key, 'f').isPresent());
				assertFailsWithSubstring(() -> cache.readIfCached(key), readFailSubstring);
				assertTrue(cache.readIfCached(key).isEmpty());
				
				// Check previously flushed data is still ok
				assertEquals(cache.readForOrException(key, 1000), "ux");
				assertTrue(cache.writeIfCached(key, 'z').isPresent());
				assertEquals(cache.readIfCached(key).get(), "uxz");
			}
			
			{
				// Test writeIfCachedOrException
				final String key = "key" + (keyIndex++);
				
				assertFailsWithSubstring(() -> cache.writeIfCachedOrException(key, 'u'), notYetLoadedSubstring);
				assertEquals(cache.readForOrException(key, 1000), "");

				cache.writeIfCachedOrException(key, 'u'); // exception if fails
				cache.writeIfCachedOrException(key, 'x'); // exception if fails
				assertEquals(cache.readIfCached(key).get(), "ux");
				
				// Failing write
				assertFailsWithSubstring(() -> cache.writeIfCachedOrException(key, 'v'), writeFailSubstring);
				assertEquals(cache.readIfCached(key).get(), "ux");
				
				// Flush
				assertTrue(cache.flushFor(1000));
				
				// Failing read leading to cache removal
				assertEquals(cache.readForOrException(key, 1000), "ux");
				cache.writeIfCachedOrException(key, 'y'); // exception if fails
				assertEquals(cache.readIfCached(key).get(), "uxy");
				cache.writeIfCachedOrException(key, 'f'); // exception if fails
				assertFailsWithSubstring(() -> cache.readIfCached(key), readFailSubstring);
				assertTrue(cache.readIfCached(key).isEmpty());
				
				// Check previously flushed data is still ok
				assertEquals(cache.readForOrException(key, 1000), "ux");
				cache.writeIfCachedOrException(key, 'z'); // exception if fails
				assertEquals(cache.readIfCached(key).get(), "uxz");
			}
			
			{
				// Test writeIfCachedAndRead
				final String key = "key" + (keyIndex++);
				
				assertTrue(cache.writeIfCachedAndRead(key, 'u').isEmpty());
				assertEquals(cache.readForOrException(key, 1000), "");

				assertEquals(cache.writeIfCachedAndRead(key, 'u').get(), "u");
				assertEquals(cache.writeIfCachedAndRead(key, 'x').get(), "ux");
				assertEquals(cache.readIfCached(key).get(), "ux");
				
				// Failing write
				assertFailsWithSubstring(() -> cache.writeIfCachedAndRead(key, 'v'), writeFailSubstring);
				assertEquals(cache.readIfCached(key).get(), "ux");
				
				// Flush
				assertTrue(cache.flushFor(1000));
				
				// Failing read leading to cache removal
				assertEquals(cache.readForOrException(key, 1000), "ux");
				assertEquals(cache.writeIfCachedAndRead(key, 'y').get(), "uxy");
				assertEquals(cache.readIfCached(key).get(), "uxy");
				assertFailsWithSubstring(() -> cache.writeIfCachedAndRead(key, 'f'), readFailSubstring);
				assertTrue(cache.readIfCached(key).isEmpty());
				
				// Check previously flushed data is still ok
				assertEquals(cache.readForOrException(key, 1000), "ux");
				assertEquals(cache.writeIfCachedAndRead(key, 'z').get(), "uxz");
				assertEquals(cache.readIfCached(key).get(), "uxz");
			}
			
			{
				// Test writeIfCachedAndReadOrException
				final String key = "key" + (keyIndex++);
				
				assertFailsWithSubstring(() -> cache.writeIfCachedAndReadOrException(key, 'u'), notYetLoadedSubstring);
				assertEquals(cache.readForOrException(key, 1000), "");

				assertEquals(cache.writeIfCachedAndReadOrException(key, 'u'), "u");
				assertEquals(cache.writeIfCachedAndReadOrException(key, 'x'), "ux");
				assertEquals(cache.readIfCached(key).get(), "ux");
				
				// Failing write
				assertFailsWithSubstring(() -> cache.writeIfCachedAndReadOrException(key, 'v'), writeFailSubstring);
				assertEquals(cache.readIfCached(key).get(), "ux");
				
				// Flush
				assertTrue(cache.flushFor(1000));
				
				// Failing read leading to cache removal
				assertEquals(cache.readForOrException(key, 1000), "ux");
				assertEquals(cache.writeIfCachedAndReadOrException(key, 'y'), "uxy");
				assertEquals(cache.readIfCached(key).get(), "uxy");
				assertFailsWithSubstring(() -> cache.writeIfCachedAndReadOrException(key, 'f'), readFailSubstring);
				assertTrue(cache.readIfCached(key).isEmpty());
				
				// Check previously flushed data is still ok
				assertEquals(cache.readForOrException(key, 1000), "ux");
				assertEquals(cache.writeIfCachedAndReadOrException(key, 'z'), "uxz");
				assertEquals(cache.readIfCached(key).get(), "uxz");
			}
			
			assert cache.shutdownFor(3000) : "Cache failed to shutdown in time, still has items: " + cache.inflightMap.size();
			
			final String value = "uxz";
			assertStorageMapContentsExactlyEquals(cache.getStorageDataMap()
				,"key1", value
				,"key2", value
				,"key3", value
				,"key4", value
				);
		}
	}

	/**
	 * Tests functionality of non-standard message logging.
	 * @throws InterruptedException 
	 * @throws IllegalStateException 
	 */
	@Test
	public void testNonStandardMessagesLogging() throws IllegalStateException, InterruptedException
	{
		final String baseTestName = "testNonStandardMessagesLogging";
		int testIndex = 1;
		
		FlatConfiguration baseConfig = Configuration.fromPropertiesFile("wbrb/wbrb-default.properties");
		
		
		{
			final String testName = baseTestName + (testIndex++);
			
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			config.override("logThrottleMaxMessagesOfTypePerTimeInterval", "2"); // reduce spam
			config.override("logThrottleTimeInterval", "200ms"); // low time to avoid having to wait too long
			
			// 3rd argument is classifier -- more specifically -- extracted first argument
			ArrayBlockingQueue<Quartet<WBRBCacheMessage, @Nullable Throwable, @Nullable String, Object[]>> msgQueue = new ArrayBlockingQueue<>(100);
			
			final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(testName, config, 20, 20)
				{

					@Override
					protected void spiUnknownLockLogMessage_Plain(Logger argLog,
						WBRBCacheMessage msg,
						@Nullable Throwable exception,
						@Nonnull Object @Nonnull... args)
						throws InterruptedException
					{
						String classifier = null;
						if (args.length > 0)
						{
							Object o = args[0];
							if (o instanceof String)
								classifier = (String)o;
						}
						msgQueue.put(new Quartet<>(msg, exception, classifier, args));
						
						super.spiUnknownLockLogMessage_Plain(argLog, msg, exception, args);
					}
					
				}
				.start();
			
			{
				Quartet<WBRBCacheMessage, @Nullable Throwable, @Nullable String, Object[]> msg = 
					nnChecked(msgQueue.poll(200, TimeUnit.MILLISECONDS));
				assertEquals(msg.getValue0(), WBRBCacheMessage.STARTED);
			}
				
			{
				// Test that severity & arguments match.
				int counter = -1;
				final String classifier = "severityTest";
				final RuntimeException sampleException = new RuntimeException("sample");
				for (WBRBCacheMessageSeverity severity : WBRBCacheMessageSeverity.values())
				{
					counter++;
					log.info("Tesing [" + counter + "] " + severity);
					
					boolean testException = (counter < 3);
					String fullClassifier = classifier;// + '_' + severity;
					Object args[] = new @Nonnull String[counter];
					Object fullArgs[] = new @Nonnull String[counter + 1];
					fullArgs[0] = fullClassifier;
					for (int i = 0; i < counter; i++)
					{
						args[i] = "arg" + i;
						fullArgs[i + 1] = args[i];
					}
					
					if (args.length > 0)
						cache.logNonStandardMessage(severity, classifier, testException ? sampleException : null, args);
					else
						cache.logNonStandardMessage(severity, classifier, testException ? sampleException : null);
					
					{
						Quartet<WBRBCacheMessage, @Nullable Throwable, @Nullable String, Object[]> msg = 
							nnChecked(msgQueue.poll(200, TimeUnit.MILLISECONDS));
						assertEquals(msg.getValue0().getSeverity(), severity);
						assertEquals(msg.getValue1(), testException ? sampleException : null);
						assertEquals(msg.getValue2(), fullClassifier);
						assertEquals(Arrays.asList(msg.getValue3()), Arrays.asList(fullArgs));
					}
				}
			}
			
			{
				// Test throttling.
				final String classifier = "throttlingTest";
				final String otherClassifier = "otherClassifier";
				// Test with two severities as they should be throttled separately
				WBRBCacheMessageSeverity[] severities = new WBRBCacheMessageSeverity[] {WBRBCacheMessageSeverity.INFO, WBRBCacheMessageSeverity.WARN};
				
				// Check messages are throttled after 2 (per severity)
				for (int i = 1; i <= 5; i++)
				{
					for (WBRBCacheMessageSeverity severity : severities)
					{
						cache.logNonStandardMessage(severity, classifier, null);
						if (i <= 2)
						{
							Quartet<WBRBCacheMessage, @Nullable Throwable, @Nullable String, Object[]> msg = 
								nnChecked(msgQueue.poll(200, TimeUnit.MILLISECONDS));
							assertEquals(msg.getValue0().getSeverity(), severity);
							assertEquals(msg.getValue2(), classifier);
						}
						if (i == 2)
						{
							String classifierWithSeverity = classifier + '_' + severity;
							
							{
								Quartet<WBRBCacheMessage, @Nullable Throwable, @Nullable String, Object[]> msg = 
									nnChecked(msgQueue.poll(200, TimeUnit.MILLISECONDS));
								assertEquals(msg.getValue0(), WBRBCacheMessage.LOG_MESSAGE_TYPE_MESSAGES_MAY_BE_SKIPPED_FOR);
								assertEquals(msg.getValue2(), classifierWithSeverity);
							}
							
							// Check other classifier is not throttled
							cache.logNonStandardMessage(severity, otherClassifier, null);
							{
								Quartet<WBRBCacheMessage, @Nullable Throwable, @Nullable String, Object[]> msg = 
									nnChecked(msgQueue.poll(200, TimeUnit.MILLISECONDS));
								assertEquals(msg.getValue0().getSeverity(), severity);
								assertEquals(msg.getValue2(), otherClassifier);
							}
						}
					}
				}
				
				 // wait and check there are no other messages
				assertNull(msgQueue.poll(250, TimeUnit.MILLISECONDS));
				
				// After throttle interval
				for (WBRBCacheMessageSeverity severity : severities)
				{
					String classifierWithSeverity = classifier + '_' + severity;
					
					cache.logNonStandardMessage(severity, classifier, null);
					{
						Quartet<WBRBCacheMessage, @Nullable Throwable, @Nullable String, Object[]> msg = 
							nnChecked(msgQueue.poll(200, TimeUnit.MILLISECONDS));
						assertEquals(msg.getValue0(), WBRBCacheMessage.LOG_MESSAGE_TYPE_PREVIOUS_MESSAGES_SKIPPED);
						assertEquals(msg.getValue2(), classifierWithSeverity);
						assertEquals(msg.getValue3()[1], 3);
					}
					{
						Quartet<WBRBCacheMessage, @Nullable Throwable, @Nullable String, Object[]> msg = 
							nnChecked(msgQueue.poll(200, TimeUnit.MILLISECONDS));
						assertEquals(msg.getValue0().getSeverity(), severity);
						assertEquals(msg.getValue2(), classifier);
					}
				}
			}
			
			assert cache.shutdownFor(3000) : "Cache failed to shutdown in time, still has items: " + cache.inflightMap.size();
			{
				Quartet<WBRBCacheMessage, @Nullable Throwable, @Nullable String, Object[]> msg = 
					nnChecked(msgQueue.poll(200, TimeUnit.MILLISECONDS));
				assertEquals(msg.getValue0(), WBRBCacheMessage.SHUTDOWN_REQUESTED);
			}
			{
				Quartet<WBRBCacheMessage, @Nullable Throwable, @Nullable String, Object[]> msg = 
					nnChecked(msgQueue.poll(200, TimeUnit.MILLISECONDS));
				assertEquals(msg.getValue0(), WBRBCacheMessage.SHUTDOWN_COMPLETED);
			}
		}
	}

	/**
	 * Tests functionality of tracking messages in monitoring
	 * @throws InterruptedException 
	 * @throws IllegalStateException 
	 */
	@Test
	public void testMessagesTrackingInMonitoring() throws IllegalStateException, InterruptedException
	{
		final String baseTestName = "testMessagesTrackingInMonitoring";
		int testIndex = 1;
		
		FlatConfiguration baseConfig = Configuration.fromPropertiesFile("wbrb/wbrb-default.properties");
		
		
		{
			final String testName = baseTestName + (testIndex++);
			
			OverrideFlatConfiguration config = new OverrideFlatConfiguration(baseConfig);
			config.override("logThrottleMaxMessagesOfTypePerTimeInterval", "2"); // reduce spam
			config.override("logThrottleTimeInterval", "200ms"); // low time to avoid having to wait too long
			
			// 3rd argument is classifier -- more specifically -- extracted first argument
			ArrayBlockingQueue<Quartet<WBRBCacheMessage, @Nullable Throwable, @Nullable String, Object[]>> msgQueue = new ArrayBlockingQueue<>(100);
			
			final TestWBRBStringOverwriteCache cache = new TestWBRBStringOverwriteCache(testName, config, 20, 20)
				{

					@Override
					protected void spiUnknownLockLogMessage_Plain(Logger argLog,
						WBRBCacheMessage msg,
						@Nullable Throwable exception,
						@Nonnull Object @Nonnull... args)
						throws InterruptedException
					{
						String classifier = null;
						if (args.length > 0)
						{
							Object o = args[0];
							if (o instanceof String)
								classifier = (String)o;
						}
						msgQueue.put(new Quartet<>(msg, exception, classifier, args));
						
						super.spiUnknownLockLogMessage_Plain(argLog, msg, exception, args);
					}
					
				}
				;
			
			{
				// Test last msg timestamp / last msg text tracking. 
				final String classifier = "msgTrackingTest";
				final long[] expectedLastTimestampMsgPerSeverityOrdinal;
				final @Nullable String[] expectedLastLoggedTextMsgPerSeverityOrdinal;
				{
					final int length = cache.getStatus(30000).getLastTimestampMsgPerSeverityOrdinal().length;
					expectedLastTimestampMsgPerSeverityOrdinal = new long[length];
					expectedLastLoggedTextMsgPerSeverityOrdinal = new @Nullable String[length];
				}
				
				{
					WBRBStatus status = cache.getStatus(0);
					assertEquals(Arrays.asList(nn(ArrayUtils.toObject(status.getLastTimestampMsgPerSeverityOrdinal()))), Arrays.asList(nn(ArrayUtils.toObject(expectedLastTimestampMsgPerSeverityOrdinal))));
					assertEquals(Arrays.asList(status.getLastLoggedTextMsgPerSeverityOrdinal()), Arrays.asList(expectedLastLoggedTextMsgPerSeverityOrdinal));
				}
				
				// 'STARTED' message must be accounted for
				cache.start();
				{
					Quartet<WBRBCacheMessage, @Nullable Throwable, @Nullable String, Object[]> msg = 
						nnChecked(msgQueue.poll(200, TimeUnit.MILLISECONDS));
					assertEquals(msg.getValue0(), WBRBCacheMessage.STARTED);
					
					WBRBStatus status = cache.getStatus(0);
					int index = msg.getValue0().getSeverity().ordinal();
					expectedLastTimestampMsgPerSeverityOrdinal[index] = status.getLastTimestampMsgPerSeverityOrdinal()[index];
					expectedLastLoggedTextMsgPerSeverityOrdinal[index] = status.getLastLoggedTextMsgPerSeverityOrdinal()[index];
					
					assertEquals(Arrays.asList(nn(ArrayUtils.toObject(status.getLastTimestampMsgPerSeverityOrdinal()))), Arrays.asList(nn(ArrayUtils.toObject(expectedLastTimestampMsgPerSeverityOrdinal))));
					assertEquals(Arrays.asList(status.getLastLoggedTextMsgPerSeverityOrdinal()), Arrays.asList(expectedLastLoggedTextMsgPerSeverityOrdinal));
				}
				
				long lastWarnMsgTimestamp = 0;
				@Nullable String lastWarnLoggedMsgText = null;
				long lastErrorMsgTimestamp = 0;
				@Nullable String lastErrorLoggedMsgText = null;
				long lastFatalMsgTimestamp = 0;
				@Nullable String lastFatalLoggedMsgText = null;
				
				int count = 0;
				for (WBRBCacheMessageSeverity severity : WBRBCacheMessageSeverity.values())
				{
					log.info("Testing tracking: " + severity);
					
					count++;
					String msgArg = "message_" + count + "_";
					
					long minTimestamp = System.currentTimeMillis();
					cache.logNonStandardMessage(severity, classifier, null, msgArg);
					
					String formattedMsg;
					{
						Quartet<WBRBCacheMessage, @Nullable Throwable, @Nullable String, Object[]> msg = 
							nnChecked(msgQueue.poll(200, TimeUnit.MILLISECONDS));
						assertEquals(msg.getValue0().getSeverity(), severity);
						formattedMsg = cache.spiUnknownLockLogMessage_FormatMessage(log, msg.getValue0(), msg.getValue1(), msg.getValue3());
					}
					Thread.sleep(5);
					long maxTimestamp = System.currentTimeMillis();
					
					WBRBStatus status = cache.getStatus(0);
					
					long actualMsgTs = status.getLastTimestampMsgPerSeverityOrdinal()[severity.ordinal()];
					String actualMsgText = status.getLastLoggedTextMsgPerSeverityOrdinal()[severity.ordinal()];
					assertBetweenInclusive(actualMsgTs, minTimestamp, maxTimestamp);
					
					expectedLastTimestampMsgPerSeverityOrdinal[severity.ordinal()] = actualMsgTs;
					// Need to figure out if message was logged and act accordingly
					switch (severity)
					{
						case DEBUG:
							// These are not logged, so do nothing
							break;
						case ERROR:
						case EXTERNAL_DATA_LOSS:
						case EXTERNAL_ERROR:
						case EXTERNAL_INFO:
						case EXTERNAL_WARN:
						case FATAL:
						case INFO:
						case WARN:
							// These are logged, so update
							expectedLastLoggedTextMsgPerSeverityOrdinal[severity.ordinal()] = formattedMsg;
							break;
					}
					
					
					assertEquals(Arrays.asList(nn(ArrayUtils.toObject(status.getLastTimestampMsgPerSeverityOrdinal()))), Arrays.asList(nn(ArrayUtils.toObject(expectedLastTimestampMsgPerSeverityOrdinal))));
					assertEquals(Arrays.asList(status.getLastLoggedTextMsgPerSeverityOrdinal()), Arrays.asList(expectedLastLoggedTextMsgPerSeverityOrdinal));
					
					// Figure out which fields to update
					switch (severity)
					{
						case DEBUG:
						case EXTERNAL_INFO:
						case INFO:
							break;
						case WARN:
						case EXTERNAL_WARN:
							lastWarnMsgTimestamp = actualMsgTs;
							lastWarnLoggedMsgText = actualMsgText;
							break;
						case ERROR:
						case EXTERNAL_DATA_LOSS:
						case EXTERNAL_ERROR:
							lastErrorMsgTimestamp = actualMsgTs;
							lastErrorLoggedMsgText = actualMsgText;
							break;
						case FATAL:
							lastFatalMsgTimestamp = actualMsgTs;
							lastFatalLoggedMsgText = actualMsgText;
							break;
					}
					
					assertEquals(status.getLastWarnMsgTimestamp(), lastWarnMsgTimestamp);
					assertEquals(status.getLastWarnLoggedMsgText(), lastWarnLoggedMsgText);
					assertEquals(status.getLastErrorMsgTimestamp(), lastErrorMsgTimestamp);
					assertEquals(status.getLastErrorLoggedMsgText(), lastErrorLoggedMsgText);
					assertEquals(status.getLastFatalMsgTimestamp(), lastFatalMsgTimestamp);
					assertEquals(status.getLastFatalLoggedMsgText(), lastFatalLoggedMsgText);
				}
			}
			
			assert cache.shutdownFor(3000) : "Cache failed to shutdown in time, still has items: " + cache.inflightMap.size();
			{
				Quartet<WBRBCacheMessage, @Nullable Throwable, @Nullable String, Object[]> msg = 
					nnChecked(msgQueue.poll(200, TimeUnit.MILLISECONDS));
				assertEquals(msg.getValue0(), WBRBCacheMessage.SHUTDOWN_REQUESTED);
			}
			{
				Quartet<WBRBCacheMessage, @Nullable Throwable, @Nullable String, Object[]> msg = 
					nnChecked(msgQueue.poll(200, TimeUnit.MILLISECONDS));
				assertEquals(msg.getValue0(), WBRBCacheMessage.SHUTDOWN_COMPLETED);
			}
		}
	}
}
