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
 * NOTE: the implementation of this on top of standard {@link HashSet} is pretty 
 * cheap and can basically be used everywhere where {@link HashSet}s are normally
 * used EXCEPT for the note about replacing elements below.
 * <p>
 * NOTE ABOUT REPLACING ELEMENTS: this implementation has specific overhead -- 
 * due to how {@link HashMap}
 * is implemented, if value in the set is replaced, the reference to the original
 * value is still retained as it is used as a key in the underlying {@link HashMap}.  
 *
 * @author Sergey Olefir
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class RHashSet<E> extends AbstractSet<E> implements Cloneable, SerializableRSet<E>
{
	/** UID for serialization */
	private static final long serialVersionUID = 1L;
	
    /**
     * Constructs a new, empty set; the backing {@code HashMap} instance has
     * default initial capacity (16) and load factor (0.75).
     * <p>
     * This exists (in addition to constructors) in order to provide interface similar to {@link BHashSet}
     */
	@Nonnull
	public static <E> RHashSet<E> create()
	{
		return new RHashSet<E>();
	}
	

    /**
     * Constructs a new, empty set; the backing {@code HashMap} instance has
     * the specified initial capacity and the specified load factor.
     * <p>
     * This exists (in addition to constructors) in order to provide interface similar to {@link BHashSet}
     *
     * @param      initialCapacity   the initial capacity of the hash map
     * @param      loadFactor        the load factor of the hash map
     * @throws     IllegalArgumentException if the initial capacity is less
     *             than zero, or if the load factor is nonpositive
     */
	@Nonnull
	public static <E> RHashSet<E> create(int initialCapacity, float loadFactor) 
	{
		return new RHashSet<E>(initialCapacity, loadFactor);
    }

    /**
     * Constructs a new, empty set; the backing {@code HashMap} instance has
     * the specified initial capacity and default load factor (0.75).
     * <p>
     * This exists (in addition to constructors) in order to provide interface similar to {@link BHashSet}
     *
     * @param      initialCapacity   the initial capacity of the hash table
     * @throws     IllegalArgumentException if the initial capacity is less
     *             than zero
     */
	@Nonnull
	public static <E> RHashSet<E> create(int initialCapacity) 
	{
		return new RHashSet<E>(initialCapacity);
    }

    /**
     * Constructs a new set containing the elements in the specified
     * collection.  The {@code HashMap} is created with default load factor
     * (0.75) and an initial capacity sufficient to contain the elements in
     * the specified collection.
     * <p>
     * This exists (in addition to constructors) in order to provide interface similar to {@link BHashSet}
     *
     * @param c the collection whose elements are to be placed into this set
     * @throws NullPointerException if the specified collection is null
     */
	@Nonnull
	public static <E> RHashSet<E> create(@Nonnull Collection<? extends E> c) 
	{
		return new RHashSet<E>(c); 
    }

    /**
     * Constructs a new set containing the elements in the specified
     * {@link ReadOnlyCollection}.  The {@code HashMap} is created with default load factor
     * (0.75) and an initial capacity sufficient to contain the elements in
     * the specified {@link ReadOnlyCollection}.
     *
     * @param c the {@link ReadOnlyCollection} whose elements are to be placed into this set
     * @throws NullPointerException if the specified {@link ReadOnlySet} is null
     */
	@Nonnull
	public static <E> RHashSet<E> createFromReadOnly(@Nonnull ReadOnlyCollection<? extends E> c) 
	{
		if (c instanceof Collection)
		{
			@SuppressWarnings("unchecked") Collection<E> collection = (Collection<E>)c;
			return create(collection);
		}
		
		return create(c.toUnmodifiableJavaCollection()); 
    }
	
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
	
	@Override
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
	 * @deprecated use {@link #addIfAbsentAndGetIfPresent(Object)} for clarity
	 */
	@Deprecated
	@Override
	public boolean add(E e)
	{
		int preSize = map.size();
		
        addIfAbsentAndGetIfPresent(e);
        
        return preSize != map.size(); // true if map changed
	}

	@Override
	public boolean addIfAbsent(E e)
	{
		return add(e);
	}
	
	
    @Override
	@Nullable
    public E addOrReplace(E e) {
        return map.put(e, e);
    }
    
    @Override
	@Nullable
    public E addIfAbsentAndGetIfPresent(E e) {
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
    
    @Override
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
        return removeElement((E)o);
    }

	@Override
	public boolean removeElement(E o)
	{
    	int beforeSize = map.size();
        removeAndGet(o);
        
        return beforeSize != map.size(); // true if map changed
	}
    

    @Override
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
		return SerializableRSet.super.stream();
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
