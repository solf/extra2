/*
 * Copyright Terracotta, Inc.
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

package site.sonata.extra2.collection;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.AbstractMap.SimpleEntry;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Concurrent weak identity hash map that is taken from ehcache3:
 * https://github.com/ehcache/ehcache3/blob/master/core/src/main/java/org/ehcache/core/collections/ConcurrentWeakIdentityHashMap.java
 * <p>
 * SOLF: BUT it contains fix from here: https://github.com/ehcache/ehcache3/pull/1071
 * <p>
 * Does NOT support null keys or values! 
 * 
 * @author Alex Snaps
 */
@ParametersAreNonnullByDefault
public class ConcurrentWeakIdentityHashMap<K, V> implements ConcurrentMap<K, V> {

  private final ConcurrentMap<WeakReference<K>, V> map = new ConcurrentHashMap<>();
  private final ReferenceQueue<K> queue = new ReferenceQueue<>();

  @Nullable
  @Override
  public V putIfAbsent(final K key, final V value) {
    purgeKeys();
    return map.putIfAbsent(newKey(key), value);
  }

  @Override
  public boolean remove(@SuppressWarnings("null") final Object key, @SuppressWarnings("null") final Object value) {
    purgeKeys();
    return map.remove(new WeakReference<>(key, null), value);
  }

  @Override
  public boolean replace(final K key, final V oldValue, final V newValue) {
    purgeKeys();
    return map.replace(newKey(key), oldValue, newValue);
  }


  @Override
  public V replace(final K key, final V value) {
    purgeKeys();
    return map.replace(newKey(key), value);
  }

  @Override
  public int size() {
    purgeKeys();
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    purgeKeys();
    return map.isEmpty();
  }

  @SuppressWarnings("unlikely-arg-type")
  @Override
  public boolean containsKey(@SuppressWarnings("null") final Object key) {
    purgeKeys();
    return map.containsKey(new WeakReference<>(key, null));
  }

  @SuppressWarnings("unlikely-arg-type")
  @Override
  public boolean containsValue(@SuppressWarnings("null") final Object value) {
    purgeKeys();
    return map.containsValue(value);
  }

  @Nullable
  @SuppressWarnings("unlikely-arg-type")
  @Override
  public V get(@SuppressWarnings("null") final Object key) {
    purgeKeys();
    return map.get(new WeakReference<>(key, null));
  }

  @Nullable
  @Override
  public V put(final K key, final V value) {
    purgeKeys();
    return map.put(newKey(key), value);
  }

  @Nullable
  @SuppressWarnings("unlikely-arg-type")
  @Override
  public V remove(@SuppressWarnings("null") final Object key) {
    purgeKeys();
    return map.remove(new WeakReference<>(key, null));
  }

  @Override
  public void putAll(@SuppressWarnings("null") final Map<? extends K, ? extends V> m) {
    purgeKeys();
    for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
      map.put(newKey(entry.getKey()), entry.getValue());
    }
  }

  @Override
  public void clear() {
    purgeKeys();
    map.clear();
  }

  @Override
  public Set<K> keySet() {
    return new AbstractSet<K>() {
      @Override
      public Iterator<K> iterator() {
        purgeKeys();
        return new WeakSafeIterator<K, WeakReference<K>>(map.keySet().iterator()) {
          @Override
          protected K extract(WeakReference<K> u) {
            return u.get();
          }
        };
      }

      @SuppressWarnings("unlikely-arg-type")
	  @Override
      public boolean contains(@SuppressWarnings("null") Object o) {
        return ConcurrentWeakIdentityHashMap.this.containsKey(o);
      }

      @Override
      public int size() {
        return map.size();
      }
    };
  }

  @Override
  public Collection<V> values() {
    purgeKeys();
    return map.values();
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return new AbstractSet<Entry<K, V>>() {
      @Override
      public Iterator<Entry<K, V>> iterator() {
        purgeKeys();
        return new WeakSafeIterator<Entry<K, V>, Entry<WeakReference<K>, V>>(map.entrySet().iterator()) {
          @Nullable
          @Override
          protected Entry<K, V> extract(Entry<WeakReference<K>, V> u) {
            K key = u.getKey().get();
            if (key == null) {
              return null;
            } else {
              return new SimpleEntry<>(key, u.getValue());
            }
          }
        };
      }

      @Override
      public int size() {
        return map.size();
      }
    };
  }

  @SuppressWarnings("unlikely-arg-type")
  private void purgeKeys() {
    Reference<? extends K> reference;
    while ((reference = queue.poll()) != null) {
      map.remove(reference);
    }
  }

  private WeakReference<K> newKey(final K key) {
    return new WeakReference<>(key, queue);
  }

  private static class WeakReference<T> extends java.lang.ref.WeakReference<T> {

    private final int hashCode;

    private WeakReference(final T referent, @Nullable final ReferenceQueue<? super T> q) {
      super(referent, q);
      
      // SOLF: changed code below as per: https://github.com/ehcache/ehcache3/pull/1071
      //hashCode = referent.hashCode();
      if (referent == null)
    	  throw new NullPointerException();
      hashCode = System.identityHashCode(referent);    
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
      return obj != null && obj.getClass() == this.getClass() && (this == obj || this.get() == ((WeakReference<?>)obj).get());
    }

    @Override
    public int hashCode() {
      return hashCode;
    }
  }

  private static abstract class WeakSafeIterator<T, U> implements Iterator<T> {

    private final Iterator<U> weakIterator;
    @Nullable
    protected T strongNext;

    public WeakSafeIterator(Iterator<U> weakIterator) {
      this.weakIterator = weakIterator;
      advance();
    }

    private void advance() {
      while (weakIterator.hasNext()) {
        U nextU = weakIterator.next();
        if ((strongNext = extract(nextU)) != null) {
          return;
        }
      }
      strongNext = null;
    }

    @Override
    public boolean hasNext() {
      return strongNext != null;
    }

    @SuppressWarnings("null")
	@Nullable
    @Override
    public final T next() {
      T next = strongNext;
      advance();
      return next;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }


    @Nullable
    protected abstract T extract(U u);
  }
} 