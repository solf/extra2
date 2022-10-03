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
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.experimental.Delegate;

/**
 * Thin wrapper implementation that allows representing any java sets as {@link SerializableBSet}
 * (although whether it is actually serializable depends on underlying set
 * serializability).
 * <p>
 * All operations on the wrapper are pass-through to the underlying set instance.
 * <p>
 * This has very little performance impact and can be used freely via e.g.
 * {@link BSet#of(Set)} or {@link ReadOnlySet#of(Set)}
 *
 * @author Sergey Olefir
 */
public class WrapperBSet<E> implements SerializableBSet<E>
{
	/** UID for serialization */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Underlying set.
	 */
	@Nonnull
	@Delegate
	private final Set<E> uset;
	
	/**
	 * Constructor.
	 */
	public WrapperBSet(@Nonnull Set<E> srcSet)
	{
		this.uset = srcSet;
	}

	@Override
	public @Nonnull Iterator<E> liveIterator()
	{
		return uset.iterator();
	}

	@Override
	public boolean has(E o)
	{
		return uset.contains(o);
	}
	
	@Override
	public boolean hasAll(Collection<E> c)
	{
		return uset.containsAll(c);
	}

	@Override
	public boolean removeElement(E o)
	{
		return uset.remove(o);
	}

	/**
	 * Cached unmodifiable set if was created previously.
	 */
	transient private volatile Set<E> cachedUnmodifiableSet = null;
	@Override
	public @Nonnull Set<E> toUnmodifiableJavaSet()
	{
		Set<E> result = cachedUnmodifiableSet;
		if (result == null)
		{
			result = Collections.unmodifiableSet(uset);
			cachedUnmodifiableSet = result;
		}
		
		return result;
	}

	@Override
	public int hashCode()
	{
		return uset.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object obj)
	{
		return uset.equals(obj);
	}

	@Override
	public @Nonnull String toString()
	{
		return uset.toString();
	}
}
