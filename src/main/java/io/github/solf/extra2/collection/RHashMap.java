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

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * A {@link HashMap} implementation that 
 * recognizes the fact that key equality (according to hashCode/equals) is not
 * the same as keys being identical.
 * <p>
 * A typical example might be an Object representing a row in the database -- it
 * is fully identifiable via its primary key (and thus hashCode/equals might
 * be based solely on the primary key) but at the same time it might contain
 * different values for non-primary-key columns.
 * <p>
 * Therefore this {@link RHashMap} contains additional methods to replace keys
 * in the map ({@link #putWithNewKey(Object, Object)}), to get the actual key 
 * stored in the map ({@link #getKey(Object)}) and some other methods that 
 * are very useful but missing for standard {@link HashMap} for some reason,
 * e.g. {@link #getEntry(Object)}
 * <p>
 * It also supports {@link ForIterable} and {@link ReadOnlyMap} that provide
 * for cleaner interfaces where limited access to the {@link RHashMap} needs
 * to be provided.
 * <p>
 * NOTE: this implementation has specific overhead -- due to how {@link HashMap}
 * is implemented, if key in the map is replaced, the reference to the original
 * key is still retained as it is used as a key in the underlying {@link HashMap}.  
 * <p>
 * 
 * TODO this probably ought to be rewritten with some wrapper for keys so that they can be replaced (and not to keep reference to original key forever)
 * 
 * TODO {@link #iterator()} and maybe {@link #keys()} and similar ought to return unmodifiable ForIterable/Iterators?
 *
 * @author Sergey Olefir
 */
@ToString
public class RHashMap<K, V> extends AbstractMap<K, V> implements Cloneable, Serializable, ReadOnlyMap<K, V>
{
	/** UID for serialization */
	private static final long serialVersionUID = 1L;
	
	
	/**
	 * Backing map for this {@link RHashMap}.
	 */
	@NonNull
	@Nonnull
	private HashMap<K, @Nonnull Entry<K, V>> map;
	
	
	/**
	 * Used to store actual key-value mapping.
	 */
	@AllArgsConstructor
	@ToString
	public static class Entry<K, V> implements Serializable, Map.Entry<K, V>, ReadOnlyEntry<K, V>
	{
		/**
		 * Key.
		 */
		@Getter
		@Setter
		private K key;
		
		/**
		 * Value.
		 */
		@Getter
		private V value;

		@Override
		public V setValue(V newValue)
		{
			V old = value;
			value = newValue;
			return old;
		}
	}
	

    /**
     * Constructs an empty {@code RHashMap} with the specified initial
     * capacity and load factor.
     *
     * @param  initialCapacity the initial capacity
     * @param  loadFactor      the load factor
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive
     */
    public RHashMap(int initialCapacity, float loadFactor) {
    	map = new HashMap<>(initialCapacity, loadFactor);
    }

    /**
     * Constructs an empty {@code RHashMap} with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param  initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    public RHashMap(int initialCapacity) {
    	map = new HashMap<>(initialCapacity);
    }

    /**
     * Constructs an empty {@code RHashMap} with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public RHashMap() {
    	map = new HashMap<>();
    }

    /**
     * Constructs a new {@code RHashMap} with the same mappings as the
     * specified {@code Map}.  The {@code RHashMap} is created with
     * default load factor (0.75) and an initial capacity sufficient to
     * hold the mappings in the specified {@code Map}.
     *
     * @param   m the map whose mappings are to be placed in this map
     * @throws  NullPointerException if the specified map is null
     */
    public RHashMap(Map<? extends K, ? extends V> m) {
    	map = new HashMap<>();
    	putAll(m);
    }
	
    /**
     * Copy-constructor, second arg is unused and only exists to distinguish
     * from other Map-based constructor.
     */
    protected RHashMap(@Nonnull HashMap<K, @Nonnull Entry<K, V>> map, @SuppressWarnings("unused") @Nullable Object unused)
    {
    	this.map = map;
    }

	@Override
	public int size()
	{
		return map.size();
	}

	@Override
	public boolean isEmpty()
	{
		return map.isEmpty();
	}
	
	@Override
	public boolean hasKey(K key)
	{
		return map.containsKey(key);
	}

	/**
	 * @deprecated use {@link #hasKey(Object)} for better type-checking
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	@Override
	public boolean containsKey(Object key)
	{
		return hasKey((K)key);
	}

	/**
	 * @deprecated this is highly inefficient, consider adjusting your model
	 */
	@Deprecated
	@Override
	public boolean containsValue(Object value)
	{
		for (Entry<K, V> entry : map.values())
		{
			V v = entry.getValue();
			if ((value == v) || ((value != null) && value.equals(v)))
				return true;
		}
		
		return false;
	}

	/**
	 * @deprecated use {@link #getValue(Object)} for better type-checking
	 */
	@Deprecated
	@Override
	public @Nullable V get(Object key)
	{
		@SuppressWarnings("unchecked") K k = (K)key;
		
		return getValue(k);
	}

	@Override
	public @Nullable V getValue(K key)
	{
		@Nullable Entry<K, V> entry = getRealEntry(key);
		if (entry == null)
			return null;
		
		return entry.getValue();
	}
	
	/**
	 * If map already contains value mapped to the given key, then that value
	 * is returned; otherwise the provided producer is invoked and the resulting
	 * key-value pair is entered into the map (and the produced value is returned).
	 * 
	 * @return current value associated with the key after this method processing;
	 * 		this is either a pre-existing value or the value created by the producer
	 */
	public V getOrCreateValue(K key, @Nonnull Function<? super K, ? extends V> producer)
	{
		Entry<K, V> entry = nn(map.computeIfAbsent(key, k -> {
			V r = producer.apply(k);
			
			return new Entry<>(k, r);
		}));
		
		return entry.getValue();
	}

	@Override
	public @Nullable K getKey(K key)
	{
		@Nullable Entry<K, V> entry = getRealEntry(key);
		if (entry == null)
			return null;
		
		return entry.getKey();
	}

	@Override
	public @Nullable ReadOnlyEntry<K, V> getEntry(K key)
	{
		return getRealEntry(key);
	}

	/**
	 * Gets a live view (can change value) of the entry matching the given key 
	 * (including the actual stored key instance).
	 * <p>
	 * This acknowledges the fact that key equality via hashCode/equals doesn't
	 * mean that keys are identical, therefore the actual stored key instance
	 * is returned.
	 * 
	 * @return entry matching the key or null if there are none
	 */
	public Map.@Nullable Entry<K, V> getLiveEntry(K key)
	{
		return getRealEntry(key);
	}

	/**
	 * Internal method for retrieving actually-stored entry for the given key.
	 */
	protected @Nullable Entry<K, V> getRealEntry(K key)
	{
		return map.get(key);
	}
	
	/**
	 * Internal method for creating entry with null value.
	 */
	@SuppressWarnings("null")
	@Nonnull
	protected static <K,V> Entry<K, V> createNullValueEntry(K key)
	{
		return new Entry<>(key, null);
	}
	
	/**
	 * Internal method for creating entry with null key & value.
	 */
	@SuppressWarnings("null")
	@Nonnull
	protected static <K,V> Entry<K, V> createNullKeyValueEntry(@SuppressWarnings("unused") K key)
	{
		return new Entry<>(null, null);
	}
	
	/**
	 * 'random' object that is used e.g. for 'random' keys.
	 */
	protected static final Object RANDOM_OBJECT = new Object(); 
	/**
	 * Internal method for creating entry with 'random' NON-NULL key & null value
	 * -- this is used internally to detect certain map behaviors.
	 */
	@SuppressWarnings({"null", "unchecked"})
	@Nonnull
	protected static <K,V> Entry<K, V> createNonNullKeyNullValueEntry(@SuppressWarnings("unused") K key)
	{
		return new Entry<>((K)RANDOM_OBJECT, null);
	}
	
	/**
	 * Internal implementation of actual 'put' functionality.
	 */
	protected @Nullable V putInternal(K key, V value, boolean replaceKey)
	{
		Entry<K, V> entry = nn(map.computeIfAbsent(key, RHashMap::createNullValueEntry));
		V old = entry.getValue();
		if (replaceKey)
			entry.setKey(key);
		entry.setValue(value);
		
		return old;
	}
	
	/**
	 * @deprecated use {@link #putRetainKey(Object, Object)} for clarity 
	 */
	@Deprecated
	@Override
	public @Nullable V put(K key, V value)
	{
		return putRetainKey(key, value);
	}
	
    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value AND THE OLD KEY ARE REPLACED.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with {@code key}.)
     */
	public @Nullable V putWithNewKey(K key, V value)
	{
		return putInternal(key, value, true);
	}

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value is replaced but the OLD KEY IS RETAINED.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with {@code key}.)
     */
	public @Nullable V putRetainKey(K key, V value)
	{
		return putInternal(key, value, false);
	}
	
    /**
     * Exactly equivalent to {@link Map#remove(Object)} but with better type-checking.
     * <p>
     * Removes the mapping for a key from this map if it is present
     * (optional operation).   More formally, if this map contains a mapping
     * from key {@code k} to value {@code v} such that
     * {@code Objects.equals(key, k)}, that mapping
     * is removed.  (The map can contain at most one such mapping.)
     *
     * <p>Returns the value to which this map previously associated the key,
     * or {@code null} if the map contained no mapping for the key.
     *
     * <p>If this map permits null values, then a return value of
     * {@code null} does not <i>necessarily</i> indicate that the map
     * contained no mapping for the key; it's also possible that the map
     * explicitly mapped the key to {@code null}.
     *
     * <p>The map will not contain a mapping for the specified key once the
     * call returns.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}.
     * @throws UnsupportedOperationException if the {@code remove} operation
     *         is not supported by this map
     * @throws ClassCastException if the key is of an inappropriate type for
     *         this map
     * (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key is null and this
     *         map does not permit null keys
     * (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     */
	public @Nullable V removeAndGet(K key)
	{
		Entry<K, V> old = map.remove(key);
		if (old == null)
			return null;
		
		return old.getValue();
	}

	/**
	 * @deprecated use {@link #removeAndGet(Object)} for better type-checking
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	@Override
	public @Nullable V remove(Object key)
	{
		return removeAndGet((K)key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m)
	{
		super.putAll(m);
	}

	@Override
	public void clear()
	{
		map.clear();
	}


	/**
	 * Base support for various iterators used by the map implementation.
	 */
	protected class RealEntryIterator
	{
		private final @Nonnull Iterator<@Nonnull Entry<K, V>> iter = map.values().iterator();

		public boolean hasNext()
		{
			return iter.hasNext();
		}

		@Nonnull
		public Entry<K, V> nextEntry()
		{
			return iter.next();
		}
		
		public void remove()
		{
			iter.remove();
		}
	}
	
	/**
	 * Iterator using {@link RHashMap.Entry}
	 */
	protected class EntryIterator extends RealEntryIterator implements Iterator<@Nonnull Entry<K, V>>
	{
		@Override
		public @Nonnull Entry<K, V> next()
		{
			return nextEntry();
		}
	}
	
	/**
	 * Iterator using {@link Map.Entry}
	 */
	protected class MapEntryIterator extends RealEntryIterator implements Iterator<Map.@Nonnull Entry<K, V>>
	{
		@Override
		public java.util.Map.@Nonnull Entry<K, V> next()
		{
			return nextEntry();
		}
	}
	
	/**
	 * Iterator for keys.
	 */
	protected class KeyIterator extends RealEntryIterator implements Iterator<K>
	{
		@Override
		public K next()
		{
			return nextEntry().getKey();
		}
	}
	
	/**
	 * Iterator for values.
	 */
	protected class ValueIterator extends RealEntryIterator implements Iterator<V>
	{
		@Override
		public V next()
		{
			return nextEntry().getValue();
		}
	}
	
	/**
	 * Key set for this map.
	 */
	protected class KeySet extends AbstractSet<K> {
    	@Override public final int size()                 { return RHashMap.this.size(); }
    	@Override public final void clear()               { RHashMap.this.clear(); }
    	@Override public final @Nonnull Iterator<K> iterator()     { return new KeyIterator(); }
    	@SuppressWarnings("unlikely-arg-type")
		@Override public final boolean contains(Object o) { return containsKey(o); }
    	@Override public final boolean remove(Object key) {
    		@SuppressWarnings("unlikely-arg-type") Entry<K, V> old = map.remove(key);
            return old != null;
        }
    }

	/**
	 * Key set instance for this map.
	 */
    protected transient Set<K>        keySet;
    
    /**
     * @deprecated use {@link #keys()} or {@link #liveKeys()}
     */
    @Deprecated
	@Override
	public @Nonnull Set<K> keySet()
	{
        Set<K> ks = keySet;
        if (ks == null) {
            ks = new KeySet();
            keySet = ks;
        }
        return ks;
	}
	

    /**
     * Values collection for this map.
     */
    protected class Values extends AbstractCollection<V> {
    	@Override public final int size()                 { return RHashMap.this.size(); }
    	@Override public final void clear()               { RHashMap.this.clear(); }
    	@Override public final Iterator<V> iterator()     { return new ValueIterator(); }
        /**
         * @deprecated this is highly inefficient, consider adjusting your model
         */
        @SuppressWarnings("unlikely-arg-type")
		@Deprecated
        @Override public final boolean contains(Object o) { return containsValue(o); }
    }
	
    /**
     * Values collection instance for this map.
     */
    protected transient Values values;
    
    /**
     * @deprecated use {@link #vals()} or {@link #liveVals()}
     */
    @Deprecated
	@Override
	public @Nonnull Collection<V> values()
	{
		Values vs = values;
        if (vs == null) {
            vs = new Values();
            values = vs;
        }
        return vs;
	}

    /**
     * Entry set for this map.
     */
    protected class EntrySet extends AbstractSet<Map.@Nonnull Entry<K,V>> {
    	@Override public final int size()                 { return RHashMap.this.size(); }
    	@Override public final void clear()               { RHashMap.this.clear(); }
    	@Override public final @Nonnull Iterator<Map.@Nonnull Entry<K,V>> iterator() {
            return new MapEntryIterator();
        }
    	@Override public final boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>) o;
            @SuppressWarnings("unchecked") K key = (K)e.getKey();
            
            Entry<K, V> entry = getRealEntry(key);
            if (entry == null)
            	return false;
            
            return Objects.equals(e.getValue(), entry.getValue()); 
        }
    	@Override public final boolean remove(Object o) {
        	throw new UnsupportedOperationException();
        }
    }
	
    /**
     * Entry set instance for this map.
     */
    protected transient Set<Map.@Nonnull Entry<K,V>> entrySet;
    
    /**
     * @deprecated use instance itself (for (var entry : rMap) ...) or {@link #entries()} or {@link #liveEntries()}
     */
    @Deprecated
	@Override
	public @Nonnull Set<Map.@Nonnull Entry<K, V>> entrySet()
	{
        Set<Map.@Nonnull Entry<K,V>> es = entrySet;
        if (es == null)
        {
        	es = new EntrySet();
        	entrySet = es;
        }
        return es;
	}

    /**
     * Returns a shallow copy of this {@code RHashMap} instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map
     */
	@Override
	@Nonnull
	@SuppressWarnings("unchecked")
	public RHashMap<K, V> clone()
	{
		return new RHashMap<>((HashMap<K, @Nonnull Entry<K, V>>)map.clone(), null); // use copy-constructor
	}

	/**
	 * @deprecated exists only for compatibility with for-loops, use instance
	 * itself (for (var entry : rMap)...), or {@link #entries()}, 
	 * or {@link #liveEntries()} instead
	 */
	@Deprecated
	@Override
	public @Nonnull Iterator<@Nonnull ReadOnlyEntry<K, V>> iterator()
	{
		return WACollections.remapIterator(new EntryIterator(), i -> i);
	}
	
	@Override
	@Nonnull
	public ForIterable<@Nonnull ReadOnlyEntry<K, V>> entries()
	{
		return this;
	}
	
	@Override
	@Nonnull
	public ForIterable<K> keys()
	{
		return WACollections.toForIterable(new KeyIterator());
	}
	
	@Override
	@Nonnull
	public ForIterable<V> vals()
	{
		return WACollections.toForIterable(new ValueIterator());
	}
	
	/**
	 * A live-view iterator for entries in this map that is backed by the map
	 * itself.
	 * <p>
	 * Items removed via {@link Iterator#remove()} are removed from the map itself.
	 * <p>
	 * It is also possible to change values via {@link Map.Entry#setValue(Object)}
	 */
	@Nonnull
	public Iterator<Map.@Nonnull Entry<K, V>> liveEntries()
	{
		return new MapEntryIterator();
	}
	
	/**
	 * A live-view iterator for keys in this map that is backed by the map
	 * itself.
	 * <p>
	 * Items removed via {@link Iterator#remove()} are removed from the map itself.
	 */
	@Nonnull
	public Iterator<K> liveKeys()
	{
		return new KeyIterator();
	}
	
	/**
	 * A live-view iterator for values in this map that is backed by the map
	 * itself.
	 * <p>
	 * Items removed via {@link Iterator#remove()} are removed from the map itself.
	 */
	@Nonnull
	public Iterator<V> liveVals()
	{
		return new ValueIterator();
	}

	/**
	 * @deprecated use {@link #getOrFallback(Object, Object)} for better type-checking
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	@Override
	public V getOrDefault(Object key, V defaultValue)
	{
		return getOrFallback((K)key, defaultValue);
	}

	@Override
	public V getOrFallback(K key, V defaultValue)
	{
		Entry<K, V> entry = getRealEntry(key);
		if (entry == null)
			return defaultValue;
		
		return entry.getValue();
	}

	@Override
	public @Nullable V putIfAbsent(K key, V value)
	{
		if (key != null)
		{
			Entry<K, V> entry = nn(map.computeIfAbsent(key, RHashMap::createNullKeyValueEntry));
			V old = entry.getValue(); // null if it was null or if it was just created
			if (entry.getKey() == null) // null key value means we have a new entry, otherwise retain the old one
			{
				entry.setKey(key);
				entry.setValue(value);
			}
			
			return old;
		}
		else
		{
			Entry<K, V> entry = nn(map.computeIfAbsent(key, RHashMap::createNonNullKeyNullValueEntry));
			V old = entry.getValue(); // null if it was null or if it was just created
			if (entry.getKey() != null) // non-null key value means we have a new entry, otherwise retain the old one
			{
				entry.setKey(key);
				entry.setValue(value);
			}
			
			return old;
		}
	}

    /**
     * Exactly equivalent to {@link Map#remove(Object, Object)} but with better type-checking.
     * <p>
     * Removes the entry for the specified key only if it is currently
     * mapped to the specified value.
     *
     * @param key key with which the specified value is associated
     * @param value value expected to be associated with the specified key
     * @return {@code true} if the value was removed
     * @throws UnsupportedOperationException if the {@code remove} operation
     *         is not supported by this map
     *         (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws ClassCastException if the key or value is of an inappropriate
     *         type for this map
     *         (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key or value is null,
     *         and this map does not permit null keys or values
     *         (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @since 1.8
     */
	public boolean removeIfValue(K key, V value)
	{
		int preSize = map.size();
		
		map.computeIfPresent(key, (k, entry) -> 
			Objects.equals(entry.getValue(), value) ? null : entry); // will return null if item matches, which leads to remove
		
		return preSize != map.size();
	}
	
	/**
	 * @deprecated use {@link #removeIfValue(Object, Object)} for better type-checking
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Object key, Object value)
	{
		return removeIfValue((K)key, (V)value);
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue)
	{
		Entry<K, V> entry = getRealEntry(key);
		if (entry == null)
			return false;
		
		if (!Objects.equals(oldValue, entry.getValue()))
			return false;
		
		entry.setValue(newValue);
		return true;
	}

	@Override
	public @Nullable V replace(K key, V value)
	{
		Entry<K, V> entry = getRealEntry(key);
		if (entry == null)
			return null;
		
		V prev = entry.getValue();
		entry.setValue(value);
		return prev;
	}

	@Override
	public @Nullable V computeIfAbsent(K key,
		@Nonnull Function<? super K, @Nullable ? extends @Nullable V> mappingFunction)
	{
		Entry<K, V> entry = map.computeIfAbsent(key, k -> {
			@Nullable V r = mappingFunction.apply(k);
			if (r == null)
				return null;
			
			return new Entry<>(k, r);
		});
		
		if (entry == null)
			return null;
		
		return entry.getValue();
	}
	
	@Override
	public @Nullable V computeIfPresent(K key,
		@Nonnull BiFunction<? super K, @Nonnull ? super @Nonnull V, @Nullable ? extends @Nullable V> remappingFunction)
	{
		Entry<K, V> entry = map.computeIfPresent(key, (k, mapEntry) -> {
			V v = mapEntry.getValue();
			if (v == null)
				return mapEntry; // if value is null, no remapping is performed
			
			@Nullable V r = remappingFunction.apply(k, v);
			if (r == null)
				return null;
			
			mapEntry.setValue(r);
			
			return mapEntry;
		});
		
		if (entry == null)
			return null;
		
		return entry.getValue();
	}

	@Override
	public @Nullable V compute(K key,
		@Nonnull BiFunction<? super K, @Nullable ? super @Nullable V, @Nullable ? extends @Nullable V> remappingFunction)
	{
		Entry<K, V> entry = map.compute(key, (k, mapEntry) -> {
			@Nullable V v = mapEntry == null ? null : mapEntry.getValue();
			
			@Nullable V r = remappingFunction.apply(k, v);
			if (r == null)
				return null;
			
			if (mapEntry == null)
				mapEntry = new Entry<>(k, r);
			else
				mapEntry.setValue(r);
			
			return mapEntry;
		});
		
		
		if (entry == null)
			return null;
		
		return entry.getValue();
	}

	@Override
	public @Nullable V merge(K key, @Nonnull V value,
		BiFunction<@Nonnull ? super @Nonnull V, @Nonnull ? super @Nonnull V, @Nullable ? extends @Nullable V> remappingFunction)
	{
		Entry<K, V> entry = map.merge(key, new Entry<>(key, value), (Entry<K,V> mapEntry, Entry<K, @Nonnull V> newEntry) -> {
			V v1 = mapEntry.getValue();
			@Nonnull V v2 = newEntry.getValue();
			if (v1 == null) // if stored value is null, we just replace the value with new one
			{
				mapEntry.setValue(v2);
				return mapEntry;
			}
			
			@Nullable V r = remappingFunction.apply(v1, v2);
			if (r == null)
				return null;
			
			mapEntry.setValue(r);
			
			return mapEntry;
		});
		
		
		if (entry == null)
			return null;
		
		return entry.getValue();
	}
	
	/**
	 * Unmodifiable Java map facade for this instance.
	 */
	@ToString.Exclude
	protected transient Map<K, V> unmodifiableJavaMap; 
	
	@Override
	@Nonnull
	public Map<K, V> toUnmodifiableJavaMap()
	{
		Map<K, V> ujm = unmodifiableJavaMap;
		if (ujm == null)
		{
			ujm = Collections.unmodifiableMap(this);
			unmodifiableJavaMap = ujm;
		}
		
		return ujm;
	}
}
