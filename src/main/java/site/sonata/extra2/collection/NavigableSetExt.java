/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.collection;

import java.util.NavigableSet;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Extends {@link NavigableSet} with additiona methods from {@link SetExt}
 * 
 * @see SetExt
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public interface NavigableSetExt<E> extends SortedSetExt<E>, NavigableSet<E>
{
	// no additional methods
}
