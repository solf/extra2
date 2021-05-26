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
 * An extended version of the visitor that is also notified whenever object 
 * graph utility is 'done' with a particular object
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public interface ObjectGraphExtendedVisitor extends ObjectGraphVisitor
{
	/**
	 * Invoked when object graph util is 'done' with a particular compound
	 * object (that is it read all its fields and will no longer access them).
	 * When this is invoked, it is safe to change any fields in the object -- it'll
	 * not affect object graph util operation.
	 */
	void finishedCompoundObject(Object compoundObject);
}
