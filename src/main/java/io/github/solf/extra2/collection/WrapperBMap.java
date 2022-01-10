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
package io.github.solf.extra2.collection;

import static io.github.solf.extra2.util.NullUtil.nn;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.github.solf.extra2.util.TypeUtil;
import lombok.experimental.Delegate;

/**
 * Thin wrapper implementation that allows representing any java maps as {@link SerializableBMap}
 * (although whether it is actually serializable depends on underlying map
 * serializability).
 * <p>
 * All operations on the wrapper are pass-through to the underlying map instance.
 * <p>
 * The performance impact of using this is very similar to using {@link Collections#unmodifiableMap(Map)} --
 * specifically any entry iteration over the map results in a creation of 
 * intermediate wrapper object ({@link ReadOnlyEntry}) for each entry iterated.
 * <p>
 * This wrapper is used in e.g.: {@link BMap#of(Map)} and {@link ReadOnlyMap#of(Map)}
 *
 * @author Sergey Olefir
 */
public class WrapperBMap<K, V> implements SerializableBMap<K, V>
{
	/** UID for serialization */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Underlying map.
	 */
	@Nonnull
	@Delegate
	private final Map<K, V> umap;
	
	/**
	 * Constructor.
	 */
	public WrapperBMap(@Nonnull Map<K, V> srcMap)
	{
		this.umap = srcMap; // wrapping into Collections.unmodifiableMap(srcMap) has unnecessary iteration costs
	}
	
	/**
	 * Returns iterator over all entries in this map.
	 * 
     * @deprecated should not be used directly as per interface contract; method
     * 		is only present for use with 'for (var item : collection)' constructs;
     * 		use {@link #enumeration()} if you need some kind of iterator
	 */
	@Deprecated
	@Override
	public @Nonnull Iterator<@Nonnull ReadOnlyEntry<K, V>> iterator()
	{
		return WACollections.remapIterator(umap.entrySet().iterator(), e -> ReadOnlyEntry.of(e));
	}

	@Override
	public V getOrFallback(K key, V defaultValue)
	{
		return umap.getOrDefault(key, defaultValue);
	}

	@Override
	public @Nonnull ForIterable<V> vals()
	{
		return ForIterable.of(umap.values());
	}

	@Override
	public @Nonnull ForIterable<K> keys()
	{
		return ForIterable.of(umap.keySet());
	}

	@Override
	public @Nonnull ForIterable<@Nonnull ReadOnlyEntry<K, V>> entries()
	{
		return this;
	}

	@Override
	public @Nullable V getValue(K key)
	{
		return umap.get(key);
	}

	@Override
	public boolean hasKey(K key)
	{
		return umap.containsKey(key);
	}

	/**
	 * Cached unmodifiable map if was created previously.
	 */
	transient private volatile Map<K, V> cachedUnmodifiableMap = null;
	@Override
	public @Nonnull Map<K, V> toUnmodifiableJavaMap()
	{
		Map<K, V> result = cachedUnmodifiableMap;
		if (result == null)
		{
			result = Collections.unmodifiableMap(umap);
			cachedUnmodifiableMap = result;
		}
		
		return result;
	}

	@Override
	public V getOrCreateValue(K key,
		@Nonnull Function<? super K, ? extends V> producer)
	{
		if (umap.containsKey(key))
			return nn(umap.get(key)); // it ain't actually non-null, but it is of type V, not @Nullable V
		
		V value = producer.apply(key);
		umap.put(key, value);
		
		return value;
	}

	@Override
	public V getOrCreateValueNonNull(K key,
		@Nonnull Function<? super K, @Nonnull ? extends V> producer)
		throws NullPointerException
	{
		// Will return null if there's no mapping (or mapping's value is null)
		// AND producer fails to produce non-null value.
		@Nullable V result = umap.computeIfAbsent(key, TypeUtil.coerce(producer));
		
		if (result == null)
		{
			umap.remove(key);
			throw new NullPointerException("Unexpected null value from producer for key: " + key);
		}
		
		return result;
	}
	
	
	@Override
	public @Nonnull Iterator<@Nonnull Entry<K, V>> liveEntries()
	{
		return umap.entrySet().iterator();
	}

	@Override
	public @Nonnull Iterator<K> liveKeys()
	{
		return umap.keySet().iterator();
	}

	@Override
	public @Nonnull Iterator<V> liveVals()
	{
		return umap.values().iterator();
	}

	@Override
	public @Nullable V removeAndGet(K key)
	{
		return umap.remove(key);
	}

	@Override
	public boolean removeIfValue(K key, V value)
	{
		return umap.remove(key, value);
	}

	@Override
	public @Nullable V putIfNoValue(K key, V value)
	{
		return umap.putIfAbsent(key, value);
	}

	@Override
	public @Nullable V putIfNoKey(K key, V value)
	{
		if (umap.containsKey(key))
			return umap.get(key);
		
		umap.put(key, value);
		return null;
	}

	@Override
	public int hashCode()
	{
		return umap.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object obj)
	{
		return umap.equals(obj);
	}

	@Override
	public @Nonnull String toString()
	{
		return umap.toString();
	}
}
