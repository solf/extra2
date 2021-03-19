/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.objectgraph;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * 'Visitor' for object graph -- for each processed relation in the object
 * graph this will be invoked.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public interface ObjectGraphVisitor
{
	/**
	 * Visits relation.
	 * Conceptually we always visit a relation -- some parent, some field in the
	 * parent (or a collection in the parent), 'child' object which is actual
	 * visitee.
	 */
	public void visit(ObjectGraphRelation relation);
}
