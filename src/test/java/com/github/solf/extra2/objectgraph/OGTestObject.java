/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.objectgraph;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.github.solf.extra2.objectgraph.ObjectGraphUtil;
import com.github.solf.extra2.objectgraph2.OGTestObject2;

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
