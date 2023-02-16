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
package io.github.solf.extra2.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.collection.BArrayList;
import io.github.solf.extra2.collection.BList;
import io.github.solf.extra2.collection.ReadOnlyList;
import io.github.solf.extra2.util.reflection.FieldDefinition;
import io.github.solf.extra2.util.reflection.FieldNullType;
import io.github.solf.extra2.util.reflection.MappingName;

/**
 * Some reflection-related utilities.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class ReflectionUtil
{
	/**
	 * {@link Method#isAccessible()} is deprecated since 1.9 with no direct
	 * replacement usable in JDK8/11.
	 * <p>
	 * So this is a wrapper that can be used to avoid the warning.
	 */
	@SuppressWarnings("all") // should really be @SuppressWarnings("deprecation") but 'all' doesn't produce warnings in both 8 and 11 java 
	public static boolean isAccessible(Method method)
	{
		return method.isAccessible();
	}
	
	/**
	 * {@link Field#isAccessible()} is deprecated since 1.9 with no direct
	 * replacement usable in JDK8/11.
	 * <p>
	 * So this is a wrapper that can be used to avoid the warning.
	 */
	@SuppressWarnings("all") // should really be @SuppressWarnings("deprecation") but 'all' doesn't produce warnings in both 8 and 11 java
	public static boolean isAccessible(Field field)
	{
		return field.isAccessible();
	}

	/**
	 * Retrieves a list of fields defined in some class and classifies them
	 * according to their flags (static, final) and nullability.
	 * <p>
	 * Uses {@link MappingName} annotation for determining field mapping names.
	 * 
	 * @param javaClass class to retrieve fields for
	 * @param includeParentClasses whether parent classes are also scanned
	 */
	public static ReadOnlyList<FieldDefinition> getClassFields(Class<?> javaClass, boolean includeParentClasses)
		throws InaccessibleObjectException, SecurityException
	{
		return getClassFields(javaClass, includeParentClasses, MappingName.class, MappingName::value);
	}
	
	/**
	 * Retrieves a list of fields defined in some class and classifies them
	 * according to their flags (static, final) and nullability.
	 * 
	 * @param javaClass class to retrieve fields for
	 * @param includeParentClasses whether parent classes are also scanned
	 * @param mappingNameAnnotationClazz class type of annotation used to determine
	 * 		'mapping name' for a field (e.g. {@link MappingName}); if annotation
	 * 		is not found, then field name is used as a mapping name
	 * @param retrieveMappingNameFunction function to retrieve mapping name from
	 * 		mapping name annotation (if such annotation is present); may return
	 * 		null, in which case field name is used as a mapping name; e.g. MappingName::value
	 */
	public static <A extends Annotation> ReadOnlyList<FieldDefinition> getClassFields(Class<?> javaClass, boolean includeParentClasses,
		Class<A> mappingNameAnnotationClazz, Function<A, @Nullable String> retrieveMappingNameFunction)
		throws InaccessibleObjectException, SecurityException
	{
		BList<FieldDefinition> result = BArrayList.create();
		
		for (Class<?> current = javaClass; ; 
			current = includeParentClasses ? current.getSuperclass() : Object.class)
		{
			if (current.getSuperclass() == null)
				break; // current class is Object or primitive
			
			for (Field field : current.getDeclaredFields())
			{
				field.setAccessible(true);
				
				final FieldNullType nullType;
				if (field.getType().isPrimitive())
					nullType = FieldNullType.PRIMITIVE;
				else
				{
					Nullable isNullable = field.getAnnotation(Nullable.class);
					if (isNullable == null)
						isNullable = field.getAnnotatedType().getAnnotation(Nullable.class);
					if (isNullable != null)
						nullType = FieldNullType.NULLABLE;
					else
						nullType = FieldNullType.NON_NULL;
				}
				switch (nullType)
				{
					case NON_NULL:
					case NULLABLE:
					case PRIMITIVE:
						// does nothing, serves as a marker to edit code above
						// this switch in case possible values of enum change
						break;
				}
				
				final @Nonnull String mappingName;
				{
					String name = null;
					@Nullable A mappingNameAnno = field.getAnnotation(mappingNameAnnotationClazz);
					if (mappingNameAnno != null)
						name = retrieveMappingNameFunction.apply(mappingNameAnno);
					
					if (name == null)
						mappingName = field.getName();
					else
						mappingName = name;
				}
				
				result.add(new FieldDefinition(field,
					Modifier.isStatic(field.getModifiers()),
					Modifier.isFinal(field.getModifiers()),
					nullType, mappingName));
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves a list of fields (excluding static fields) defined in some class and classifies them
	 * according to their flags (static, final) and nullability.
	 * <p>
	 * Uses {@link MappingName} annotation for determining field mapping names.
	 * 
	 * @param javaClass class to retrieve fields for
	 * @param includeParentClasses whether parent classes are also scanned
	 */
	public static ReadOnlyList<FieldDefinition> getClassFieldsNonStatic(Class<?> javaClass, boolean includeParentClasses)
		throws InaccessibleObjectException, SecurityException
	{
		return getClassFieldsNonStatic(javaClass, includeParentClasses, MappingName.class, MappingName::value);
	}
	
	/**
	 * Retrieves a list of fields (excluding static fields) defined in some class and classifies them
	 * according to their flags (static, final) and nullability.
	 * 
	 * @param javaClass class to retrieve fields for
	 * @param includeParentClasses whether parent classes are also scanned
	 * @param mappingNameAnnotationClazz class type of annotation used to determine
	 * 		'mapping name' for a field (e.g. {@link MappingName}); if annotation
	 * 		is not found, then field name is used as a mapping name
	 * @param retrieveMappingNameFunction function to retrieve mapping name from
	 * 		mapping name annotation (if such annotation is present); may return
	 * 		null, in which case field name is used as a mapping name; e.g. MappingName::value
	 */
	public static <A extends Annotation> ReadOnlyList<FieldDefinition> getClassFieldsNonStatic(Class<?> javaClass, boolean includeParentClasses,
		Class<A> mappingNameAnnotationClazz, Function<A, @Nullable String> retrieveMappingNameFunction)
		throws InaccessibleObjectException, SecurityException
	{
		@Nonnull ReadOnlyList<FieldDefinition> tmp = getClassFields(javaClass, includeParentClasses, mappingNameAnnotationClazz, retrieveMappingNameFunction);
		
		BList<FieldDefinition> result = BArrayList.create(tmp.size());
		for (FieldDefinition fd : tmp)
		{
			if (!fd.isStatic())
				result.add(fd);
		}
		
		return result;
	}
	
	/**
	 * Retrieves a list of fields (excluding static and final fields) defined in some class and classifies them
	 * according to their flags (static, final) and nullability.
	 * <p>
	 * Uses {@link MappingName} annotation for determining field mapping names.
	 * 
	 * @param javaClass class to retrieve fields for
	 * @param includeParentClasses whether parent classes are also scanned
	 */
	public static ReadOnlyList<FieldDefinition> getClassFieldsNonStaticNonFinal(Class<?> javaClass, boolean includeParentClasses)
		throws InaccessibleObjectException, SecurityException
	{
		return getClassFieldsNonStaticNonFinal(javaClass, includeParentClasses, MappingName.class, MappingName::value);
	}
	
	/**
	 * Retrieves a list of fields (excluding static and final fields) defined in some class and classifies them
	 * according to their flags (static, final) and nullability.
	 * 
	 * @param javaClass class to retrieve fields for
	 * @param includeParentClasses whether parent classes are also scanned
	 * @param mappingNameAnnotationClazz class type of annotation used to determine
	 * 		'mapping name' for a field (e.g. {@link MappingName}); if annotation
	 * 		is not found, then field name is used as a mapping name
	 * @param retrieveMappingNameFunction function to retrieve mapping name from
	 * 		mapping name annotation (if such annotation is present); may return
	 * 		null, in which case field name is used as a mapping name; e.g. MappingName::value
	 */
	public static <A extends Annotation> ReadOnlyList<FieldDefinition> getClassFieldsNonStaticNonFinal(Class<?> javaClass, boolean includeParentClasses,
		Class<A> mappingNameAnnotationClazz, Function<A, @Nullable String> retrieveMappingNameFunction)
		throws InaccessibleObjectException, SecurityException
	{
		@Nonnull ReadOnlyList<FieldDefinition> tmp = getClassFields(javaClass, includeParentClasses, mappingNameAnnotationClazz, retrieveMappingNameFunction);
		
		BList<FieldDefinition> result = BArrayList.create(tmp.size());
		for (FieldDefinition fd : tmp)
		{
			if ((!fd.isFinal()) && (!fd.isStatic()))
				result.add(fd);
		}
		
		return result;
	}
}
