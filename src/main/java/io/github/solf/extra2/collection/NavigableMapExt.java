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

import java.util.NavigableMap;

import javax.annotation.NonNullByDefault;

/**
 * Extends {@link NavigableMap} with additional methods from {@link MapExt}
 * 
 * @see MapExt
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public interface NavigableMapExt<K, V> extends SortedMapExt<K, V>, NavigableMap<K, V>
{
	// No additional methods
}
