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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utility helper for integrating JSR305 (null/non-null) code with legacy code
 * which doesn't have proper annotations.
 *
 * @author Sergey Olefir
 */
//@ParametersAreNonnullByDefault // There must be no default as otherwise there might be type-cast errors in clients
								 // (some arguments must actually be un-annotated, not even by default)
public class NullUtil
{
	/**
	 * 'converts' value to non-nullable one.
	 */
	@SuppressWarnings("null")
	public static @Nonnull <T> T nonNull(@Nullable T arg)
	{
		return arg;
	}

	/**
	 * 'converts' value to non-nullable one (a shorter version for convenience).
	 */
	@SuppressWarnings("null")
	public static @Nonnull <T> T nn(@Nullable T arg)
	{
		return arg;
	}

	/**
	 * Checks that value is not null -- returns value if not null, exception otherwise.
	 * 
	 * @throws IllegalStateException if value is actually null
	 */
	public static @Nonnull <T> T nnChecked(@Nullable T arg) throws IllegalStateException
	{
		if (arg == null)
			throw new IllegalStateException("non-nullable value is null");
		return arg;
	}
	
	/**
	 * 'converts' value to nullable one.
	 */
	public static @Nullable <T> T nullable(T arg)
	{
		return arg;
	}
	
	/**
	 * 'Fake' conversion of nullable to non-nullable.
	 * To be used if we *know* that the value CAN be null, but for some reason
	 * we want to still force it to make compiler happy (e.g. if we catch
	 * exception anyway). 
	 */
	@SuppressWarnings("null")
	public static @Nonnull <T> T fakeNonNull(@Nullable T arg)
	{
		return arg;
	}
	
	/**
	 * 'Fake' non-null null (actually returns null but it is non-null from compiler POV).
	 * To be used to initialize something to null (e.g. in constructor or in
	 * fields) that we *know* won't stay as null by the time it is used (e.g.
	 * initialized via ORM or something). 
	 */
	@SuppressWarnings("null")
	public static @Nonnull <T> T fakeNonNull()
	{
		return null;
	}

	/**
	 * 'converts' collection to the one that:
	 * (a) is not null
	 * (b) contains non-null elements only
	 */
	@SuppressWarnings("null")
	public static <E> @Nonnull Iterator<@Nonnull E> nnc(Iterator<E> arg)
	{
		return arg;
	}

	/**
	 * Only exists for the rare cases when calling nnc() may be ambigious because
	 * of target implementing both Iterator and Iterable
	 * 
	 * 'converts' collection to the one that:
	 * (a) is not null
	 * (b) contains non-null elements only
	 */
	@SuppressWarnings("null")
	public static <E> @Nonnull Iterator<@Nonnull E> nnci(Iterator<E> arg)
	{
		return arg;
	}

	/**
	 * 'converts' collection to the one that:
	 * (a) is not null
	 * (b) contains non-null elements only
	 */
	@SuppressWarnings("null")
	public static <E> @Nonnull Iterable<@Nonnull E> nnc(Iterable<E> arg)
	{
		return arg;
	}

	/**
	 * 'converts' collection to the one that:
	 * (a) is not null
	 * (b) contains non-null elements only
	 */
	@SuppressWarnings("null")
	public static <E> @Nonnull Collection<@Nonnull E> nnc(Collection<E> arg)
	{
		return arg;
	}

	/**
	 * 'converts' collection to the one that:
	 * (a) is not null
	 * (b) contains non-null elements only
	 */
	@SuppressWarnings("null")
	public static <E> @Nonnull Set<@Nonnull E> nnc(Set<E> arg)
	{
		return arg;
	}

	/**
	 * 'converts' collection to the one that:
	 * (a) is not null
	 * (b) contains non-null elements only
	 */
	@SuppressWarnings("null")
	public static <E> @Nonnull List<@Nonnull E> nnc(List<E> arg)
	{
		return arg;
	}

	/**
	 * 'converts' collection to the one that:
	 * (a) is not null
	 * (b) contains non-null elements only
	 * <p>
	 * Works for collection that explicitly contain {@link Nullable} elements
	 * which may not always work with {@link #nnc(Collection)}
	 */
	@SuppressWarnings("null")
	public static <E> @Nonnull List<@Nonnull E> nncn(List<@Nullable E> arg)
	{
		return arg;
	}

	/**
	 * 'converts' collection to the one that:
	 * (a) contains non-null elements only
	 * (b) may itself be null
	 */
	@SuppressWarnings("null")
	public static <K, V> @Nonnull Map<@Nonnull K, @Nonnull V> nnc(Map<K, V> arg)
	{
		return arg;
	}

	/**
	 * 'converts' collection to the one that:
	 * (a) is not null
	 * (b) contains non-null elements only
	 */
	@SuppressWarnings("null")
	public static <E> @Nonnull E @Nonnull[] nnc(E[] arg)
	{
		return arg;
	}

	/**
	 * 'converts' collection to the one that:
	 * (a) contains non-null elements only
	 * (b) may itself be null
	 */
	@SuppressWarnings("null")
	public static <E> @Nullable Collection<@Nonnull E> nnce(Collection<E> arg)
	{
		return arg;
	}

	/**
	 * 'converts' collection to the one that:
	 * (a) contains non-null elements only
	 * (b) may itself be null
	 */
	@SuppressWarnings("null")
	public static <E> @Nullable Set<@Nonnull E> nnce(Set<E> arg)
	{
		return arg;
	}

	/**
	 * 'converts' collection to the one that:
	 * (a) contains non-null elements only
	 * (b) may itself be null
	 */
	@SuppressWarnings("null")
	public static <E> @Nullable List<@Nonnull E> nnce(List<E> arg)
	{
		return arg;
	}

	/**
	 * 'converts' collection to the one that:
	 * (a) contains non-null elements only
	 * (b) may itself be null
	 */
	@SuppressWarnings("null")
	public static <K, V> @Nullable Map<@Nonnull K, @Nonnull V> nnce(Map<K, V> arg)
	{
		return arg;
	}

	/**
	 * 'converts' collection to the one that:
	 * (a) contains non-null elements only
	 * (b) may itself be null
	 */
	@SuppressWarnings("null")
	public static <E> @Nonnull E @Nullable[] nnce(E[] arg)
	{
		return arg;
	}

	/**
	 * Coerces collection elements to be @Nullable
	 * <p>
	 * Collection itself becomes @Nonnull; use nullable() if you need to make it @Nullable
	 */
	@SuppressWarnings("null")
	public static <E> @Nonnull Collection<@Nullable E> nuce(Collection<E> arg)
	{
		return arg;
	}

	/**
	 * Coerces collection elements to be @Nullable
	 * <p>
	 * Collection itself becomes @Nonnull; use nullable() if you need to make it @Nullable
	 */
	@SuppressWarnings("null")
	public static <E> @Nonnull Set<@Nullable E> nuce(Set<E> arg)
	{
		return arg;
	}

	/**
	 * Coerces collection elements to be @Nullable
	 * <p>
	 * Collection itself becomes @Nonnull; use nullable() if you need to make it @Nullable
	 */
	@SuppressWarnings("null")
	public static <E> @Nonnull List<@Nullable E> nuce(List<E> arg)
	{
		return arg;
	}

	/**
	 * Coerces collection elements to be @Nullable
	 * <p>
	 * Collection itself becomes @Nonnull; use nullable() if you need to make it @Nullable
	 */
	@SuppressWarnings("null")
	public static <K, V> @Nonnull Map<@Nullable K, @Nonnull V> nuce(Map<K, V> arg)
	{
		return arg;
	}

	/**
	 * Coerces collection elements to be @Nullable.
	 * <p>
	 * Collection itself becomes @Nonnull; use nullable() if you need to make it @Nullable
	 */
	@SuppressWarnings("null")
	public static <E> @Nullable E @Nonnull [] nuce(E[] arg)
	{
		return arg;
	}
	
	/**
	 * @see Enum#valueOf(Class, String)
	 */
	@SuppressWarnings("null")
	public static <T extends Enum<T>> @Nonnull T enumValueOf(Class<T> enumType, String name)
	{
		return Enum.valueOf(enumType, name);
	}
}
