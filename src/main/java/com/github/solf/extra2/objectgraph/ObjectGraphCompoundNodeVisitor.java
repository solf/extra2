/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.objectgraph;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Interface for visiting nodes on the graph that are compound (i.e. not primitive
 * and not collections).
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public interface ObjectGraphCompoundNodeVisitor
{
	/**
	 * Visits a compound node.
	 */
	public void visit(Object compoundNode);
}
