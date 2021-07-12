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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Utilities for working with classes / types.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class TypeUtil
{
	/**
	 * Coerces any object to the requested type -- both argument and result
	 * must be non-null.
	 * 
	 * E.g.:
	 * T result = TypeUtil.coerce(new SomethingThatIsActuallyT());
	 */
	@SuppressWarnings("unchecked")
	public static <K> @Nonnull K coerce(@Nonnull Object obj)
	{
		return (K)obj;
	}
	
	/**
	 * Coerces any object to the requested type -- argument may be nullable or
	 * unknown, but result is coerced to be non-null -- USE WITH CAUTION.
	 * 
	 * E.g.:
	 * T result = TypeUtil.coerce(new SomethingThatIsActuallyT());
	 */
	@SuppressWarnings("unchecked")
	@NonNullByDefault({})
	public static <@Nonnull K> @Nonnull K coerceForceNonnull(Object obj)
	{
		return (K)obj;
	}
	
	/**
	 * Coerces any object to the requested type -- both argument and result
	 * are nullable.
	 * 
	 * E.g.:
	 * T result = TypeUtil.coerce(new SomethingThatIsActuallyT());
	 */
	@SuppressWarnings("unchecked")
	@Nullable 
	public static <K> K coerceNullable(@Nullable Object obj)
	{
		return (K)obj;
	}
	
	/**
	 * Coerces any object to the requested type (with unknown nullability) --
	 * useful with e.g. type variables of unknown nullability.
	 * 
	 * E.g.:
	 * T result = TypeUtil.coerce(new SomethingThatIsActuallyT());
	 */
	@SuppressWarnings("unchecked")
	@NonNullByDefault({})
	public static <K> K coerceUnknown(Object obj)
	{
		return (K)obj;
	}
}
