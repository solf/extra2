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
