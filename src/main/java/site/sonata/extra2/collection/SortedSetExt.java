/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.collection;

import java.util.SortedSet;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Extends {@link SortedSet} with additional methods from {@link SetExt}
 * 
 * @see SetExt
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public interface SortedSetExt<E> extends SetExt<E>, SortedSet<E>
{
	// no additional methods
}
