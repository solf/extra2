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
 * 'Visitor' for object graph -- for each processed relation in the object
 * graph this will be invoked.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public interface ObjectGraphVisitor
{
	/**
	 * Visits relation.
	 * Conceptually we always visit a relation -- some parent, some field in the
	 * parent (or a collection in the parent), 'child' object which is actual
	 * visitee.
	 */
	public void visit(ObjectGraphRelation relation);
}
