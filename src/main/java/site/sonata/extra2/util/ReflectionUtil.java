/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.util;

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
