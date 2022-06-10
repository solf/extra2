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
import java.util.Set;

import javax.annotation.Nonnull;

import lombok.NonNull;

/**
 * A serializable version of {@link BSet}.
 * 
 * @author Sergey Olefir
 */
public interface SerializableBSet<E> extends BSet<E>, Serializable
{
	/**
	 * Represents any given Java set as {@link SerializableBSet} via a thin
	 * wrapper.
	 * <p>
	 * All operations on the wrapper are pass-through to the underlying set instance.
	 * <p>  
	 * NOTE: whether resulting set is actually serializable depends on whether
	 * underlying set is serializable.
	 * <p>
	 * This has very little performance impact and can be used freely as needed.
	 */
	@Nonnull
	public static <E> SerializableBSet<E> of(@Nonnull @NonNull Set<E> setToWrap)
	{
		return new WrapperBSet<>(setToWrap); 
	}
}
