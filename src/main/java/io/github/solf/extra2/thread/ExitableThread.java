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
package io.github.solf.extra2.thread;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Thread implementation that can be signaled to exit via {@link #exitAsap()}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public abstract class ExitableThread extends Thread
{
	/**
	 * 
	 */
	public ExitableThread()
	{
		super();
	}

	/**
	 * @param target
	 * @param name
	 */
	public ExitableThread(@Nullable Runnable target, String name)
	{
		super(target, name);
	}

	/**
	 * @param target
	 */
	public ExitableThread(@Nullable Runnable target)
	{
		super(target);
	}

	/**
	 * @param name
	 */
	public ExitableThread(String name)
	{
		super(name);
	}

	/**
	 * @param group
	 * @param target
	 * @param name
	 * @param stackSize
	 */
	public ExitableThread(@Nullable ThreadGroup group, @Nullable Runnable target, String name,
		long stackSize)
	{
		super(group, target, name, stackSize);
	}

	/**
	 * @param group
	 * @param target
	 * @param name
	 */
	public ExitableThread(@Nullable ThreadGroup group, @Nullable Runnable target, String name)
	{
		super(group, target, name);
	}

	/**
	 * @param group
	 * @param target
	 */
	public ExitableThread(@Nullable ThreadGroup group, @Nullable Runnable target)
	{
		super(group, target);
	}

	/**
	 * @param group
	 * @param name
	 */
	public ExitableThread(@Nullable ThreadGroup group, String name)
	{
		super(group, name);
	}

	/**
	 * Signals this thread that it should exit as soon as possible.
	 * <p>
	 * Typical implementation is expected to set some 'exit flag' and interrupt
	 * the thread itself in order to achieve that.
	 */
	public abstract void exitAsap();
}
