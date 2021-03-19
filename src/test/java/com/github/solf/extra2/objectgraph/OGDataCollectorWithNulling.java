/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.objectgraph;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.solf.extra2.objectgraph.ObjectGraphRelation;

/**
 * A version of {@link OGDataCollector} that also nulls any non-primitive field
 * (e.g. to test what happens when structure is being updated as it is being
 * processed).
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class OGDataCollectorWithNulling extends OGDataCollector
{

	/* (non-Javadoc)
	 * @see com.github.solf.extra2.objectgraph.OGDataCollector#visit(com.github.solf.extra2.objectgraph.ObjectGraphRelation)
	 */
	@Override
	public void visit(ObjectGraphRelation relation)
	{
		super.visit(relation);
		
		if (!relation.getField().getType().isPrimitive())
		{
			try
			{
				relation.getField().set(relation.getParent(), null);
			} catch( Exception e )
			{
				throw new IllegalStateException("Unexpected reflection failure: " + e, e);
			}
		}
	}

}
