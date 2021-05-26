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
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * This is an abstract implementation of {@link ExitableThread} that also provides
 * an interrupt-handling service via {@link #handleUnexpectedInterruptedException(InterruptedException)}
 * <p>
 * When {@link InterruptedException} occurs, this implementation checks if exit
 * flag is set (via {@link #exitAsap()}); if it is set, then thread exits normally.
 * <p>
 * Otherwise {@link #handleUnexpectedInterruptedException(InterruptedException)}
 * method is invoked and depending on its results thread either exits or re-enters
 * {@link #run1(boolean)} method.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public abstract class InterruptHandlingExitableThread extends ExitableThread
{
	/**
	 * If true, indicates that thread should exit ASAP; if false, means that
	 * any {@link InterruptedException} should result in {@link #run1(boolean)}
	 * restart.
	 */
	protected volatile boolean exitFlag = false;
	
	/**
	 * 
	 */
	public InterruptHandlingExitableThread()
	{
		super();
	}

	/**
	 * @param name
	 */
	public InterruptHandlingExitableThread(String name)
	{
		super(name);
	}

	/**
     * @param  stackSize
     *         the desired stack size for the new thread, or zero to indicate
     *         that this parameter is to be ignored.
	 */
	public InterruptHandlingExitableThread(@Nullable ThreadGroup group,
		String name, long stackSize)
	{
		super(group, null/*target runnable*/, name, stackSize);
	}

	/**
	 * @param group
	 * @param name
	 */
	public InterruptHandlingExitableThread(@Nullable ThreadGroup group, String name)
	{
		super(group, name);
	}

	@Override
	public void exitAsap()
	{
		exitFlag = true;
		
		this.interrupt(); // Interrupt this thread
	}

	@Override
	public final void run()
	{
		boolean reentry = false;
		while(true)
		{
			try
			{
				run1(reentry);
			} catch (InterruptedException e)
			{
				if (exitFlag)
					return; // we should exit, so do so
				
				Thread.interrupted(); // clear interrupt flag
				if (!handleUnexpectedInterruptedException(e))
					return; // normal thread exit
			} finally
			{
				reentry = true;
			}
		}
	}

	/**
	 * Subclasses need to implement this method -- it acts exactly as {@link Thread#run()}
	 * except there's an outer handler that restarts this method if an 
	 * {@link InterruptedException} occurs (unless the exit flag is also set
	 * via {@link #exitAsap()})
	 * 
	 * @param reentry false for initial entry, true for any re-entries after
	 * 		unexpected {@link InterruptedException}
	 */
	protected abstract void run1(boolean reentry) throws InterruptedException;
	
	/**
	 * This is invoked whenever unexpected {@link InterruptedException} occurs.
	 * <p>
	 * Interrupted flag is CLEARED before this method is invoked.
	 * <p>
	 * Handler should decide whether to re-enter {@link #run1(boolean)} method;
	 * if not, then thread will exit normally.
	 * 
	 * @return true if {@link #run1(boolean)} should be re-entered; false if
	 * 		thread to exit normally
	 */
	protected abstract boolean handleUnexpectedInterruptedException(InterruptedException e);
}
