/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.util;

import java.net.URL;
import java.net.URLClassLoader;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Some system utilities.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class SystemUtil
{
	/**
	 * Gets current system classpath.
	 */
	public static URL[] getSystemClasspath()
	{
        ClassLoader cl = ClassLoader.getSystemClassLoader();

        return ((URLClassLoader)cl).getURLs();
	}
	
	/**
	 * Dumps current system classpath to System.out
	 */
	public static void dumpSystemClasspath()
	{
		for(URL url : getSystemClasspath())
		{
			System.out.println(url.getFile());
		}
	}
}
