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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Interface for {@link WriteBehindResyncInBackgroundCache} that contains both
 * access and control methods (start, shutdown) (i.e. all public-facing methods).
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
@NonNullByDefault
public interface IWriteBehindResyncInBackgroundCacheWithControlMethods<@Nonnull K, V, S, R, W, UExt, UInt>
	extends IWriteBehindResyncInBackgroundCache<K, V, S, R, W, UExt, UInt>, IWriteBehindResyncInBackgroundCacheOnlyControlMethods<K, V, S, R, W, UExt, UInt>
{
	// empty because all methods are in parent interfaces
}
