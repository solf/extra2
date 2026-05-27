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
package io.github.solf.extra2.file;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.stacktrace.StackTrace;

/**
 * ddd debug only, remove!!!
 * TODO ccc - class description
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class TestStack
{

	public static void main(String[] args)
	{
		main2(args);
	}
	
	/**
	 * @param args
	 */
	public static void main2(String[] args)
	{
		System.out.println(StackTrace.getShortInvocationTrace());
		System.out.println(StackTrace.getCurrentJavaClassShortName(1));
		System.out.println(StackTrace.getCurrentLineNumber(1));
	}

}
