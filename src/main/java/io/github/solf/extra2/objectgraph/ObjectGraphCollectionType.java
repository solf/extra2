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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Types of supported collections.
 * 
 * Used in {@link ObjectGraphCollectionStep}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
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
