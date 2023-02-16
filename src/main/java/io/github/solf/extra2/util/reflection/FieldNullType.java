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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Classification of field types in terms of nullability.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public enum FieldNullType
{
	/**
	 * Primitive type (cannot be null).
	 */
	PRIMITIVE,
	/**
	 * Object type that is allowed to be null.
	 */
	NULLABLE,
	/**
	 * Object type that is not supposed to be null.
	 */
	NON_NULL,
	;
}
