/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.collection;

import java.util.NavigableMap;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Extends {@link NavigableMap} with additional methods from {@link MapExt}
 * 
 * @see MapExt
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public interface NavigableMapExt<K, V> extends SortedMapExt<K, V>, NavigableMap<K, V>
{
	// No additional methods
}
