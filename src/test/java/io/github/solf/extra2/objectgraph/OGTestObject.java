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

import io.github.solf.extra2.objectgraph.ObjectGraphUtil;
import io.github.solf.extra2.objectgraph2.OGTestObject2;
import lombok.Getter;
import lombok.Setter;

/**
 * 'data' class for testing {@link ObjectGraphUtil}
 *
 * ATTN: nullability annotations are removed on purpose, because they made test
 * pain to write
 * 
 * @author Sergey Olefir
 */
/*package*/ class OGTestObject extends OGTOParent
{
	/**
	 * ID for instance identification.
	 */
	public final int id;
	
	/**
	 * Direct Reference to another compound object instance.
	 */
	public OGTestObject object = null;
	
	/**
	 * Reference to other test object in different package.
	 */
	public OGTestObject2 object2 = null;
	
	@SuppressWarnings("hiding")
	public transient String transientName = null; // For testing field hiding & transients
	
	public Map<String, OGTestObject> map = null;
	
	protected List<OGTestObject> list = null;
	
	/*package*/ Collection<OGTestObject> collection = null;
	
	/*package*/ Collection<OGTestSkip> skipCollection = null;
	
	@Getter @Setter
	private Map<OGTestSkip, OGTestSkip2> skipMap = null;
	
	private OGTestObject[] array = null;
	
	public Map<String, List<Collection<Map<String, OGTestObject>>>> nestedMap = null;
	
	public OGTestEnum sampleEnum = null;
	
	public Map<OGTestObject, OGTestObject> complexKeyMap = null;
	
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
	public OGTestObject[] getArray()
	{
		return array;
	}
	
	/**
	 * Sets array.
	 */
	public OGTestObject setArray(OGTestObject[] array)
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
