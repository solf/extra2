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
package com.github.solf.extra2.storage;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Used to define 'already exist' modes.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public enum ExistMode
{
	/** Item must already exist */
	MUST_EXIST,
	/** Item must NOT already exist (descriptor doesn't immediately create it) */
	MUST_NOT_EXIST,
	/** Item either must NOT already exist (it is created) or it must be empty */
	MUST_BE_EMPTY_OR_NOT_EXIST,
	/** Item must exists and be non-empty */
	MUST_BE_NON_EMPTY,
	/** Any is fine */
	ANY,
	;
}
