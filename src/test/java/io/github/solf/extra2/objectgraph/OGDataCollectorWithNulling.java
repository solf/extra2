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

import javax.annotation.NonNullByDefault;

import io.github.solf.extra2.objectgraph.ObjectGraphRelation;

/**
 * A version of {@link OGDataCollector} that also nulls any non-primitive field
 * (e.g. to test what happens when structure is being updated as it is being
 * processed).
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class OGDataCollectorWithNulling extends OGDataCollector
{

	/* (non-Javadoc)
	 * @see io.github.solf.extra2.objectgraph.OGDataCollector#visit(io.github.solf.extra2.objectgraph.ObjectGraphRelation)
	 */
	@Override
	public void visit(ObjectGraphRelation relation)
	{
		super.visit(relation);
		
		if (!relation.getField().getType().isPrimitive())
		{
			try
			{
				relation.getField().set(relation.getParent(), null);
			} catch( Exception e )
			{
				throw new IllegalStateException("Unexpected reflection failure: " + e, e);
			}
		}
	}

}
