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

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Extension to {@link Future} interface that has reference to {@link Callable} 
 * that this future is for.
 *
 * @param <T> invoking task type
 * @param <V> task result type
 * 
 * @author Sergey Olefir
 */
@NonNullByDefault
public interface WAFuture<T extends Callable<V>, V> extends Future<V>
{
	/**
	 * Gets {@link Callable} that this future is for.
	 * This is useful if e.g. task didn't complete for whatever reason and
	 * result is not available.
	 */
	public T getTask();
}
