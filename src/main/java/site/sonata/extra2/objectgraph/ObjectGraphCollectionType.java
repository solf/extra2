/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.objectgraph;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Types of supported collections.
 * 
 * Used in {@link ObjectGraphCollectionStep}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public enum ObjectGraphCollectionType
{
	/** Key is index */
	ARRAY,
	/** Key is map key */
	MAP,
	/** Key is index */
	LIST,
	/** Collection don't have keys per se, but they are still indexed (numbered) by ObjectGraph for reference */
	COLLECTION,
	;
}
