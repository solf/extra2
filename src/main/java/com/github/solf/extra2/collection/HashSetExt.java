/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.collection;

import java.util.Collection;
import java.util.HashSet;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * An extension to {@link HashSet}
 * 
 * Currently provides {@link #get(Object)} method.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class HashSetExt<E> extends HashSet<E> implements SetExt<E>
{

	/**
	 * 
	 */
	public HashSetExt()
	{
		super();
	}

	/**
	 * @param c
	 */
	public HashSetExt(Collection<? extends E> c)
	{
		super(c);
	}

	/**
	 * @param initialCapacity
	 * @param loadFactor
	 */
	public HashSetExt(int initialCapacity, float loadFactor)
	{
		super(initialCapacity, loadFactor);
	}

	/**
	 * @param initialCapacity
	 */
	public HashSetExt(int initialCapacity)
	{
		super(initialCapacity);
	}

	/* (non-Javadoc)
	 * @see site.sonata.extra2.collection.ExtendedSet#get(java.lang.Object)
	 */
	@Override
	public @Nullable E get(E item)
		throws IllegalStateException
	{
		return WACollections.getSetItem(this, item);
	}

	
}
