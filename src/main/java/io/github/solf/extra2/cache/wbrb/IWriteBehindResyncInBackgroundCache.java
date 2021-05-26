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

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import io.github.solf.extra2.cache.exception.CacheControlStateException;
import io.github.solf.extra2.cache.exception.CacheElementNotYetLoadedException;
import io.github.solf.extra2.cache.exception.CacheFullException;
import io.github.solf.extra2.cache.exception.CacheIllegalExternalStateException;
import io.github.solf.extra2.cache.exception.CacheIllegalStateException;
import io.github.solf.extra2.cache.exception.CacheInternalException;
import io.github.solf.extra2.concurrent.exception.WAInterruptedException;
import io.github.solf.extra2.nullable.NullableOptional;

/**
 * Interface for {@link WriteBehindResyncInBackgroundCache} that doesn't contain
 * control methods, see {@link IWriteBehindResyncInBackgroundCacheWithControlMethods}
 * for those.
 * <p>
 * Can be helpful to completely hide implementation stuff from the clients.
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
 * 
 * @author Sergey Olefir
 */
@SuppressWarnings("unused") // for type parameters
@ParametersAreNonnullByDefault
public interface IWriteBehindResyncInBackgroundCache<@Nonnull K, V, S, R, W, UExt, UInt>
{
	
	/**
	 * Attempts to pre-load cache value if it is not already in the cache.
	 * <p>
	 * This is useful in case client code 'knows' that it'll need cache value
	 * before it actually needs it -- this way data loading may start earlier
	 * and be available sooner when it is actually needed.
	 * <p>
	 * The method is a 'best effort' attempt, it does not provide 100% guarantee
	 * that value will be loaded.
	 * <p>
	 * This method returns as fast as possible, doesn't wait for any background
	 * processing.
	 * 
	 * @throws IllegalArgumentException if key is null
	 * @throws CacheFullException if element is not currently cached and cache
	 * 		is full (so no additional element may be added)
	 * @throws CacheIllegalStateException if there's problem with cache state,
	 * 		such as shutdown or element failed loading
	 * @throws WAInterruptedException if thread was interrupted
	 */
	public void preloadCache(K key) throws IllegalArgumentException, CacheFullException,
		CacheIllegalStateException, CacheInternalException, WAInterruptedException;

	
	/**
	 * Reads cache element if it can be read within specified time limit
	 * 
	 * @param limitMillis positive number specifies maximum wait time in milliseconds
	 * 		[this is affected by {@link #timeFactor()}]; method invocation may
	 * 		take longer as time limit only has impact on how long various sleep/
	 * 		wait methods may take; negative value specifies no waiting (same
	 * 		as using {@link #readIfCached(Object)}); zero has special meaning --
	 * 		there's no waiting, but if element is not present in cache, it will
	 * 		be scheduled for loading
	 * 
	 * @return {@link NullableOptional} -- empty if element is not yet loaded,
	 * 		with value if value was read; if it is empty, may contain exception
	 * 		indicating some additional reason as to why element not yet loaded,
	 * 		e.g. CacheInternalException "Too many attempts ... encountered REMOVED_FROM_CACHE"
	 * 		or CacheElementFailedLoadingException (in case underlying load failed)
	 * 
	 * @throws IllegalArgumentException if key is null
	 * @throws CacheFullException if element is not currently cached and cache
	 * 		is full (so no additional element may be added)
	 * @throws CacheIllegalStateException if there's problem with cache state,
	 * 		such as shutdown or element failed loading
	 * @throws WAInterruptedException if thread was interrupted
	 */
	public NullableOptional<V> readFor(K key, long limitMillis) 
		throws IllegalArgumentException, CacheFullException, CacheIllegalStateException, CacheInternalException, WAInterruptedException;
	
	/**
	 * Reads cache element if it can be read within specified time limit or throws
	 * an {@link CacheElementNotYetLoadedException} if it is not present / not yet loaded.
	 * 
	 * @param limitMillis positive number specifies maximum wait time in milliseconds
	 * 		[this is affected by {@link #timeFactor()}]; method invocation may
	 * 		take longer as time limit only has impact on how long various sleep/
	 * 		wait methods may take; negative value specifies no waiting (same
	 * 		as using {@link #readIfCached(Object)}); zero has special meaning --
	 * 		there's no waiting, but if element is not present in cache, it will
	 * 		be scheduled for loading
	 * 
	 * @return {@link NullableOptional} -- empty if element is not yet loaded,
	 * 		with value if value was read; if it is empty, may contain exception
	 * 		indicating some additional reason as to why element not yet loaded,
	 * 		e.g. "Too many attempts ... encountered REMOVED_FROM_CACHE"
	 * 
	 * @throws CacheElementNotYetLoadedException if element is not present /
	 * 		not yet loaded
	 * @throws IllegalArgumentException if key is null
	 * @throws CacheFullException if element is not currently cached and cache
	 * 		is full (so no additional element may be added)
	 * @throws CacheIllegalStateException if there's problem with cache state,
	 * 		such as shutdown or element failed loading
	 * @throws WAInterruptedException if thread was interrupted
	 */
	public V readForOrException(K key, long limitMillis) 
		throws CacheElementNotYetLoadedException, CacheIllegalExternalStateException, IllegalArgumentException, CacheFullException, CacheIllegalStateException, CacheInternalException, WAInterruptedException;
	
	
	/**
	 * Reads cache element if it is already cached.
	 * <p>
	 * This will NOT schedule element for reading from the underlying storage.
	 * <p>
	 * It WILL update 'last read' timestamp ('touch') the entry if it is present.
	 * 
	 * @return {@link NullableOptional} -- empty if element is not yet loaded,
	 * 		with value if value was read; if it is empty, may contain exception
	 * 		indicating some additional reason as to why element not yet loaded,
	 * 		e.g. CacheInternalException "Too many attempts ... encountered REMOVED_FROM_CACHE"
	 * 		or CacheElementFailedLoadingException (in case underlying load failed)
	 * 
	 * @throws IllegalArgumentException if key is null
	 * @throws CacheIllegalStateException if there's problem with cache state,
	 * 		such as shutdown or element failed loading
	 * @throws WAInterruptedException if thread was interrupted
	 */
	public NullableOptional<V> readIfCached(K key) 
		throws IllegalArgumentException, CacheIllegalStateException, CacheInternalException, WAInterruptedException;
	
	/**
	 * Reads cache element if it is already cached or throws
	 * an {@link CacheElementNotYetLoadedException} if it is not present / not yet loaded.
	 * <p>
	 * This will NOT schedule element for reading from the underlying storage.
	 * <p>
	 * It WILL update 'last read' timestamp ('touch') the entry if it is present.
	 * 
	 * @return cache value
	 * 
	 * @throws CacheElementNotYetLoadedException if element is not present /
	 * 		not yet loaded
	 * @throws IllegalArgumentException if key is null
	 * @throws CacheIllegalStateException if there's problem with cache state,
	 * 		such as shutdown or element failed loading
	 * @throws WAInterruptedException if thread was interrupted
	 */
	public V readIfCachedOrException(K key) 
		throws CacheElementNotYetLoadedException, CacheIllegalExternalStateException, IllegalArgumentException, CacheIllegalStateException, CacheInternalException, WAInterruptedException;
	
	
	/**
	 * Reads cache element if it can be read until given timestamp
	 * 
	 * @param limitTimestamp value 'after now' specifies maximum wait time 
	 * 		[this is affected by {@link #timeFactor()} -- with non-default factor
	 * 		method may return sooner or later than the target time]; method invocation may
	 * 		take longer as time limit only has impact on how long various sleep/
	 * 		wait methods may take; values of 'now' or 'before now' act the same
	 * 		as {@link #readIfCached(Object)}
	 * 
	 * @return {@link NullableOptional} -- empty if element is not yet loaded,
	 * 		with value if value was read; if it is empty, may contain exception
	 * 		indicating some additional reason as to why element not yet loaded,
	 * 		e.g. "Too many attempts ... encountered REMOVED_FROM_CACHE"
	 * 
	 * @throws IllegalArgumentException if key is null
	 * @throws CacheFullException if element is not currently cached and cache
	 * 		is full (so no additional element may be added)
	 * @throws CacheIllegalStateException if there's problem with cache state,
	 * 		such as shutdown or element failed loading
	 * @throws WAInterruptedException if thread was interrupted
	 */
	public NullableOptional<V> readUntil(K key, long limitTimestamp) 
		throws IllegalArgumentException, CacheFullException, CacheIllegalStateException, CacheInternalException, WAInterruptedException; 
	
	/**
	 * Reads cache element if it can be read until given timestamp or throws
	 * an {@link CacheElementNotYetLoadedException} if it is not present / not yet loaded.
	 * 
	 * @param limitTimestamp value 'after now' specifies maximum wait time 
	 * 		[this is affected by {@link #timeFactor()} -- with non-default factor
	 * 		method may return sooner or later than the target time]; method invocation may
	 * 		take longer as time limit only has impact on how long various sleep/
	 * 		wait methods may take; values of 'now' or 'before now' act the same
	 * 		as {@link #readIfCached(Object)}
	 * 
	 * @return {@link NullableOptional} -- empty if element is not yet loaded,
	 * 		with value if value was read; if it is empty, may contain exception
	 * 		indicating some additional reason as to why element not yet loaded,
	 * 		e.g. CacheInternalException "Too many attempts ... encountered REMOVED_FROM_CACHE"
	 * 		or CacheElementFailedLoadingException (in case underlying load failed)
	 * 
	 * @throws CacheElementNotYetLoadedException if element is not present /
	 * 		not yet loaded
	 * @throws IllegalArgumentException if key is null
	 * @throws CacheFullException if element is not currently cached and cache
	 * 		is full (so no additional element may be added)
	 * @throws CacheIllegalStateException if there's problem with cache state,
	 * 		such as shutdown or element failed loading
	 * @throws WAInterruptedException if thread was interrupted
	 */
	public V readUntilOrException(K key, long limitTimestamp) 
		throws CacheElementNotYetLoadedException, CacheIllegalExternalStateException, IllegalArgumentException, CacheFullException, CacheIllegalStateException, CacheInternalException, WAInterruptedException;

	/**
	 * Records given update in the cache value if item is already cached.
	 * 
	 * @return optional containing {@link Boolean#TRUE} if write was successfully
	 * 		queued; empty optional if write was not made because element is not
	 * 		cached or is otherwise in invalid state; if optional is empty,
	 * 		it may additionally contain exception with additional details as to
	 * 		why write has failed 
	 * 
	 * @throws IllegalArgumentException if key is null
	 * @throws CacheIllegalStateException if there's problem with cache state,
	 * 		such as shutdown or element failed loading
	 * @throws WAInterruptedException if thread was interrupted
	 */
	public NullableOptional<@Nonnull Boolean> writeIfCached(K key, UExt update) 
		throws IllegalArgumentException, CacheIllegalStateException, CacheInternalException, WAInterruptedException;
	
	/**
	 * Records given update in the cache value if item is already cached or throws
	 * an {@link CacheElementNotYetLoadedException} if it is not present / not yet loaded.
	 * 
	 * @return false if write was not made because element was not cached; true
	 * 		if write was queued
	 * 
	 * @throws CacheElementNotYetLoadedException if element is not present /
	 * 		not yet loaded
	 * @throws IllegalArgumentException if key is null
	 * @throws CacheIllegalStateException if there's problem with cache state,
	 * 		such as shutdown or element failed loading
	 * @throws WAInterruptedException if thread was interrupted
	 */
	// CCC comment and check exceptions
	public void writeIfCachedOrException(K key, UExt update) 
		throws CacheElementNotYetLoadedException, CacheIllegalExternalStateException, IllegalArgumentException, CacheIllegalStateException, CacheInternalException, WAInterruptedException;
	

	/**
	 * Records given update in the cache value if item is already cached and then
	 * produces a read result.
	 * <p>
	 * This is very similar to calling {@link #writeIfCached(Object, Object)}
	 * followed by {@link #readIfCached(Object)} except this one is atomic and
	 * is more performant.
	 * <p>
	 * NOTE: using this method instead of {@link #writeIfCached(Object, Object)}
	 * has additional performance costs, such as invoking {@link WriteBehindResyncInBackgroundCache#convertFromCacheFormatToReturnValue(Object, Object)};
	 * only use this method if return value is going to be used.
	 * 
	 * @return optional containing return value same as from {@link #readIfCached(Object)} if write was successfully
	 * 		queued; empty optional if write was not made because element is not
	 * 		cached or is otherwise in invalid state; if optional is empty,
	 * 		it may additionally contain exception with additional details as to
	 * 		why write has failed 
	 * 
	 * @throws IllegalArgumentException if key is null
	 * @throws CacheIllegalStateException if there's problem with cache state,
	 * 		such as shutdown or element failed loading
	 * @throws WAInterruptedException if thread was interrupted
	 */
	public NullableOptional<V> writeIfCachedAndRead(K key, UExt update) 
		throws IllegalArgumentException, CacheIllegalStateException, CacheInternalException, WAInterruptedException;
	
	/**
	 * Records given update in the cache value if item is already cached and then
	 * produces a read result.
	 * <p>
	 * Throws an {@link CacheElementNotYetLoadedException} if the element is not 
	 * present / not yet loaded.
	 * <p>
	 * This is very similar to calling {@link #writeIfCachedOrException(Object, Object)}
	 * followed by {@link #readIfCachedOrException(Object)} except this one is atomic and
	 * is more performant.
	 * <p>
	 * NOTE: using this method instead of {@link #writeIfCachedOrException(Object, Object)}
	 * has additional performance costs, such as invoking {@link WriteBehindResyncInBackgroundCache#convertFromCacheFormatToReturnValue(Object, Object)};
	 * only use this method if return value is going to be used.
	 * 
	 * @return read value after update, same as {@link #readIfCached(Object)}
	 * 
	 * @throws CacheElementNotYetLoadedException if element is not present /
	 * 		not yet loaded
	 * @throws IllegalArgumentException if key is null
	 * @throws CacheIllegalStateException if there's problem with cache state,
	 * 		such as shutdown or element failed loading
	 * @throws WAInterruptedException if thread was interrupted
	 */
	// CCC comment and check exceptions
	public V writeIfCachedAndReadOrException(K key, UExt update) 
		throws CacheElementNotYetLoadedException, CacheIllegalExternalStateException, IllegalArgumentException, CacheIllegalStateException, CacheInternalException, WAInterruptedException;
}
