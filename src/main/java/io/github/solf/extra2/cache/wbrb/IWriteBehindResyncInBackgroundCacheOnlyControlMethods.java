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
import io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache.WBRBCacheControlState;
import io.github.solf.extra2.concurrent.exception.WAInterruptedException;

/**
 * Interface for {@link WriteBehindResyncInBackgroundCache} that ONLY contains 
 * control methods (start, shutdown).
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
@ParametersAreNonnullByDefault
public interface IWriteBehindResyncInBackgroundCacheOnlyControlMethods<@Nonnull K, V, S, R, W, UExt, UInt>
{
	
	/**
	 * Actually starts the cache (all the processing threads etc).
	 * 
	 * @return 'this' for convenience: cache = new Cache(...).start();
	 */
	public <C extends IWriteBehindResyncInBackgroundCacheWithControlMethods<K, V, S, R, W, UExt, UInt>> C start() 
		throws CacheControlStateException;
	
	/**
	 * CCC
	 * 
	 * @param limitMillis positive number specifies maximum wait time in milliseconds
	 * 		[this is affected by {@link #timeFactor()}]; method invocation may
	 * 		take longer as time limit only has impact on how long various sleep/
	 * 		wait methods may take; negative value specifies no waiting
	 */
	public boolean shutdownFor(long limitMillis)
		throws CacheControlStateException, WAInterruptedException;
	
	/**
	 * CCC
	 * 
	 * @param limitTimestamp value 'after now' specifies maximum wait time 
	 * 		[this is affected by {@link #timeFactor()} -- with non-default factor
	 * 		method may return sooner or later than the target time]; method invocation may
	 * 		take longer as time limit only has impact on how long various sleep/
	 * 		wait methods may take; values of 'now' or 'before now' result in no waiting
	 */
	public boolean shutdownUntil(long limitTimestamp)
		throws CacheControlStateException, WAInterruptedException;
	
	/**
	 * Checks whether cache is alive (that is it was started and not stopped yet).
	 */
	public boolean isAlive();
	
	/**
	 * Checks whether cache is usable -- that is standard read & write operations
	 * can be performed; this can differ from {@link #isAlive()} value for various
	 * reasons such as cache flush. 
	 */
	public boolean isUsable();
	
	/**
	 * Gets cache control state.
	 */
	public WBRBCacheControlState getControlState();
	
	/**
	 * Attempts to flush cache data to the storage.
	 * <p>
	 * This only really makes sense in non-concurrent environment (e.g. during
	 * testing); during flushing normal (read/write) operations will fail.
	 * 
	 * @param limitMillis positive number specifies maximum wait time in milliseconds
	 * 		[this is affected by {@link #timeFactor()}]; method invocation may
	 * 		take longer as time limit only has impact on how long various sleep/
	 * 		wait methods may take; negative value specifies no waiting
	 * 
	 * @return true if cache managed to flush all data during the given time
	 * 		limit (i.e. there's no more in-flight data); false otherwise
	 */
	public boolean flushFor(long limitMillis)
		throws CacheControlStateException, WAInterruptedException;
	
	/**
	 * Attempts to flush cache data to the storage.
	 * <p>
	 * This only really makes sense in non-concurrent environment (e.g. during
	 * testing); during flushing normal (read/write) operations will fail.
	 * 
	 * @param limitTimestamp value 'after now' specifies maximum wait time 
	 * 		[this is affected by {@link #timeFactor()} -- with non-default factor
	 * 		method may return sooner or later than the target time]; method invocation may
	 * 		take longer as time limit only has impact on how long various sleep/
	 * 		wait methods may take; values of 'now' or 'before now' result in no waiting
	 * 
	 * @return true if cache managed to flush all data during the given time
	 * 		limit (i.e. there's no more in-flight data); false otherwise
	 */
	public boolean flushUntil(long limitTimestamp)
		throws CacheControlStateException, WAInterruptedException;
}
