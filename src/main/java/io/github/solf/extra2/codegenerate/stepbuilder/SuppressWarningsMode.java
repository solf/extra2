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
package io.github.solf.extra2.codegenerate.stepbuilder;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * What mode is used for suppressing warnings.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
@RequiredArgsConstructor
public enum SuppressWarningsMode
{
	/**
	 * No warnings should be suppressed.
	 */
	NONE(null),
	/**
	 * 'unused' warnings should be suppressed.
	 */
	UNUSED("unused"),
	/**
	 * 'all' warnings should be suppressed.
	 */
	ALL("all"),
	;
	
	/**
	 * Token that can be used to suppress this kind of warnings. 
	 */
	@Getter
	@Nullable
	private final String warningsSuppressString;
}
