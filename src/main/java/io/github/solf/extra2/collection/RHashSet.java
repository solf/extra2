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
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * A {@link Set} implementation based on {@link HashMap}/{@link HashSet} that 
 * recognizes the fact that item equality (according to hashCode/equals) is not
 * the same as items being identical.
 * <p>
 * A typical example might be an Object representing a row in the database -- it
 * is fully identifiable via its primary key (and thus hashCode/equals might
 * be based solely on the primary key) but at the same time it might contain
 * different values for non-primary key columns.
 * <p>
 * Therefore this {@link RHashSet} contains additional methods to replace values
 * in the set ({@link #addOrReplace(Object)}, e.g. if you're tracking
 * changes to be written to database rows, you might want to replace data for
 * specific row with the never one) and to get the actual value stored in the
 * set ({@link #get(Object)}, e.g. you can get latest row information from the
 * set by using object with only primary key information).
 * <p>
 * It also supports {@link ForIterable} and {@link ReadOnlySet} that provide
 * for cleaner interfaces where limited access to the {@link RHashSet} needs
 * to be provided.
 * <p>
 * NOTE: this implementation has specific overhead -- due to how {@link HashMap}
 * is implemented, if value in the set is replaced, the reference to the original
 * value is still retained as it is used as a key in the underlying {@link HashMap}.  
 *
 * @author Sergey Olefir
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class RHashSet<E> extends AbstractSet<E> implements Cloneable, Serializable, ReadOnlySet<E>
{
	/** UID for serialization */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Backing map for this set.
	 * <p>
	 * Keys are only used for identification, actual set values are stored in
	 * map values (this is because it is impossible to replace {@link HashMap}
	 * keys).
	 */
	@NonNull
	@Nonnull
	private HashMap<E, E> map;
	

    /**
     * Constructs a new, empty set; the backing {@code HashMap} instance has
     * default initial capacity (16) and load factor (0.75).
     */
    public RHashSet() {
        map = new HashMap<>();
    }

    /**
     * Constructs a new set containing the elements in the specified
     * collection.  The {@code HashMap} is created with default load factor
     * (0.75) and an initial capacity sufficient to contain the elements in
     * the specified collection.
     *
     * @param c the collection whose elements are to be placed into this set
     * @throws NullPointerException if the specified collection is null
     */
    public RHashSet(Collection<? extends E> c) {
        map = new HashMap<>(Math.max((int) (c.size()/.75f) + 1, 16));
        for (E e : c)
        	addOrReplace(e);
    }

    /**
     * Constructs a new, empty set; the backing {@code HashMap} instance has
     * the specified initial capacity and the specified load factor.
     *
     * @param      initialCapacity   the initial capacity of the hash map
     * @param      loadFactor        the load factor of the hash map
     * @throws     IllegalArgumentException if the initial capacity is less
     *             than zero, or if the load factor is nonpositive
     */
    public RHashSet(int initialCapacity, float loadFactor) {
        map = new HashMap<>(initialCapacity, loadFactor);
    }

    /**
     * Constructs a new, empty set; the backing {@code HashMap} instance has
     * the specified initial capacity and default load factor (0.75).
     *
     * @param      initialCapacity   the initial capacity of the hash table
     * @throws     IllegalArgumentException if the initial capacity is less
     *             than zero
     */
    public RHashSet(int initialCapacity) {
        map = new HashMap<>(initialCapacity);
    }

	
    /**
     * @deprecated use {@link #liveIterator()} instead
     */
	@Override
	@Nonnull
	@Deprecated
    public Iterator<E> iterator() {
        return liveIterator();
    }
	
	/**
	 * Returns a live iterator over the elements contained in this set.
	 * <p>
	 * Live iterator can be used to remove the elements from the set.
	 */
	@Nonnull
    public Iterator<E> liveIterator() {
        return map.values().iterator();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * @deprecated use {@link #has(Object)} for better type-checking
     */
    @SuppressWarnings("unchecked")
	@Override
	@Deprecated
	public boolean contains(Object o) {
        return has((E)o);
    }
    
	@Override
	public boolean has(E o) {
        return map.containsKey(o);
    }


	/**
	 * @deprecated use {@link #addIfAbsent(Object)} for clarity
	 */
	@Deprecated
	@Override
	public boolean add(E e)
	{
		int preSize = map.size();
		
        addIfAbsent(e);
        
        return preSize != map.size(); // true if map changed
	}
	
	
    /**
     * Adds (or replaces IF it is ALREADY PRESENT) the specified element to this set.
     * <p>
     * Since item equality via hashCode/equals does not guarantee that items are
     * identical otherwise, this method provides a way to actually REPLACE item
     * stored in the set even if a matching elements is already present. 
     *
     * @param e element to be added to this set
     * @return the PREVIOUS value stored in the set or null if there was no
     * 		matching element
     */
    @Nullable
    public E addOrReplace(E e) {
        return map.put(e, e);
    }
    
    /**
     * Adds the specified element to this set if it is NOT ALREADY present.
     * More formally, adds the specified element {@code e} to this set if
     * this set contains no element {@code e2} such that
     * {@code Objects.equals(e, e2)}.
     * If this set already contains the element, the call leaves the set
     * unchanged.
     *
     * @param e element to be added to this set
     * @return the PREVIOUS value stored in the set or null if there was no
     * 		matching element
     * 
     * @see #addIfAbsentAndGet(Object)
     */
    @Nullable
    public E addIfAbsent(E e) {
    	if (e == null)
    	{
    		map.put(e, e);
    		return null; // when adding 'null' we always return 'null', there's no possible other 'instance'
    	}
    	else
    	{
    		int preSize = map.size();
    		
    		E maybeOldValue = nn(map.computeIfAbsent(e, k -> e)); // we know this cannot be null as e is non-null
    		
    		if (map.size() != preSize)
    			return null; // map changed, so that means there was no old value
    		else
    			return maybeOldValue; // map unchanged, so there was something stored, return that
    	}
    }
    
    /**
     * Adds the specified element to this set if it is NOT ALREADY present.
     * More formally, adds the specified element {@code e} to this set if
     * this set contains no element {@code e2} such that
     * {@code Objects.equals(e, e2)}.
     * If this set already contains the element, the call leaves the set
     * unchanged.
     *
     * @param e element to be added to this set
     * @return the CURRENT value stored in the set after the method invocation (either
     * 		the one that was already there before or the new one specified as
     * 		argument)
     * 
     * @see #addIfAbsent(Object)
     */
    @Nullable
    public E addIfAbsentAndGet(E e) {
    	if (e == null)
    	{
    		map.put(e, e);
    		return null; // when adding 'null' we always return 'null', there's no possible other 'instance'
    	}
    	else
    		return map.computeIfAbsent(e, k -> e);
    }

    /**
     * @deprecated use {@link #removeAndGet(Object)} for better type-checking
     */
    @SuppressWarnings("unchecked")
    @Override
    @Deprecated
    public boolean remove(Object o) {
    	int beforeSize = map.size();
        removeAndGet((E)o);
        
        return beforeSize != map.size(); // true if map changed
    }

    /**
     * Removes the specified element from this set if it is present.
     * More formally, removes an element {@code e} such that
     * {@code Objects.equals(o, e)},
     * if this set contains such an element.
     *
     * @param o object to be removed from this set, if present
     * @return the matching element that was in the set prior to remove or null
     * 		if there was none
     */
    @Nullable
    public E removeAndGet(E o) {
        return map.remove(o);
    }
    
    @Override
    public void clear() {
        map.clear();
    }

	@Override
	public @Nullable E get(E item)
	{
		return map.get(item);
	}
    
    /**
     * Returns a shallow copy of this {@link RHashSet} instance: the elements
     * themselves are not cloned.
     *
     * @return a shallow copy of this set
     */
    @Override
    @Nonnull
	@SuppressWarnings("unchecked")
    public RHashSet<E> clone() {
       	return new RHashSet<E>((HashMap<E, E>) map.clone());
    }

	@Override
	public @Nonnull Stream<E> stream()
	{
		return ReadOnlySet.super.stream();
	}
	
	
	protected transient Set<E> unmodifiableJavaSet; 
	
	@Override
	@Nonnull
	public Set<E> toUnmodifiableJavaSet()
	{
		Set<E> ujs = unmodifiableJavaSet;
		if (ujs == null)
		{
			ujs = Collections.unmodifiableSet(this);
			unmodifiableJavaSet = ujs;
		}
		
		return ujs;
	}
	
	
}
