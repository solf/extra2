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

import java.util.concurrent.Callable;

import io.github.solf.extra2.concurrent.RunnableWithException;

/**
 * Some exception-related utilities.
 *
 * @author Sergey Olefir
 */
public class ExceptionUtil
{
	
	/**
	 * A way to re-throw non-runtime exception as runtime exceptions.
	 */
	public static <T> T rethrowAsRuntime(Callable<T> src)
	{
		try
		{
			return src.call();
		} catch (RuntimeException e) {
			throw e;
		} catch( Exception e )
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * A way to re-throw non-runtime exception as runtime exceptions.
	 */
	public static void rethrowAsRuntime(RunnableWithException src)
	{
		try
		{
			src.run();
		} catch (RuntimeException e) {
			throw e;
		} catch( Exception e )
		{
			throw new RuntimeException(e);
		}
	}
}
