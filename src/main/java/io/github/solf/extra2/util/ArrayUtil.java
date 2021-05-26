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

import static io.github.solf.extra2.util.NullUtil.nn;

import java.util.Arrays;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Array utilities.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class ArrayUtil
{
	/**
	 * Method merges two arrays -- returns one array that contains all elements
	 * from the first array followed by the all elements from the second array.
	 */
	public static <T> T[] mergeArrays(T[] array1, T[] array2)
	{
		T[] result = nn(Arrays.copyOf(array1, array1.length + array2.length));
		System.arraycopy(array2, 0, result, array1.length, array2.length);

		return result;
	}
}
