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
package io.github.solf.extra2.objectgraph;

import static io.github.solf.extra2.util.NullUtil.nn;

import javax.annotation.Nullable;
import javax.annotation.NonNullByDefault;

/**
 * Step (reference) inside a collection.
 * 
 * Basically suppose we have object A that contains filed F with map M which has entry
 * with key K and value V (another compound object). In this case this will result
 * in visitation for V like this:
 * parent=A, field=F, type=IN_COLLECTION, visitee=V, path=(type=MAP, key=K)
 * 
 * The 'path' may contain more than entry in case we have collection-in-collection(... -in-collection)
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class ObjectGraphCollectionStep
{
	/**
	 * Containing collection type.
	 */
	private final ObjectGraphCollectionType containingCollectionType;
	
	/**
	 * Key within containing collection (map key or array/list index; simple
	 * collections don't have key/index per se, but they are still numbered
	 * by {@link ObjectGraphUtil} for reference).
	 * Can be null because some maps can have null keys.
	 */
	@Nullable
	private final Object key;
	
	/**
	 * Constructor.
	 */
	public ObjectGraphCollectionStep(ObjectGraphCollectionType containingCollectionType,
		@Nullable Object key)
	{
		this.containingCollectionType = containingCollectionType;
		this.key = key;
	}
	
	/**
	 * Gets containing collection type.
	 */
	public ObjectGraphCollectionType getContainingCollectionType()
	{
		return containingCollectionType;
	}
	
	/**
	 * Key within containing collection (map key or array/list index; simple
	 * collections don't have key/index per se, but they are still numbered
	 * by {@link ObjectGraphUtil} for reference).
	 * Can be null because some maps can have null keys.
	 */
	@Nullable
	public Object getKey()
	{
		return key;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return (getKey() == null ? "null" : nn(getKey()).toString()) + '(' + getContainingCollectionType().toString().toLowerCase() + ')';
	}
}
