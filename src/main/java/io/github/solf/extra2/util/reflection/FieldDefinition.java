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
package io.github.solf.extra2.util.reflection;

import java.lang.reflect.Field;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.util.ReflectionUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Field definition used in certain code analysis, such as in
 * {@link ReflectionUtil#getClassFields(Class, boolean, Class, java.util.function.Function)}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
@RequiredArgsConstructor
@ToString
public class FieldDefinition
{
	/**
	 * Actual field that can be used to access value.
	 * <p>
	 * These should already be set to 'accessible'.
	 */
	@Getter
	private final Field field;
	
	/**
	 * Whether field is static.
	 */
	@Getter
	private final boolean isStatic;
	
	/**
	 * Whether field is final.
	 */
	@Getter
	private final boolean isFinal;
	
	/**
	 * Field classification in terms of nullability (primitive/nullable/non-null).
	 */
	@Getter
	private final FieldNullType nullType;
	
	/**
	 * 'mapping name' for the field, e.g. if field is represented under
	 * a different name externally (such as in JSON).
	 */
	@Getter
	private final String mappingName;
}
