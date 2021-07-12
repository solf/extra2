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

import static io.github.solf.extra2.util.NullUtil.nnChecked;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.cache.exception.CacheElementFailedWriteException;
import io.github.solf.extra2.cache.exception.CacheInternalException;
import io.github.solf.extra2.nullable.NullableOptional;
import io.github.solf.extra2.util.TypeUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * This is a version of {@link WriteBehindResyncInBackgroundCache} that implements
 * read-before-write logic -- that is all updates (hopefully) are collected
 * in-memory and during the write instead of blindly writing out current
 * in-memory state a read-update(with collected updates)-write cycle is performed
 * -- thus hopefully preserving changes made to the storage by other clients.
 * <p>
 * This read-update-write cycle is also used to resync in-memory data -- i.e.
 * it is also read-update-resync cycle. Therefore standard resyncs are only
 * performed if e.g. write was not required.
 * <p>
 * In this cache it is sort-of expected that typeof(R) == typeof(S) -- and if
 * this is the case, then {@link #convertToCacheFormatFromStorageData(Object, Object)}
 * and {@link #convertFromCacheFormatToStorageData(Object, Object)} can trivially
 * return the argument. But if needed, you can use different types too.
 *
 * @author Sergey Olefir
 * 
 * @param <K> type of the key used in this cache; must be usable as key for
 * 		HashMaps (e.g. proper hashCode() and equals())
 * @param <V> type of the values returned by this cache
 * @param <S> type of the values internally stored by the cache (it doesn't have to
 * 		be of V type)
 * @param <R> type of the values read from the underlying storage; doesn't need
 * 		to be the same as anything else, but methods must be implemented to
 * 		convert/merge it to S 
 * @param <W> type of the values written to the underlying storage; doesn't need
 * 		to be of V or S types
 * @param <UExt> type of the values used to update data in this cache externally
 * 		(in public API)
 * @param <UInt> type of the values used to store & apply updates internally
 * 		(converted from <UExt>)
 */
//Exclude TYPE_ARGUMENT as we will allow null cache values.
@NonNullByDefault({DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE, DefaultLocation.FIELD, DefaultLocation.TYPE_BOUND, DefaultLocation.ARRAY_CONTENTS}) 
public abstract class WBRBReadBeforeWriteCache<@Nonnull K, V, S, R, W, UExt, UInt>
	extends WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, WBRBReadBeforeWriteCache.@Nonnull RBWWriteData<W, UInt>, UExt, UInt>
{
	/**
	 * Whether this cache is allowed to perform item write without resyncing
	 * beforehand (e.g. when updates tracking is not available due to exceeding 
	 * maximum size or something).
	 * <p>
	 * If allowed, then a full write of in-memory data is performed that may
	 * overwrite data previously written to storage by other clients.
	 * <p>
	 * If not allowed, then all in-memory updates are lost.
	 * <p>
	 * The actual decision is taken in {@link #isAllowWritesWithoutPriorResync(Object, Object, NullableOptional, io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 */
	protected final boolean allowWritesWithoutPriorResync;
	
	/**
	 * Class for sending data for writes.
	 * <p> 
	 * Typically only one of the {@link #inMemoryState} and {@link #collectedUpdates}
	 * will be filled (non-null).
	 */
	@ToString
	protected static class RBWWriteData<W, UInt>
	{
		/**
		 * Current in-memory state with all the applied updates (to be used
		 * if updates list is not available and we just overwrite whatever
		 * is in the storage).
		 */
		@Nullable
		@Getter
		private final W inMemoryStateCopy;
		
		/**
		 * Collected updates (if available).
		 */
		@Nullable
		@Getter
		private final List<UInt> collectedUpdatesCopy;
		
		/**
		 * Constructor for when we are writing the memory state 'as is' (without
		 * reading first & applying collected updates).
		 */
		public RBWWriteData(@NonNull W inMemoryStateCopy)
		{
			this.inMemoryStateCopy = inMemoryStateCopy;
			this.collectedUpdatesCopy = null;
		}
		
		/**
		 * Constructor for when we use a copy of collected updates to perform
		 * a 'write' -- i.e. read current storage data, apply all updates,
		 * write it out (and also update in-memory state accordingly).
		 */
		public RBWWriteData(@NonNull List<UInt> collectedUpdatesCopy)
		{
			this.inMemoryStateCopy = null;
			this.collectedUpdatesCopy = collectedUpdatesCopy;
		}
		
		/**
		 * Constructor for possible special usages, normally we do not specify
		 * both of the values together.
		 */
		protected RBWWriteData(@Nullable W inMemoryStateCopy, @Nullable List<UInt> collectedUpdatesCopy)
		{
			if ((inMemoryStateCopy == null) && (collectedUpdatesCopy == null))
				throw new IllegalArgumentException("Both arguments may not be null at the same time.");
			
			this.inMemoryStateCopy = inMemoryStateCopy;
			this.collectedUpdatesCopy = collectedUpdatesCopy;
		}
	}
	
	
	/**
	 * Similar to {@link WriteSplit} except in this case 'data to be written'
	 * is represented by the copy of the collected updates. 
	 */
	@RequiredArgsConstructor
	@ToString
	protected class RBWUpdatesSplit
	{
		/**
		 * New cache data to be stored instead of the old one (may be the same
		 * instance as the old one).
		 */
		@Getter
		@NonNull // lombok checking
		private final S newCacheData;
		
		/**
		 * Copy of collected updates to be used for writing.
		 */
		@Getter
		@NonNull // lombok checking
		private final List<UInt> collectedUpdatesCopyForWrite;
	}
	
	/**
	 * This is used to split value in two cases:
	 * <p>
	 * - When read-before-write is successful and updates are applied, it is 
	 * 	necessary to split value into two -- one for actually writing to storage
	 * 	(W) and for resyncing cached value (S, which will be converted to R)
	 * <p>
	 * - If updates tracking is not available for w/e reason and 
	 * 	{@link WBRBReadBeforeWriteCache#isAllowWritesWithoutPriorResync(Object, Object, NullableOptional, io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * 	return true, then split is performed in order to separate full in-memory value
	 * 	to be written and the copy staying in the cache
	 */
	@RequiredArgsConstructor
	@ToString
	protected class RBWWriteSplit
	{
		/**
		 * New cache data to be stored instead of the old one (may be the same
		 * instance as the old one).
		 */
		@Getter
		@NonNull // for Lombok Non-Null checking
		private final S newCacheData;
		
		/**
		 * Data to send for writing.
		 */
		@Getter
		@NonNull // for Lombok Non-Null checking
		private final W writeData;
	}
	

	/**
	 * Constructor
	 *
	 * @param allowWritesWithoutPriorResync controls what happens if there are
	 * 		writes, but collected updates are not available (e.g. due to exceeding
	 * 		the maximum size or something); in this case we expect some kind of
	 * 		data loss; true means that in-memory data is written to the storage
	 * 		(without prior read-update cycle), thus potentially overwriting any
	 * 		changes made by other clients; false means that in-memory data is
	 * 		discarded instead, thus losing any in-memory updates that haven't
	 * 		been written to the storage
	 */
	public WBRBReadBeforeWriteCache(WBRBConfig config, boolean allowWritesWithoutPriorResync)
		throws IllegalArgumentException,
		IllegalStateException,
		MissingResourceException,
		NumberFormatException
	{
		super(config);
		
		this.allowWritesWithoutPriorResync = allowWritesWithoutPriorResync;
	}


	@Override
	protected WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>.WriteSplit 
		spiWriteLockSplitForWrite(
			@Nonnull K key, S cacheData,
			NullableOptional<@Nonnull RBWWriteData<W, UInt>> previousFailedWriteData,
			WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>.WBRBCacheEntry cacheEntry,
			WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>.WBRBCachePayload payload)
				throws InterruptedException
	{
		if (previousFailedWriteData.isPresent())
		{
			logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), "code should not be reachable (this cache doesn't support merging failed writes)");
			throw new CacheInternalException("code should not be reachable (this cache doesn't support merging failed writes)");
		}
		
		// If we reach this point, we know that there's data to be written.
		RBWUpdatesSplit updatesSplit = spiWriteLockAttemptUpdatesSplit(key, cacheData, previousFailedWriteData, cacheEntry, payload);
		if (updatesSplit != null)
		{
			// Collected updates will be reset in the calling method when appropriate.
			WriteSplit result = new WriteSplit(updatesSplit.getNewCacheData(), 
				new RBWWriteData<>(spiWriteLockMakeCollectedUpdatesCopy(updatesSplit.getCollectedUpdatesCopyForWrite(), key, cacheData, previousFailedWriteData, cacheEntry, payload)));
			return TypeUtil.coerce(result); // something isn't right with Eclipse compiler here, so have to coerce
			
		}
		
		// If we are here, we don't have tracked updates, but still have data to write
		// Need to make a choice whether to lose in-memory updates or potentially
		// overwrite data in storage that was added/updated concurrently
		
		// Maybe ought to log something about these, but there's already logging
		// for WBRBCacheMessage.TOO_MANY_CACHE_ELEMENT_UPDATES and there are no
		// other apparent methods to get this at the moment.
		
		if (isAllowWritesWithoutPriorResync(key, cacheData, previousFailedWriteData, cacheEntry, payload))
		{
			// We are going to overwrite storage data with in-memory updates potentially losing data
			RBWWriteSplit writeSplitRBW = spiWriteLockSplitForWriteRBW(key, cacheData, previousFailedWriteData, cacheEntry, payload);
			WriteSplit result = new WriteSplit(writeSplitRBW.getNewCacheData(), new RBWWriteData<>(writeSplitRBW.getWriteData()));
			return TypeUtil.coerce(result); // something isn't right with Eclipse compiler here, so have to coerce
		}
		
		// We cannot process this write due to lost updates information.
		// Throwing exception is not the most graceful way to report this, but
		// it will achieve the desired result (remove item from cache).
		throw new CacheElementFailedWriteException(commonNamingPrefix, key, "updates information is unavailable");
	}

	/**
	 * Attempts to split cache entry into (a copy of) collected updates for 
	 * writing and new value to be kept in cache.
	 * 
	 * @return null if not possible (e.g. if collected updates are not available) 
	 */
	@Nullable
	protected RBWUpdatesSplit spiWriteLockAttemptUpdatesSplit(
		@Nonnull K key, S cacheData,
		NullableOptional<@Nonnull RBWWriteData<W, UInt>> previousFailedWriteData,
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>.WBRBCacheEntry cacheEntry,
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>.WBRBCachePayload payload)
			throws InterruptedException
	{
		List<UInt> collectedUpdates = spiWriteLockUpdates_getCollectedUpdates(key, cacheEntry, payload);
		
		if (collectedUpdates == null)
			return null;
		
		return new RBWUpdatesSplit(
			createNewCacheEntryAfterUpdatesWriteSplit(cacheData), 
			spiWriteLockMakeCollectedUpdatesCopy(collectedUpdates, key, cacheData, previousFailedWriteData, cacheEntry, payload));
	}
	
	/**
	 * New cache data to be stored instead of the old one (may be the same
	 * instance as the old one) -- this is executed when collected updates are
	 * split for writing.
	 * <p>
	 * Gives implementations a chance to do some potential data clean-up if needed. 
	 */
	protected abstract S createNewCacheEntryAfterUpdatesWriteSplit(S cacheData);
	
	/**
	 * A thin wrapper for {@link #splitForWriteRBW(Object, Object)}
	 * <p>
	 * This can be overridden by implementations if they need more access to data
	 * than {@link #splitForWriteRBW(Object, Object)} method provides.
	 */
	@SuppressWarnings("unused")
	protected RBWWriteSplit spiWriteLockSplitForWriteRBW(@Nonnull K key, S cacheData,
		NullableOptional<RBWWriteData<W, UInt>> previousFailedWriteData,
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, RBWWriteData<W, UInt>, UExt, UInt>.WBRBCacheEntry cacheEntry,
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, RBWWriteData<W, UInt>, UExt, UInt>.WBRBCachePayload payload)
			throws InterruptedException
	{
		return splitForWriteRBW(key, cacheData);
	}

	/**
	 * A version of {@link #splitForWrite(Object, Object, NullableOptional)}
	 * that is relevant for this cache implementation (default one is not used).
	 * <p>
	 * This is used to split value in two cases:
	 * <p>
	 * - When read-before-write is successful and updates are applied, it is 
	 * 	necessary to split value into two -- one for actually writing to storage
	 * 	(W) and for resyncing cached value (S, which will be converted to R)
	 * <p>
	 * - If updates tracking is not available for w/e reason and 
	 * 	{@link WBRBReadBeforeWriteCache#isAllowWritesWithoutPriorResync(Object, Object, NullableOptional, io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 * 	return true, then split is performed in order to separate full in-memory value
	 * 	to be written and the copy staying in the cache
	 */
	protected abstract RBWWriteSplit splitForWriteRBW(@Nonnull K key, S cacheData);

	/**
	 * Default {@link #splitForWrite(Object, Object, NullableOptional)} is not
	 * used in this cache because it is intercepted in {@link #spiWriteLockSplitForWrite(Object, Object, NullableOptional, io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 */
	@Override
	protected WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>.WriteSplit splitForWrite(
		@Nonnull K key, S cacheData,
		NullableOptional<@Nonnull RBWWriteData<W, UInt>> previousFailedWriteData)
	{
		{
			String emsg = "code should not be reachable (this cache doesn't support default splitForWrite method)";
			logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), emsg);
			throw new CacheInternalException(emsg);
		}
	}
	
	
	/**
	 * {@link WBRBReadBeforeWriteCache} implementation of 
	 * {@link WriteBehindResyncInBackgroundCache#spiNoLockWriteToStorage(Object, io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBWriteQueueEntry)} 
	 * that actually carries out read-before-write resync logic.
	 */
	@Override
	protected void spiNoLockWriteToStorage(@Nonnull K key,
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>.WBRBWriteQueueEntry writeEntry)
		throws InterruptedException
	{
		RBWWriteData<W, UInt> dataToWrite = writeEntry.getDataToWrite();
		List<UInt> collectedUpdates = dataToWrite.getCollectedUpdatesCopy();
		
		if (collectedUpdates == null)
		{
			// In this case we just need to perform a full write directly.
			spiNoLockWriteToStorageRBW(key, nnChecked(dataToWrite.getInMemoryStateCopy()), false, writeEntry);
			return;
		}
		
		// Do the read-before-write logic.
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>
			.WBRBCacheEntry cacheEntry = writeEntry.getCacheEntry();
		
		// read
		R storageDataBeforeUpdates = spiNoLockReadFromStorageRBW(key, true, writeEntry, cacheEntry); 
		
		// apply updates
		S cacheDataWithUpdates = convertToCacheFormatFromStorageData(key, storageDataBeforeUpdates);
		for (UInt update : collectedUpdates)
			cacheDataWithUpdates = applyUpdate(cacheDataWithUpdates, update);
		
		// split
		RBWWriteSplit splitForWrite = splitForWriteRBW(key, cacheDataWithUpdates);
		
		// Reverse-conversion isn't great, but this is mostly supposed to be used
		// with typeof(R) == typeof(S)
		R storageDataWithUpdates = convertFromCacheFormatToStorageData(key, splitForWrite.getNewCacheData());
		
		// Do resync for in-memory data using current up-to-date. 
		// We can do it before write since we are not resetting collected updates 
		// on resync anyway (so this just brings in-memory data closer to 'reality'
		// regardless of whether write will then fail or not).
		apiStorageReadSuccess(storageDataWithUpdates, cacheEntry);
		
		// And finally do an actual write.
		spiNoLockWriteToStorageRBW(key, splitForWrite.getWriteData(), true, writeEntry);
	}

	
	/**
	 * Override default implementation to channel everything through
	 * {@link #spiNoLockReadFromStorageRBW(Object, boolean, io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBWriteQueueEntry, WBRBCacheEntry)}
	 * for more flexibility in implementations.
	 */
	@Override
	protected R spiNoLockReadFromStorage(@Nonnull K key, boolean isRefreshRead,
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>.WBRBCacheEntry cacheEntry)
		throws InterruptedException
	{
		return spiNoLockReadFromStorageRBW(key, isRefreshRead, null, cacheEntry);
	}


	/**
	 * A version of {@link #readFromStorage(Object, boolean)} that provides more
	 * access to the internal data (so the implementations may override this
	 * to get more access). 
	 * <p>
	 * Default implementation simply calls {@link #readFromStorage(Object, boolean)}
	 * and returns the result.
	 * 
	 * @param writeEntry if not null, then this is the read being executed before
	 * 		write and it provides access to the causing write entry; null means
	 * 		this is a standard read, unrelated to write
	 */
	protected R spiNoLockReadFromStorageRBW(K key, boolean isRefreshRead,
		@SuppressWarnings("unused") WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>
			.@Nullable WBRBWriteQueueEntry writeEntry,
		@SuppressWarnings("unused") WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>.WBRBCacheEntry 
			cacheEntry) 
		throws InterruptedException
	{
		return readFromStorage(key, isRefreshRead);
	}
	

	@Override
	protected void writeToStorage(@Nonnull K key,
		RBWWriteData<W, UInt> dataToWrite)
		throws InterruptedException
	{
		{
			String emsg = "code should not be reachable (this cache doesn't support default writeToStorage method)";
			logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), emsg);
			throw new CacheInternalException(emsg);
		}
	}

	/**
	 * The version of {@link #writeToStorage(Object, RBWWriteData)} used by this
	 * cache implementation as the default one is overriddent to perform
	 * read-before-write.
	 */
	protected abstract void writeToStorageRBW(@Nonnull K key, W dataToWrite) throws InterruptedException;

	/**
	 * A version of {@link #writeToStorageRBW(Object, Object)} that provides
	 * more access to the internal data (so it can be overridden by implementations
	 * if needed).
	 * <p>
	 * Default implementation simply calls {@link #writeToStorageRBW(Object, Object)}
	 * 
	 * @param resyncCompleted true if read-resync-before-write was completed,
	 * 		false otherwise (if just dumping in-memory data); see {@link #isAllowWritesWithoutPriorResync(Object, Object, NullableOptional, io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheEntry, io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCachePayload)}
	 */
	@SuppressWarnings("unused")
	protected void spiNoLockWriteToStorageRBW(@Nonnull K key, W dataToWrite,
		boolean resyncCompleted,
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>.WBRBWriteQueueEntry writeEntry
		)
		throws InterruptedException
	{
		writeToStorageRBW(key, dataToWrite);
	}
	
	/**
	 * Converts data from the storage format used by cache internally back into
	 * format that is used when reading data from the storage.
	 * <p>
	 * This is necessary because 'read success' methods in parent cache accept
	 * 'read from storage' format only.
	 * <p>
	 * Typically it is expected that typeof(R) == typeof(S) in implementations
	 * of this cache and therefore the method should just return the given 
	 * argument.
	 */
	protected abstract R convertFromCacheFormatToStorageData(K key, S cacheData);


	/**
	 * This is called if write is to be done and updates are not tracked for w/e
	 * reason.
	 * <p>
	 * Must decide whether to allow write without prior resync for this specific case.
	 * <p>
	 * Default implementation simply returns {@link #allowWritesWithoutPriorResync}
	 * that is given in the constructor.
	 * 
	 * @see #allowWritesWithoutPriorResync for more details
	 */
	@SuppressWarnings("unused")
	protected boolean isAllowWritesWithoutPriorResync(
		@Nonnull K key, S cacheData,
		NullableOptional<RBWWriteData<W, UInt>> previousFailedWriteData,
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, RBWWriteData<W, UInt>, UExt, UInt>.WBRBCacheEntry cacheEntry,
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, RBWWriteData<W, UInt>, UExt, UInt>.WBRBCachePayload payload)
	{
		return allowWritesWithoutPriorResync;
	}
	
	/**
	 * Gives implementations chance to override how updates copy is created
	 * (e.g. for when updates are sent to write queue).
	 * <p>
	 * Default implementation simply creates a shallow copy.
	 */
	@SuppressWarnings("unused")
	protected List<UInt> spiWriteLockMakeCollectedUpdatesCopy(
		List<UInt> collectedUpdates,
		@Nonnull K key, S cacheData,
		NullableOptional<RBWWriteData<W, UInt>> previousFailedWriteData,
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, RBWWriteData<W, UInt>, UExt, UInt>.WBRBCacheEntry cacheEntry,
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, RBWWriteData<W, UInt>, UExt, UInt>.WBRBCachePayload payload)
			throws InterruptedException
	{
		return new ArrayList<>(collectedUpdates);
	}


	/**
	 * This cache doesn't support merging writes (mainly because it's unclear
	 * how to merge full-write (in case those are allowed) with updates-write).
	 * 
	 * @return false
	 */
	@Override
	protected boolean spiWriteLockIsCanMergeWrites(@Nonnull K key, S cacheData,
		NullableOptional<@Nonnull RBWWriteData<W, UInt>> previousFailedWriteData,
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>.WBRBCacheEntry cacheEntry,
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>.WBRBCachePayload payload)
		throws InterruptedException
	{
		return false; 
	}


	/**
	 * This cache is based on working with out-of-order reads.
	 * 
	 * @return true
	 */
	@Override
	protected boolean spiWriteLockIsAcceptOutOfOrderRead(@Nonnull K key,
		R storageData,
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>.WBRBCacheEntry cacheEntry,
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>.WBRBCachePayload payload)
		throws InterruptedException
	{
		return true;
	}


	@Override
	protected void spiWriteLockUpdates_reset(WBRBUpdatesResetReason reason,
		boolean collectUpdatesAfter, @Nonnull K key,
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>.WBRBCacheEntry cacheEntry,
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>.WBRBCachePayload payload)
		throws InterruptedException
	{
		boolean collectAfter = true;
		switch(reason)
		{
			case RETURN_QUEUE_DECISION:			// main usecase -- keep collecting updates after return queue
			case STORAGE_DATA_MERGED:			// main usecase -- keep collecting updates after resync
			case READ_FAILED_FINAL_DECISION: 	// relevant for resync reads, keep collecting updates as long as possible in case something succeeds later
				return; // Block resetting collected updates
			case NO_WRITE_LOCK_NEW_CACHE_ENTRY_CREATED: // for new instances we need to immediatelly start collecting updates
			case FULL_WRITE_SENT: // previous updates were sent to write, reset them
				collectAfter = true;
				break;
			case IS_MERGE_POSSIBLE_EXCEPTION:
			case UPDATE_COLLECT_EXCEPTION:
			case REMOVED_FROM_CACHE:
				collectAfter = false;
				break;
		}
		
		super.spiWriteLockUpdates_reset(reason, collectAfter, key, cacheEntry,
			payload);
	}


	@Override
	protected WBRBReturnQueueItemProcessingDecision spiWriteLockMakeReturnQueueProcessingDecision(
		@Nonnull K key,
		boolean itemHadAccessSinceMainQueue, long itemUntouchedMs, 
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>.WBRBCacheEntry cacheEntry,
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>.WBRBCachePayload payload)
		throws InterruptedException
	{
		WBRBReturnQueueItemProcessingDecision decision = super.spiWriteLockMakeReturnQueueProcessingDecision(
			key, itemHadAccessSinceMainQueue, itemUntouchedMs, cacheEntry, payload);
		
		switch (decision.getAction())
		{
			case MAIN_QUEUE_PLUS_RESYNC:
				// need to figure out if we need to do resync
				long lastSync = payload.getLastSyncedWithStorageTimestamp();
				long timeSinceLastSyncVirtualMs = timeGapVirtual(lastSync, timeNow());
				
				if (spiWriteLockMakeReturnQueueProcessingDecision_DecideOnResync(timeSinceLastSyncVirtualMs, key, cacheEntry, payload))
					return decision; // return original decision if we are allowed to do resync
				
				return new WBRBReturnQueueItemProcessingDecision(
					WBRBReturnQueueItemProcessingDecisionAction.MAIN_QUEUE_NO_RESYNC, // prevent resync 
					decision.isStopCollectingUpdates());
			case DO_NOTHING:
			case EXPIRE_FROM_CACHE:
			case MAIN_QUEUE_NO_RESYNC:
			case REMOVE_FROM_CACHE:
			case RETURN_QUEUE:
				return decision; // return decision as-is
		}
		
		{
			String emsg = "code should not be reachable";
			logMessage(WBRBCacheMessage.ASSERTION_FAILED, new Exception("stack trace"), emsg);
			throw new CacheInternalException(emsg);
		}
	}
	
	/**
	 * When underlying code in original {@link WriteBehindResyncInBackgroundCache}
	 * decides that return queue item should be sent for resync, this code
	 * decides whether to actully DO the resync -- because normally resync is
	 * done automatically before write.
	 * <p>
	 * But if there was no write, for example, or if it was full write (as opposed
	 * to read-update-write), then resync might not have been carried out.
	 * <p>
	 * Default implementation allows resync if virtual time gap since last resync
	 * is more than 2x return queue time.
	 * 
	 * @return true if resync may be scheduled; false otherwise
	 */
	@SuppressWarnings("unused")
	protected boolean spiWriteLockMakeReturnQueueProcessingDecision_DecideOnResync(
		long timeSinceLastSyncVirtualMs,
		@Nonnull K key,
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>.WBRBCacheEntry cacheEntry,
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>.WBRBCachePayload payload)
			throws InterruptedException
	{
		return timeSinceLastSyncVirtualMs > (config.getReturnQueueCacheTimeMin() * 2);
	}


	@Override
	protected void spiWriteLockMakeReturnQueueProcessingDecision_logNonStandardOutcome(
		boolean doLog, WBRBReturnQueueItemProcessingDecision decisionOutcome,
		@Nonnull K key,
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>.WBRBCacheEntry cacheEntry,
		WriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, @Nonnull RBWWriteData<W, UInt>, UExt, UInt>.WBRBCachePayload payload)
		throws InterruptedException
	{
		boolean actualDoLog = doLog;
		switch (decisionOutcome.getAction())
		{
			case MAIN_QUEUE_NO_RESYNC:
				actualDoLog = false; // For this cache this is normal operation mode, so no need to log this.
				break;
			case DO_NOTHING:
			case EXPIRE_FROM_CACHE:
			case MAIN_QUEUE_PLUS_RESYNC:
			case REMOVE_FROM_CACHE:
			case RETURN_QUEUE:
				break;
		}
		
		super.spiWriteLockMakeReturnQueueProcessingDecision_logNonStandardOutcome(actualDoLog,
			decisionOutcome, key, cacheEntry, payload);
	}
}
