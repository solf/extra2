/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.objectgraph;

import java.util.ArrayList;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.solf.extra2.objectgraph.ObjectGraphRelation;
import com.github.solf.extra2.objectgraph.ObjectGraphVisitor;

/**
 * Collects data about all visited relations.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
/*package*/ class OGDataCollector implements ObjectGraphVisitor
{
	
	public final ArrayList<@Nonnull OGRelation> data = new ArrayList<>();

	/* (non-Javadoc)
	 * @see com.github.solf.extra2.objectgraph.ObjectGraphVisitor#visit(java.lang.Object, java.lang.Class, java.lang.String, com.github.solf.extra2.objectgraph.ObjectGraphRelationType, com.github.solf.extra2.objectgraph.ObjectGraphCollectionStep[], java.lang.Object)
	 */
	@Override
	public void visit(ObjectGraphRelation relation)
	{
		data.add(new OGRelation(relation.getParent(), relation.getFieldContainer(), relation.getFieldName(), relation.getRelationType(), relation.getPath(), relation.getVisitee()));
	}

}