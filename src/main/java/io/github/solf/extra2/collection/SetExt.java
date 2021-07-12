/**
 * Copyright Sergey Olefir
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.solf.extra2.collection;

import java.util.Set;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Extension interface for {@link Set}
 * 
 * Currently this implementation provides {@link #get(Object)} method.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
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
