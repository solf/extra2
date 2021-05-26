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
package io.github.solf.extra2.cache.wbrb.example;

import javax.annotation.NonNullByDefault;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Example in-cache representation of signal storage.
 * <p>
 * This consists of 'our' value which is modifiable and 'other' value which is
 * fixed at the time of read.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
@AllArgsConstructor
/*package*/ class ExSignalCache
{
	/**
	 * 'our' signal value, modifiable.
	 */
	@Getter
	private final StringBuilder ourSignalValue;
	
	/**
	 * 'other' signal value, not modifiable
	 */
	@Getter
	private final String otherSignalValue;
}
