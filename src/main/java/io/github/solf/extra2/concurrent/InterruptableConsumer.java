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

import javax.annotation.NonNullByDefault;

/**
 * {@link Consumer} that is allowed to throw {@link InterruptedException}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
@FunctionalInterface
public interface InterruptableConsumer<T>
{
	/**
	 * Just like {@link Consumer#accept(Object)} but allows to throw {@link InterruptedException}
	 */
    void accept(T t) throws InterruptedException;
}
