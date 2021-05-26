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
package io.github.solf.extra2.options;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Possible option constraints for methods that support it, e.g. 
 * {@link BaseOptions#getLongList(String, OptionConstraint...)}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public enum OptionConstraint
{
	/** Indicates that collection-type value must not be empty (e.g. non-empty list or w/e) */
	NON_EMPTY_COLLECTION,
	/** Indicates that element-type (i.e. non-colllection) value must not be empty (e.g. string list may not contain empty values) */
	NON_EMPTY_ELEMENT,
	/** Indicates that value(s) must be positive */
	POSITIVE,
	/** Indicates that value(s) must be non-negative (0 or more) */
	NON_NEGATIVE,
	/** Indicates that value(s) must be -1 or more (-1, 0, 1, ...) */
	NEGATIVE_ONE_OR_MORE,
}
