/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.objectgraph2;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Another test object for object graph utils that is in different package
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class OGTestObject2
{
	/**
	 * String id.
	 */
	public final String stringId;
	
	/**
	 * Constructor.
	 */
	public OGTestObject2(String stringId)
	{
		this.stringId = stringId;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + ':' + stringId;
	}
}
