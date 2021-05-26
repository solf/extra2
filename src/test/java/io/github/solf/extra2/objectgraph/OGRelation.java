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

import static io.github.solf.extra2.util.NullUtil.nn;

import java.util.Arrays;

import javax.annotation.Nullable;
import javax.annotation.NonNullByDefault;

import io.github.solf.extra2.objectgraph.ObjectGraphCollectionStep;
import io.github.solf.extra2.objectgraph.ObjectGraphRelationType;

/**
 * Records information about single relation.
 * Has proper hash code / equals.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class OGRelation
{
	public final Object parent;
	public final Class<?> fieldContainer; 
	public final String fieldName;
	public final ObjectGraphRelationType relationType;
	public final ObjectGraphCollectionStep @Nullable[] path; 
	public final @Nullable Object visitee;	
	
	/**
	 * Constructor.
	 */
	public OGRelation(Object parent, Class<?> fieldContainer, 
		String fieldName, ObjectGraphRelationType relationType, ObjectGraphCollectionStep @Nullable[] path, 
		@Nullable Object visitee)
	{
		this.parent = parent;
		this.fieldContainer = fieldContainer;
		this.fieldName = fieldName;
		this.relationType = relationType;
		this.path = path;
		this.visitee = visitee;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return
			""
			+ parent + '(' + fieldContainer.getSimpleName() + ')'
			+ '.' + fieldName
			+ (path == null ? "" : Arrays.toString(path))
			+ ' ' + relationType + ' '
			+ visitee
			;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
//		return DeepEquals.deepHashCode(this);
		int result = parent.hashCode();
		result = (result << 1) + fieldContainer.hashCode();
		result = (result << 1) + fieldName.hashCode();
		result += relationType.ordinal();
		if (path != null)
		{
			for (ObjectGraphCollectionStep entry : path)
			{
				result = (result << 1) + entry.getContainingCollectionType().hashCode();
				Object key = entry.getKey();
				if (key != null)
					result = (result << 1) + key.hashCode();
			}
		}
		if (visitee != null)
			result = (result << 1) + nn(visitee).hashCode();
		
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(@Nullable Object obj)
	{
//		return DeepEquals.deepEquals(this, obj);
		if (!(obj instanceof OGRelation))
			return false;
		
		OGRelation other = (OGRelation)obj;
		
		if (parent != other.parent)
			return false;
		if (fieldContainer != other.fieldContainer)
			return false;
		if (!fieldName.equals(other.fieldName))
			return false;
		if (relationType != other.relationType)
			return false;
		ObjectGraphCollectionStep[] p = path;
		ObjectGraphCollectionStep[] op = other.path;
		if (p != null)
		{
			if (op == null)
				return false;
			
			if (p.length != op.length)
				return false;
			
			for (int i = 0; i < p.length; i++)
			{
				if (p[i].getContainingCollectionType() != op[i].getContainingCollectionType())
					return false;
				
				Object key = p[i].getKey(); 
				
				if (key == null)
				{
					if (op[i].getKey() != null)
						return false;
				}
				else
				{
					if (op[i].getKey() == null)
						return false;
					
					if (key instanceof OGTestObject)
					{
						if (key != op[i].getKey())
							return false;
					}
					else
					{
						if (!key.equals(op[i].getKey()))
							return false;
					}
				}
			}
		}
		else if (op != null)
			return false;
		
		Object v = visitee;
		if (v == null)
		{
			if (other.visitee != null)
				return false;
		}
		else
		{
			if (v instanceof OGTestObject)
			{
				if (v != other.visitee)
					return false;
			}
			else
			{
				if (!v.equals(other.visitee))
					return false;
			}
		}
		
		return true;
	}
	
	
}
