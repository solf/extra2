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
package io.github.solf.extra2.collection;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;

import lombok.NonNull;

/**
 * A serializable version of {@link BMap}.
 * 
 * @author Sergey Olefir
 */
public interface SerializableBMap<K, V> extends BMap<K, V>, Serializable
{
	/**
	 * Represents any given Java map as {@link SerializableBMap} via a thin
	 * wrapper.
	 * <p>
	 * All operations on the wrapper are pass-through to the underlying map instance.
	 * <p>  
	 * NOTE: whether resulting map is actually serializable depends on whether
	 * underlying map is serializable.
	 * <p>
	 * The performance impact of using this is very similar to using {@link Collections#unmodifiableMap(Map)} --
	 * specifically any entry iteration over the map results in a creation of 
	 * intermediate wrapper object ({@link ReadOnlyEntry}) for each entry iterated.
	 */
	@Nonnull
	public static <K, V> SerializableBMap<K, V> of(@Nonnull @NonNull Map<K, V> mapToWrap)
	{
		return BMap.of(mapToWrap);
	}
}
