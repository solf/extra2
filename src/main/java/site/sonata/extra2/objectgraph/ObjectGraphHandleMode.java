/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.objectgraph;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Mode for handling some set of classes during the walk.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public enum ObjectGraphHandleMode
{
	/** Will cause an exception */
	EXCEPTION,
	/** Treated as primitive -- i.e. no deduplication & no 'going into' */
	PRIMITIVE,
	/** Treated as compound -- i.e. dedup & 'going into' to retrieve fields */
	COMPOUND,
	;
}
