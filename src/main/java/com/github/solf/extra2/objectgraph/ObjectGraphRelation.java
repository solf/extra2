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
package com.github.solf.extra2.objectgraph;

import static com.github.solf.extra2.util.NullUtil.nn;

import java.lang.reflect.Field;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Information about a relation within object graph.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class ObjectGraphRelation implements Cloneable
{
	/**
	 * Parent object for this relation.
	 */
	private Object parent;
	
	/**
	 * Class actually containing field definition of this relation (fields can be overloaded in subclasses and can have different values).
	 */
	private Class<?> fieldContainer;
	
	/**
	 * Field name in the parent that contains (either directly or indirectly via arrays/collections) child/visitee.
	 */
	private String fieldName;
	
	/**
	 * Relation type.
	 */
	private ObjectGraphRelationType relationType;
	
	/**
	 * Path (within arrays/collections) from the field in the parent to the visitee/child -- null if visitee/child is contained directly.
	 */
	private ObjectGraphCollectionStep @Nullable[] path;
	
	/**
	 * Visitee/child in this relation; may be null if {@link ObjectGraphConfig#isIncludeNullFields()} is true.
	 */
	@Nullable
	private Object visitee;
	
	/**
	 * actual field that contains visitee/child (either directly or indirectly via arrays/collections). This field is already made 'accessible'.
	 */
	private Field field;

	/**
	 * Gets actual field that contains visitee/child (either directly or indirectly via arrays/collections). This field is already made 'accessible'.
	 * @return actual field that contains visitee/child (either directly or indirectly via arrays/collections). This field is already made 'accessible'
	 */
	public Field getField()
	{
		return field;
	}

	/**
	 * Sets actual field that contains visitee/child (either directly or indirectly via arrays/collections). This field is already made 'accessible'.
	 * @param newField new value of actual field that contains visitee/child (either directly or indirectly via arrays/collections). This field is already made 'accessible'
	 */
	public void setField(Field newField)
	{
		field = newField;
	}

	/**
	 * Gets visitee/child in this relation; may be null if {@link ObjectGraphConfig#isIncludeNullFields()} is true.
	 * @return visitee/child in this relation; may be null if {@link ObjectGraphConfig#isIncludeNullFields()} is true
	 */
	@Nullable
	public Object getVisitee()
	{
		return visitee;
	}

	/**
	 * Sets visitee/child in this relation; may be null if {@link ObjectGraphConfig#isIncludeNullFields()} is true.
	 * @param newVisitee new value of visitee/child in this relation; may be null if {@link ObjectGraphConfig#isIncludeNullFields()} is true
	 */
	public ObjectGraphRelation setVisitee(Object newVisitee)
	{
		visitee = newVisitee;
		return this;
	}

	/**
	 * Gets path (within arrays/collections) from the field in the parent to the visitee/child -- null if visitee/child is contained directly.
	 * @return path (within arrays/collections) from the field in the parent to the visitee/child -- null if visitee/child is contained directly
	 */
	public ObjectGraphCollectionStep @Nullable[] getPath()
	{
		return path;
	}

	/**
	 * Sets path (within arrays/collections) from the field in the parent to the visitee/child -- null if visitee/child is contained directly.
	 * @param newPath new value of path (within arrays/collections) from the field in the parent to the visitee/child -- null if visitee/child is contained directly
	 */
	public ObjectGraphRelation setPath(ObjectGraphCollectionStep[] newPath)
	{
		path = newPath;
		return this;
	}

	/**
	 * Gets relation type.
	 * @return relation type
	 */
	public ObjectGraphRelationType getRelationType()
	{
		return relationType;
	}

	/**
	 * Sets relation type.
	 * @param newRelationType new value of relation type
	 */
	public ObjectGraphRelation setRelationType(ObjectGraphRelationType newRelationType)
	{
		relationType = newRelationType;
		return this;
	}

	/**
	 * Gets field name in the parent that contains (either directly or indirectly via arrays/collections) child/visitee.
	 * @return field name in the parent that contains (either directly or indirectly via arrays/collections) child/visitee
	 */
	public String getFieldName()
	{
		return fieldName;
	}

	/**
	 * Sets field name in the parent that contains (either directly or indirectly via arrays/collections) child/visitee.
	 * @param newFieldName new value of field name in the parent that contains (either directly or indirectly via arrays/collections) child/visitee
	 */
	public ObjectGraphRelation setFieldName(String newFieldName)
	{
		fieldName = newFieldName;
		return this;
	}

	/**
	 * Gets class actually containing field definition of this relation (fields can be overloaded in subclasses and can have different values).
	 * @return class actually containing field definition of this relation (fields can be overloaded in subclasses and can have different values)
	 */
	public Class<?> getFieldContainer()
	{
		return fieldContainer;
	}

	/**
	 * Sets class actually containing field definition of this relation (fields can be overloaded in subclasses and can have different values).
	 * @param newFieldContainer new value of class actually containing field definition of this relation (fields can be overloaded in subclasses and can have different values)
	 */
	public ObjectGraphRelation setFieldContainer(Class<?> newFieldContainer)
	{
		fieldContainer = newFieldContainer;
		return this;
	}

	/**
	 * Gets parent object for this relation.
	 * @return parent object for this relation
	 */
	public Object getParent()
	{
		return parent;
	}

	/**
	 * Sets parent object for this relation.
	 * @param newParent new value of parent object for this relation
	 */
	public ObjectGraphRelation setParent(Object newParent)
	{
		parent = newParent;
		return this;
	}
	
	/**
	 * Constructor.
	 */
	public ObjectGraphRelation(
		Object parent, Class<?> fieldContainer, Field field, 
		String fieldName, ObjectGraphRelationType relationType, ObjectGraphCollectionStep @Nullable[] path, 
		@Nullable Object visitee
	)
	{
		this.parent = parent;
		this.field = field;
		this.fieldContainer = fieldContainer;
		this.fieldName = fieldName;
		this.relationType = relationType;
		this.path = path;
		this.visitee = visitee;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone()
		throws CloneNotSupportedException
	{
		return nn(super.clone());
	}
}
