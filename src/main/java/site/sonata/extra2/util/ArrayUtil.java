/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.util;

import static site.sonata.extra2.util.NullUtil.nn;

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
