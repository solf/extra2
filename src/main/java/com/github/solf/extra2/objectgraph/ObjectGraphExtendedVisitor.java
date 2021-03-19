/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.objectgraph;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * An extended version of the visitor that is also notified whenever object 
 * graph utility is 'done' with a particular object
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public interface ObjectGraphExtendedVisitor extends ObjectGraphVisitor
{
	/**
	 * Invoked when object graph util is 'done' with a particular compound
	 * object (that is it read all its fields and will no longer access them).
	 * When this is invoked, it is safe to change any fields in the object -- it'll
	 * not affect object graph util operation.
	 */
	void finishedCompoundObject(Object compoundObject);
}
