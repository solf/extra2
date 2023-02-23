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
package io.github.solf.extra2.objectgraph;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.objectgraph.ObjectGraphUtil.ObjectGraphVisiteeHandleMode;

/**
 * Instances of this interface can be used in {@link ObjectGraphConfig} / 
 * {@link ObjectGraphUtil} in order to customize how visitees are handled
 * (can be used to e.g. skip visiting certain classes or some such).
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public interface ObjectGraphHandlingResolver
{

	/**
	 * Determines how given visitee should be handled.
	 * <p>
	 * Most arguments are for error reporting.
	 * 
	 * @param isKnownToBePrimitive true if the visitee is known to be primitive --
	 * 		such as fields with Java primitive types or arrays of primitives
	 * 
	 * @return handling mode for the visitee or null if default handling should
	 * 		be used
	 * 
	 * @throws ObjectGraphUnhandledTypeException if specific type is determined
	 * 		to require throwing exception (which is then thrown).
	 */
	@Nullable
	public ObjectGraphVisiteeHandleMode determineHandling(ObjectGraphConfig cfg,
		@Nullable Object parent, @Nullable Class<?> fieldContainer, 
		boolean isKnownToBePrimitive,
		@Nullable String fieldName, @Nullable ObjectGraphRelationType relationType, ObjectGraphCollectionStep @Nullable[] path, 
		Object visitee)
			throws ObjectGraphUnhandledTypeException;

}
