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

import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * An extension to {@link TreeMap}
 *
 * Currently provides {@link #getEntry(Object)} method.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class TreeMapExt<K, V> extends TreeMap<K, V> implements NavigableMapExt<K, V>
{
	/**
	 * Factory used for creating entries in {@link #getOrCreate(Object)}
	 */
	@Nullable
	private Function<K, @Nonnull V> defaultFactory; 
	

	/**
	 * 
	 */
	public TreeMapExt()
	{
		super();
	}
	
	/**
	 * Creates an instance and initializes factory to be used in {@link #getOrCreate(Object)} 
	 */
	public TreeMapExt(Function<K, @Nonnull V> factory)
	{
		super();
		setFactory(factory);
	}

	/**
	 * @param comparator
	 */
	public TreeMapExt(@Nullable Comparator<? super K> comparator)
	{
		super(comparator);
	}

	/**
	 * @param m
	 */
	public TreeMapExt(Map<? extends K, ? extends V> m)
	{
		super(m);
	}

	/**
	 * @param m
	 */
	public TreeMapExt(SortedMap<K, ? extends V> m)
	{
		super(m);
	}
	
	/* (non-Javadoc)
	 * @see com.github.solf.extra2.collection.ExtendedMap#getEntry(java.lang.Object)
	 */
	@Override
	@Nullable 
	public Entry<K, V> getEntry(K key)
		throws IllegalStateException
	{
		return WACollections.getEntry(this, key);
	}

	/* (non-Javadoc)
	 * @see com.github.solf.extra2.collection.MapExt#get(java.lang.Object, java.util.function.Supplier)
	 */
	@Override
	public V get(K key, @Nonnull Supplier<@Nonnull V> factory)
	{
		@Nullable V result = get(key);
		if (result == null)
		{
			result = factory.get();
			put(key, result);
		}
		
		return result;
	}


	/* (non-Javadoc)
	 * @see com.github.solf.extra2.collection.MapExt#setFactory(java.util.function.Function)
	 */
	@Override
	public MapExt<K, V> setFactory(Function<K, @Nonnull V> factory)
	{
		this.defaultFactory = factory;
		
		return this;
	}

	/* (non-Javadoc)
	 * @see com.github.solf.extra2.collection.MapExt#getOrCreate(java.lang.Object)
	 */
	@Override
	public V getOrCreate(K key)
		throws IllegalStateException
	{
		Function<K, @Nonnull V> f = defaultFactory;
		
		if (f == null)
			throw new IllegalStateException("getOrCreate called when factory is not set!");
		
		@Nullable V result = get(key);
		if (result == null)
		{
			result = f.apply(key);
			put(key, result);
		}
		
		return result;
	}
}
