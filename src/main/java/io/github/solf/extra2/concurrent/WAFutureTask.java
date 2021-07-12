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
import java.util.concurrent.FutureTask;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This is an extension to {@link FutureTask} that also contains reference
 * to the task being executed.
 * 
 * Note that unlike parent it only supports {@link Callable} ({@link Runnable}
 * is not supported).
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class WAFutureTask<T extends Callable<V>, V> extends FutureTask<V> implements WAFuture<T, V>
{

	/**
	 * Task being executed.
	 */
	private final T task;
	
	/**
	 * @param callable
	 */
	public WAFutureTask(T callable)
	{
		super(callable);
		this.task = callable;
	}

	/* (non-Javadoc)
	 * @see io.github.solf.extra2.concurrent.WAFuture#getTask()
	 */
	@Override
	public T getTask()
	{
		return task;
	}

}
