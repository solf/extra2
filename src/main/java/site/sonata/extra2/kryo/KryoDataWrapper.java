/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.kryo;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Wrapper for data stored by {@link KryoDB} -- contains any necessary metadata.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
/*package*/ class KryoDataWrapper<T>
{
	/**
	 * Actual data.
	 */
	@Nullable
	T data;
	
	/**
	 * Data version (default non-existent value is 0).
	 */
	int version = 0;
}
