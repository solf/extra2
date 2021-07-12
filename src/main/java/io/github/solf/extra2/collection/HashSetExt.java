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
import java.util.HashSet;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * An extension to {@link HashSet}
 * 
 * Currently provides {@link #get(Object)} method.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
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
	 * @see io.github.solf.extra2.collection.ExtendedSet#get(java.lang.Object)
	 */
	@Override
	public @Nullable E get(E item)
		throws IllegalStateException
	{
		return WACollections.getSetItem(this, item);
	}

	
}
