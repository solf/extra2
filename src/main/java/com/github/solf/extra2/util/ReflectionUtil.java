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
package com.github.solf.extra2.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Some reflection-related utilities.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
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
}
