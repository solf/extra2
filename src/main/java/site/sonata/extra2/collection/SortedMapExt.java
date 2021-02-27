/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.collection;

import java.util.SortedMap;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Extends {@link SortedMap} with additional methods from {@link MapExt}
 * 
 * @see MapExt
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public interface SortedMapExt<K, V> extends MapExt<K, V>, SortedMap<K, V>
{
	// No additional methods
}
