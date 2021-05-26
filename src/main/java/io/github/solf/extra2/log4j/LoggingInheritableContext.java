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
package io.github.solf.extra2.log4j;

import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Support for inheritable logging context, see e.g. {@link PatternLayoutWithInheritableContext}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class LoggingInheritableContext
{
	/**
	 * Keeps current logging context that is by default inherited by child threads.
	 * <p>
	 * null if not set
	 */
	private static final InheritableThreadLocal<@Nullable Supplier<String>> inheritableContext = new InheritableThreadLocal<>();
	
	/**
	 * Sets string context that will be inherited by child threads.
	 */
	public static void setContext(String strContext)
	{
		inheritableContext.set(() -> strContext);
	}
	
	/**
	 * Sets supplier context that will be inherited by child threads.
	 * <p>
	 * {@link Supplier} must generate context string when requested by the
	 * framework. 
	 */
	public static void setContext(Supplier<String> supplierContext)
	{
		inheritableContext.set(supplierContext);
	}
	
	/**
	 * Resets current context (sets it to null).
	 */
	public static void resetContext()
	{
		inheritableContext.set(null);
	}
	
	/**
	 * Gets current context.
	 */
	@Nullable
	/*package*/ static Supplier<String> getCurrentContext()
	{
		return inheritableContext.get();
	}
}
