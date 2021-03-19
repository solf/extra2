/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.objectgraph;

import static com.github.solf.extra2.util.NullUtil.nn;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.solf.extra2.objectgraph.ObjectGraphUtil;

/**
 * Enum for testing {@link ObjectGraphUtil} functionality
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
/*package*/ enum OGTestEnum
{
	VAL1,
	VAL2,
	;
	
	/**
	 * lower case name
	 */
	public final String lowerCaseName;
	
	/**
	 * Constructor.
	 */
	private OGTestEnum()
	{
		lowerCaseName = nn(name().toLowerCase());
	}
}
