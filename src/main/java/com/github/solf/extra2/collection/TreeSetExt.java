/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.collection;

import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Extension for {@link TreeSet}
 * 
 * Currently provides {@link #get(Object)} method.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class TreeSetExt<E> extends TreeSet<E> implements NavigableSetExt<E>
{

	/**
	 * 
	 */
	public TreeSetExt()
	{
		super();
	}

	/**
	 * @param c
	 */
	public TreeSetExt(Collection<? extends E> c)
	{
		super(c);
	}

	/**
	 * @param comparator
	 */
	public TreeSetExt(@ Nullable Comparator<? super E> comparator)
	{
		super(comparator);
	}

	/**
	 * @param s
	 */
	public TreeSetExt(SortedSet<E> s)
	{
		super(s);
	}

	/* (non-Javadoc)
	 * @see com.github.solf.extra2.collection.ExtendedSet#get(java.lang.Object)
	 */
	@Override
	public @Nullable E get(E item)
		throws IllegalStateException
	{
		return WACollections.getSetItem(this, item);
	}

}
