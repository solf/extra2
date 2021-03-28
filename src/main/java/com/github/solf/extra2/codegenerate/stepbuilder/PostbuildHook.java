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
package com.github.solf.extra2.codegenerate.stepbuilder;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * {@link StepBuilderGenerator} will invoke postbuild() method for classes
 * implementing this interface.
 * <p>
 * The method should return an actual instance to be returned to the builder client
 * (e.g. 'this' or it can be a new instance if required) -- i.e. return value
 * be of the buildee type.
 * <p>
 * This might be useful when e.g. using generated constructors (such as
 * with Lombok) to carry out functionality that's impossible to include
 * in generated constructor (such as validation or some fields warmup).
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public interface PostbuildHook
{
	/**
	 * Invoked on an instance after it has been constructed.
	 * <p>
	 * This might be useful when e.g. using generated constructors (such as
	 * with Lombok) to carry out functionality that's impossible to include
	 * in generated constructor (such as validation or some fields warmup).
	 */
	public Object postbuild();
}
