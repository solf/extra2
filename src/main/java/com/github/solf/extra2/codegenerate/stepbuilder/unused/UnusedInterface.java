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
package com.github.solf.extra2.codegenerate.stepbuilder.unused;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Unused interface.
 * <p>
 * Exists solely to import this into generated files to make sure @SuppressWarnings("unused")
 * doesn't generate 'unnecessary warning' warning.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public interface UnusedInterface
{
	// Empty.
}
