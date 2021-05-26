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

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Possible relation types between the parent and the child/visitee object. 
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public enum ObjectGraphRelationType
{
	/** Child/visitee is a field in parent */
	FIELD,
	/** Child/visitee is inside collection(s) in the parent; {@link ObjectGraphCollectionStep}(s) are used to determine exact location within collection(s) */
	ITEM_IN_COLLECTION,
	/** Child/visitee is a collection-type (including maps, arrays) field in parent; this is only used if collection objects themselves are requested; may be either direct field value or nested collection depending on path value */
	COLLECTION_INSTANCE,
	/** Child/visitee is a key in a map contained in parent's field; {@link ObjectGraphCollectionStep}(s) are used to further qualify location if Map is nested within other collection(s) */
	MAP_KEY,
	;
}
