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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Thin wrapper implementation that allows representing any java lists as {@link SerializableBList}
 * (although whether it is actually serializable depends on underlying list
 * serializability).
 * <p>
 * All operations on the wrapper are pass-through to the underlying list instance.
 * <p>
 * This has very little performance impact and can be used freely via e.g.
 * {@link BList#of(List)} or {@link ReadOnlyList#of(List)}
 *
 * @author Sergey Olefir
 */
public class WrapperBList<E> extends DelegateList<E> implements SerializableBList<E>
{
	/** UID for serialization */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Constructor.
	 */
	public WrapperBList(@Nonnull List<E> srcList)
	{
		super(srcList);
	}

	@Override
	public @Nonnull Iterator<E> liveIterator()
	{
		return ulist.iterator();
	}

	@Override
	public boolean has(E o)
	{
		return ulist.contains(o);
	}
	
	@Override
	public boolean hasAll(Collection<E> c)
	{
		return ulist.containsAll(c);
	}

	@Override
	public boolean removeElement(E o)
	{
		return ulist.remove(o);
	}

	@Override
	public int indexOfElement(E o)
	{
		return indexOf(o);
	}

	@Override
	public int lastIndexOfElement(E o)
	{
		return lastIndexOf(o);
	}

	/**
	 * Cached unmodifiable list if was created previously.
	 */
	transient private volatile List<E> cachedUnmodifiableList = null;
	@Override
	public @Nonnull List<E> toUnmodifiableJavaList()
	{
		List<E> result = cachedUnmodifiableList;
		if (result == null)
		{
			result = Collections.unmodifiableList(ulist);
			cachedUnmodifiableList = result;
		}
		
		return result;
	}

	@Override
	public int hashCode()
	{
		return ulist.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object obj)
	{
		return ulist.equals(obj);
	}

	@Override
	public @Nonnull String toString()
	{
		return ulist.toString();
	}
	
	@Override
	public @Nonnull BList<E> subList(int fromIndex, int toIndex)
	{
		return new WrapperBList<>(super.subList(fromIndex, toIndex));
	}
	
}
