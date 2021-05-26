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

/**
 * This is an extension to {@link FutureTask} that also contains reference
 * to the task being executed.
 * 
 * Note that unlike parent it only supports {@link Callable} ({@link Runnable}
 * is not supported).
 *
 * @author Sergey Olefir
 */
public class WARunnableFutureTask<T extends Runnable, V> extends FutureTask<V> implements WARunnableFuture<T, V>
{

	/**
	 * Task being executed.
	 */
	private final T runnable;
	
	/**
	 * @param callable
	 */
	public WARunnableFutureTask(T runnable, V result)
	{
		super(runnable, result);
		this.runnable = runnable;
	}

	/* (non-Javadoc)
	 * @see io.github.solf.extra2.concurrent.WARunnableFuture#getRunnable()
	 */
	@Override
	public T getRunnable()
	{
		return runnable;
	}
}
