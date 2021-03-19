/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.collection;

import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Extension interface for {@link Set}
 * 
 * Currently this implementation provides {@link #get(Object)} method.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public interface SetExt<E> extends Set<E>
{
	
	/**
	 * Gets set item from the set by 'itself'. Relevant in case stuff in Set has
	 * interesting fields that are not part of equals/hashCode.
	 * 
	 * TYPICAL IMPLEMENTATION USES REFLECTION!!!
	 * 
	 * @throws IllegalStateException if reflection fails for some reasons (shouldn't really happen)
	 */
	@Nullable
	public E get(E item) throws IllegalStateException;
}
