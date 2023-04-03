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
package io.github.solf.extra2.concurrent;

import java.util.function.Consumer;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * {@link Consumer} that is allowed to throw specific {@link Exception} type
 * (checked or unchecked).
 *
 * @author Sergey Olefir
 */
@FunctionalInterface
//Exclude TYPE_ARGUMENT as we will allow null return values.
@NonNullByDefault({DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE, DefaultLocation.FIELD, DefaultLocation.TYPE_BOUND, DefaultLocation.ARRAY_CONTENTS})
public interface ConsumerWithExceptionType<T, E extends Exception>
{
	/**
	 * Just like {@link Consumer#accept(Object)} but allows to throw specific exception.
	 */
    void accept(T t) throws E;
}
