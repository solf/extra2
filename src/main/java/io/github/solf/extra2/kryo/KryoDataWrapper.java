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
package io.github.solf.extra2.kryo;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Wrapper for data stored by {@link KryoDB} -- contains any necessary metadata.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
/*package*/ class KryoDataWrapper<T>
{
	/**
	 * Actual data.
	 */
	@Nullable
	T data;
	
	/**
	 * Data version (default non-existent value is 0).
	 */
	int version = 0;
}
