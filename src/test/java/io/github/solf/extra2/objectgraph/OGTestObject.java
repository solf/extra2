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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.objectgraph2.OGTestObject2;
import lombok.Getter;
import lombok.Setter;

/**
 * 'data' class for testing {@link ObjectGraphUtil}
 * 
 * @author Sergey Olefir
 */
@NonNullByDefault
/*package*/ class OGTestObject extends OGTOParent
{
	/**
	 * ID for instance identification.
	 */
	public final int id;
	
	/**
	 * Direct Reference to another compound object instance.
	 */
	@Nullable
	public OGTestObject object = null;
	
	/**
	 * Reference to other test object in different package.
	 */
	@Nullable
	public OGTestObject2 object2 = null;
	
	@SuppressWarnings("hiding")
	@Nullable
	public transient String transientName = null; // For testing field hiding & transients
	
	@Nullable
	public Map<String, OGTestObject> map = null;
	
	@Nullable
	protected List<OGTestObject> list = null;
	
	@Nullable
	/*package*/ Collection<@Nullable OGTestObject> collection = null;
	
	@Nullable
	/*package*/ Collection<OGTestSkip> skipCollection = null;
	
	@Getter @Setter
	@Nullable
	private Map<OGTestSkip, OGTestSkip2> skipMap = null;
	
	private @Nullable OGTestObject @Nullable[] array = null;
	
	@Nullable
	public Map<String, List<Collection<Map<@Nullable String, @Nullable OGTestObject>>>> nestedMap = null;
	
	@Nullable
	public OGTestEnum sampleEnum = null;
	
	@Nullable
	public Map<@Nullable OGTestObject, @Nullable OGTestObject> complexKeyMap = null;
	
	/**
	 * Constructor.
	 */
	public OGTestObject(int id)
	{
		this.id = id;
	}
	
	/**
	 * Gets array.
	 */
	public @Nullable OGTestObject @Nullable[] getArray()
	{
		return array;
	}
	
	/**
	 * Sets array.
	 */
	public OGTestObject setArray(@Nullable OGTestObject @Nullable[] array)
	{
		this.array = array;
		
		return this;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public @Nonnull String toString()
	{
		return this.getClass().getSimpleName() + '[' + id + ']';
	}
}
