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

import java.util.ArrayList;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import io.github.solf.extra2.objectgraph.ObjectGraphRelation;
import io.github.solf.extra2.objectgraph.ObjectGraphVisitor;

/**
 * Collects data about all visited relations.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
/*package*/ class OGDataCollector implements ObjectGraphVisitor
{
	
	public final ArrayList<@Nonnull OGRelation> data = new ArrayList<>();

	/* (non-Javadoc)
	 * @see io.github.solf.extra2.objectgraph.ObjectGraphVisitor#visit(java.lang.Object, java.lang.Class, java.lang.String, io.github.solf.extra2.objectgraph.ObjectGraphRelationType, io.github.solf.extra2.objectgraph.ObjectGraphCollectionStep[], java.lang.Object)
	 */
	@Override
	public void visit(ObjectGraphRelation relation)
	{
		data.add(new OGRelation(relation.getParent(), relation.getFieldContainer(), relation.getFieldName(), relation.getRelationType(), relation.getPath(), relation.getVisitee()));
	}

}
