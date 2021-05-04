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
package com.github.solf.extra2.collection;

import java.util.Map;

import javax.annotation.Nonnull;

/**
 * A version of an entry (e.g. a Map.Entry) that is read-only (doesn't allow
 * any modifications).
 *
 * @author Sergey Olefir
 */
public interface ReadOnlyEntry<K, V>
{
    /**
     * Returns the key corresponding to this entry.
     *
     * @return the key corresponding to this entry
     * @throws IllegalStateException implementations may, but are not
     *         required to, throw this exception if the underlying data bit has
     *         been removed or altered (i.e. fail-fast)
     */
    K getKey();

    /**
     * Returns the value corresponding to this entry.  If the mapping
     * has been removed from the backing map (by the iterator's
     * {@code remove} operation), the results of this call are undefined.
     *
     * @return the value corresponding to this entry
     * @throws IllegalStateException implementations may, but are not
     *         required to, throw this exception if the underlying data bit has
     *         been removed or altered (i.e. fail-fast)
     */
    V getValue();
    
    /**
     * Returns a read-only version of given Map.Entry
     */
    @Nonnull
    public static <K, V> ReadOnlyEntry<K, V> of(final Map.@Nonnull Entry<K, V> mapEntry)
    {
    	return new ReadOnlyEntry<K, V>()
		{
			@Override
			public V getValue()
			{
				return mapEntry.getValue();
			}
			
			@Override
			public K getKey()
			{
				return mapEntry.getKey();
			}
		};
    }
    
    /**
     * Returns a {@link ReadOnlyEntry} for the given key + value.
     */
    @Nonnull 
    public static <K, V> ReadOnlyEntry<K, V> of(final K key, final V value)
    {
    	return new ReadOnlyEntry<K, V>()
		{
			@Override
			public V getValue()
			{
				return value;
			}
			
			@Override
			public K getKey()
			{
				return key;
			}
		};
    }
}
